"""Exercise the bundled, real HlslTools server through JSON-RPC over stdio."""
import json
import queue
import subprocess
import tempfile
import threading
import time
from pathlib import Path

root = Path(__file__).resolve().parents[1]
server = root / "build/server/win-x64/ShaderTools.LanguageServer.exe"
checks = []


def check(condition, message):
    if not condition:
        raise AssertionError(message)
    checks.append(message)
    print("OK " + message, flush=True)


class Client:
    def __init__(self, directory):
        self.errors = open(root / "build/lsp-stderr.log", "wb")
        self.process = subprocess.Popen([str(server)], stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                        stderr=self.errors, cwd=directory)
        self.messages = queue.Queue()
        self.notifications = []
        self.sequence = 0
        self.reader = threading.Thread(target=self.read, daemon=True)
        self.reader.start()

    def read(self):
        try:
            while True:
                headers = {}
                while True:
                    line = self.process.stdout.readline()
                    if not line:
                        raise EOFError("Language server closed stdout")
                    if line in (b"\r\n", b"\n"):
                        break
                    key, value = line.decode("ascii").split(":", 1)
                    headers[key.lower()] = value.strip()
                remaining = int(headers["content-length"])
                data = bytearray()
                while remaining:
                    chunk = self.process.stdout.read(remaining)
                    if not chunk:
                        raise EOFError("Incomplete JSON-RPC message")
                    data.extend(chunk)
                    remaining -= len(chunk)
                self.messages.put(json.loads(data))
        except Exception as error:
            self.messages.put(error)

    def send(self, method, params=None, identifier=None):
        message = {"jsonrpc": "2.0", "method": method}
        if params is not None:
            message["params"] = params
        if identifier is not None:
            message["id"] = identifier
        self.write(message)

    def write(self, message):
        payload = json.dumps(message).encode("utf-8")
        self.process.stdin.write(f"Content-Length: {len(payload)}\r\n\r\n".encode("ascii") + payload)
        self.process.stdin.flush()

    def receive(self, timeout=20):
        message = self.messages.get(timeout=timeout)
        if isinstance(message, Exception):
            raise message
        if "method" in message and "id" in message:
            result = [] if message["method"] == "workspace/configuration" else None
            self.write({"jsonrpc": "2.0", "id": message["id"], "result": result})
        elif "method" in message:
            self.notifications.append(message)
        return message

    def request(self, method, params):
        self.sequence += 1
        identifier = self.sequence
        self.send(method, params, identifier)
        deadline = time.monotonic() + 30
        while time.monotonic() < deadline:
            message = self.receive()
            if message.get("id") == identifier and "method" not in message:
                if "error" in message:
                    raise RuntimeError(f"{method}: {message['error']}")
                return message.get("result")
        raise TimeoutError(method)

    def close(self):
        if self.process.poll() is None:
            try:
                self.request("shutdown", None)
                self.send("exit")
                self.process.wait(timeout=5)
            except Exception:
                self.process.kill()
                self.process.wait(timeout=5)
        self.errors.close()


def position(text, fragment, delta=0):
    offset = text.index(fragment) + delta
    return {"line": text.count("\n", 0, offset), "character": offset - text.rfind("\n", 0, offset) - 1}


