"""Collect notices and package license metadata for the self-contained distribution."""
import json
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path

root = Path(__file__).resolve().parents[1]
assets_file = root / "build/upstream/HlslTools-3cb3e76a8a5a35daf6f8e6fa5c32da7facf7175a/src/ShaderTools.LanguageServer/obj/project.assets.json"
assets = json.loads(assets_file.read_text(encoding="utf-8-sig"))
nuget = Path(next(iter(assets["packageFolders"])))
output = root / "build/server/licenses"
output.mkdir(parents=True, exist_ok=True)
packages = {name for name, info in assets["libraries"].items() if info.get("type") == "package"}
for info in assets["project"]["frameworks"].values():
    for package in info.get("downloadDependencies", []):
        packages.add(package["name"] + "/" + package["version"].strip("[]"))
metadata = []
attributions = ["Third-party package attributions for Shan HLSL", ""]
for name in sorted(packages):
    identifier, version = name.split("/")
    directory = nuget / identifier.lower() / version
    if not directory.is_dir():
        continue
    copied = []
    for source in directory.rglob("*"):
        if source.is_file() and any(word in source.name.lower() for word in ("license", "notice")) and source.suffix.lower() not in {".dll", ".pdb", ".exe"}:
            relative = source.relative_to(directory)
            destination = output / identifier / version / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, destination)
            copied.append(relative.as_posix())
    licenses = []
    copyright_text = ""
    authors = ""
    for spec in directory.glob("*.nuspec"):
        nodes = list(ET.parse(spec).iter())
        licenses.extend({"type": node.tag.split("}")[-1], "value": node.text or "", "kind": node.attrib.get("type", "url")}
                        for node in nodes if node.tag.split("}")[-1] in {"license", "licenseUrl"})
        copyright_text = next((node.text or "" for node in nodes if node.tag.split("}")[-1] == "copyright"), "")
        authors = next((node.text or "" for node in nodes if node.tag.split("}")[-1] == "authors"), "")
    metadata.append({"package": name, "authors": authors, "copyright": copyright_text, "licenses": licenses, "noticeFiles": copied})
    attributions.extend([name, "Authors: " + authors, copyright_text,
                         *[item["value"] for item in licenses], ""])
(output / "THIRD-PARTY-ATTRIBUTIONS.txt").write_text("\n".join(attributions), encoding="utf-8")
shutil.copytree(root / "backend/licenses", output / "common", dirs_exist_ok=True)
shutil.copyfile(root / "LICENSE", output / "HlslTools-LICENSE.txt")
(output / "package-licenses.json").write_text(json.dumps(metadata, indent=2), encoding="utf-8")
print(f"Collected dependency license metadata for {len(metadata)} packages")
