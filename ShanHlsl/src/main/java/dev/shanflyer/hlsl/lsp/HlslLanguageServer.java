package dev.shanflyer.hlsl.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageServer;

public interface HlslLanguageServer extends LanguageServer {
    record ClassificationParams(TextDocumentIdentifier textDocument) {}
    record ClassificationSpan(int start, int end, String kind) {}
    record ClassificationResult(String text, List<ClassificationSpan> spans) {}

    @JsonRequest("shaderTools/classifications")
    CompletableFuture<ClassificationResult> classifications(ClassificationParams params);
}