def main():
    with tempfile.TemporaryDirectory(prefix="shan-hlsl-tests-") as temporary:
        directory = Path(temporary)
        client = Client(directory)
        try:
            result = client.request("initialize", {
                "processId": None, "rootUri": directory.as_uri(),
                "capabilities": {"textDocument": {
                    "completion": {"completionItem": {"snippetSupport": True}},
                    "hover": {"contentFormat": ["markdown", "plaintext"]},
                    "signatureHelp": {"signatureInformation": {"documentationFormat": ["markdown"]}}
                }}, "clientInfo": {"name": "Shan HLSL tests", "version": "0.1.2"}
            })
            caps = result["capabilities"]
            (root / "build/lsp-capabilities.json").write_text(json.dumps(caps, indent=2), encoding="utf-8")
            check(caps.get("completionProvider") is not None, "Completion capability")
            check(caps.get("signatureHelpProvider") is not None, "Signature help capability")
            check(caps.get("definitionProvider"), "Definition capability")
            check(caps.get("documentFormattingProvider"), "Original HLSL formatter exposed over LSP")
            client.send("initialized", {})
            text = """struct Input { float3 color; float2 uv; };
float3 Tint(float3 value) { return value * 2.0; }
float4 PSMain(Input input) : SV_Target {
    float3 shaded = Tint(input.color);
    return float4(shaded, 1.0);
}
#if 0
float excludedVariable = missingValue;
#endif
"""
            file = directory / "sample.hlsl"
            file.write_text(text, encoding="utf-8")
            uri = file.as_uri()
            document = {"uri": uri}
            client.send("textDocument/didOpen", {"textDocument": {"uri": uri, "languageId": "hlsl", "version": 1, "text": text}})
            def at(fragment, delta=0):
                return {"textDocument": document, "position": position(text, fragment, delta)}
            hover = client.request("textDocument/hover", at("shaded,", 2))
            check("float3" in json.dumps(hover), "Hover shows inferred shader variable type")
            definition = client.request("textDocument/definition", at("Tint(input", 2))
            check(bool(definition) and "sample.hlsl" in json.dumps(definition), "Go to user function definition")
            help_result = client.request("textDocument/signatureHelp", at("Tint(input", 5))
            check("Tint" in json.dumps(help_result), "User function signature help")
            completion = client.request("textDocument/completion", at("input.color", 6))
            check('"color"' in json.dumps(completion) and '"uv"' in json.dumps(completion), "Struct member semantic completion")
            symbols = client.request("textDocument/documentSymbol", {"textDocument": document})
            check("PSMain" in json.dumps(symbols) and "Tint" in json.dumps(symbols), "Document symbols / structure")
            highlights = client.request("textDocument/documentHighlight", at("shaded,", 2))
            check(len(highlights or []) >= 2, "Definition and usage highlighting")
            classified = client.request("shaderTools/classifications", {"textDocument": document})
            check(classified["text"] == text, "Classification tied to exact unsaved document snapshot")
            kinds = {span["kind"] for span in classified["spans"]}
            check("excluded code" in kinds, "Preprocessor excluded code classification")
            check("Hlsl.Function" in kinds and "Hlsl.Struct" in kinds, "Semantic function and struct colors")
            edits = client.request("textDocument/formatting", {"textDocument": document, "options": {"tabSize": 4, "insertSpaces": True}})
            check(bool(edits) and "PSMain" in edits[0]["newText"], "Formatting produces HLSL source edits")
            changed = text.replace("return float4(shaded, 1.0);", "return float4(undefinedValue, 1.0);")
            end = {"line": text.count("\n"), "character": 0}
            client.notifications.clear()
            client.send("textDocument/didChange", {"textDocument": {"uri": uri, "version": 2}, "contentChanges": [
                {"range": {"start": {"line": 0, "character": 0}, "end": end}, "text": changed}]})
            after = client.request("shaderTools/classifications", {"textDocument": document})
            check(after["text"] == changed, "Incremental change updates the unsaved semantic document")
            deadline = time.monotonic() + 15
            while time.monotonic() < deadline:
                errors = [notification for notification in client.notifications
                          if notification["method"] == "textDocument/publishDiagnostics"
                          and "undefinedValue" in json.dumps(notification)]
                if errors:
                    break
                client.receive(timeout=15)
            check(bool(errors), "Live semantic diagnostics for unsaved content")
            (root / "build/lsp-test-report.txt").write_text("\n".join(checks) + f"\nPASS: {len(checks)} LSP checks\n", encoding="utf-8")
            print(f"PASS: {len(checks)} real HlslTools LSP checks", flush=True)
        finally:
            client.close()


if __name__ == "__main__":
    main()
