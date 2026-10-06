"""Prepare an isolated installer cache using installer/Mojang SHA-1 metadata."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import urllib.request
import zipfile
from pathlib import Path


def native_path(path: Path) -> Path:
    absolute = str(path.resolve())
    if os.name == "nt" and not absolute.startswith("\\\\?\\"):
        return Path("\\\\?\\" + absolute)
    return Path(absolute)


def digest(path: Path) -> str:
    with native_path(path).open("rb") as stream:
        return hashlib.file_digest(stream, "sha1").hexdigest()


def contained(root: Path, relative: str) -> Path:
    target = (root / relative).resolve()
    target.relative_to(root.resolve())
    return target


def request(url: str) -> bytes:
    with urllib.request.urlopen(url, timeout=60) as response:
        return response.read()


def verified_download(url: str, destination: Path, expected: str) -> None:
    if not url.startswith("https://"):
        raise ValueError(f"Artifact URL must use HTTPS: {url}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    partial = destination.with_name(destination.name + ".partial")
    with urllib.request.urlopen(url, timeout=60) as source, partial.open("wb") as output:
        shutil.copyfileobj(source, output, 1024 * 1024)
    if digest(partial) != expected:
        raise ValueError(f"Official SHA-1 mismatch: {destination.name}")
    partial.replace(destination)


def maven_path(coordinate: str) -> str:
    coordinate, _, extension = coordinate.partition("@")
    group, artifact, version, *classifier = coordinate.split(":")
    suffix = "-" + classifier[0] if classifier else ""
    return f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}{suffix}.{extension or 'jar'}"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--installer", required=True, type=Path)
    parser.add_argument("--installer-sha1", required=True)
    parser.add_argument("--server-dir", required=True, type=Path)
    parser.add_argument("--prism-libraries", type=Path, default=Path.home() / "AppData/Roaming/PrismLauncher/libraries")
    parser.add_argument("--gradle-cache", type=Path, default=Path.home() / ".gradle/caches")
    args = parser.parse_args()
    if digest(args.installer) != args.installer_sha1.lower():
        raise ValueError("Installer does not match the verified official installer manifest")
    # NeoForge Maven paths exceed MAX_PATH under a dated result directory.
    # Keep this local to Python file operations; Java receives the normal path.
    server = native_path(args.server_dir)
    libraries = server / "libraries"
    verification = server / "verification"
    verification.mkdir(parents=True, exist_ok=True)
    records: list[dict] = []

    def candidates(relative: str, coordinate: str | None = None):
        yield args.prism_libraries / relative
        yield args.gradle_cache / "forge_gradle/maven_downloader" / relative
        if coordinate:
            base, _, _ = coordinate.partition("@")
            parts = base.split(":")
            if len(parts) >= 3:
                directory = args.gradle_cache / "modules-2/files-2.1" / parts[0] / parts[1] / parts[2]
                yield from directory.glob("*/" + Path(relative).name)

    def supply(destination: Path, expected: str, sources, url: str, label: str) -> None:
        expected = expected.lower()
        if destination.is_file() and digest(destination) == expected:
            records.append(dict(artifact=label, status="verified-existing", sha1=expected, url=url))
            return
        for source in sources:
            if source.is_file() and digest(source) == expected:
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(native_path(source), destination)
                records.append(dict(artifact=label, status="verified-cache-copy", source=str(source), sha1=expected, url=url))
                return
        if not url:
            records.append(dict(artifact=label, status="generated-by-installer", sha1=expected))
            return
        print(f"Downloading verified server artifact {label}", flush=True)
        verified_download(url, destination, expected)
        records.append(dict(artifact=label, status="verified-download", sha1=expected, url=url))

    with zipfile.ZipFile(args.installer) as archive:
        profile = json.loads(archive.read("install_profile.json"))
        version_path = profile.get("json", "/version.json").lstrip("/")
        loader_version = json.loads(archive.read(version_path))
        (verification / "install_profile.json").write_text(json.dumps(profile, indent=2))
        (verification / "loader-version.json").write_text(json.dumps(loader_version, indent=2))
        artifacts: dict[str, dict] = {}
        for library in profile.get("libraries", []) + loader_version.get("libraries", []):
            if library.get("serverreq") is False:
                continue
            artifact = library.get("downloads", {}).get("artifact")
            if not artifact or not artifact.get("sha1"):
                continue
            relative = artifact.get("path") or maven_path(library["name"])
            previous = artifacts.get(relative)
            if previous and previous["sha1"] != artifact["sha1"]:
                raise ValueError(f"Conflicting official artifact hashes: {relative}")
            artifacts[relative] = artifact | {"coordinate": library["name"]}
        for relative, artifact in artifacts.items():
            destination = contained(libraries, relative)
            embedded = "maven/" + relative
            if embedded in archive.namelist():
                payload = archive.read(embedded)
                if hashlib.sha1(payload).hexdigest() != artifact["sha1"]:
                    raise ValueError(f"Bundled artifact hash mismatch: {relative}")
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes(payload)
                records.append(dict(artifact=relative, status="verified-installer-entry", sha1=artifact["sha1"]))
            else:
                supply(destination, artifact["sha1"], candidates(relative, artifact["coordinate"]),
                       artifact.get("url", ""), relative)

    manifest_url = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    manifest_payload = request(manifest_url)
    (verification / "mojang-version-manifest.json").write_bytes(manifest_payload)
    manifest = json.loads(manifest_payload)
    minecraft = profile["minecraft"]
    metadata_entry = next(entry for entry in manifest["versions"] if entry["id"] == minecraft)
    cached_metadata = args.gradle_cache / "forge_gradle/minecraft_repo/versions" / minecraft / "version.json"
    if cached_metadata.is_file() and digest(cached_metadata) == metadata_entry["sha1"]:
        metadata_payload = cached_metadata.read_bytes()
    else:
        metadata_payload = request(metadata_entry["url"])
    if hashlib.sha1(metadata_payload).hexdigest() != metadata_entry["sha1"]:
        raise ValueError("Minecraft version metadata differs from the official version manifest")
    (verification / "minecraft-version.json").write_bytes(metadata_payload)
    metadata = json.loads(metadata_payload)
    jar_path = profile.get("serverJarPath", f"minecraft_server.{minecraft}.jar")
    jar_path = jar_path.replace("{LIBRARY_DIR}", str(libraries)).replace("{ROOT}", str(server))
    jar_path = jar_path.replace("{MINECRAFT_VERSION}", minecraft)
    server_jar = Path(jar_path)
    if not server_jar.is_absolute():
        server_jar = contained(server, jar_path)
    server_jar.resolve().relative_to(server)
    mojang_server = metadata["downloads"]["server"]
    sources = list((args.gradle_cache / "forge_gradle/mcp_repo/de/oceanlabs/mcp/mcp_config").glob(
        minecraft + "*/joined/downloadServer/server.jar"))
    sources.extend((args.gradle_cache / "ng_execute").glob("*/server.jar"))
    sources.append(args.gradle_cache / "forge_gradle/minecraft_repo/versions" / minecraft / "server.jar")
    supply(server_jar, mojang_server["sha1"], sources, mojang_server["url"], f"Minecraft {minecraft} server")

    mappings_entry = metadata["downloads"].get("server_mappings")
    mappings_target = profile.get("data", {}).get("MOJMAPS", {}).get("server", "")
    if mappings_entry and mappings_target.startswith("["):
        relative = maven_path(mappings_target[1:-1])
        sources = [args.gradle_cache / "forge_gradle/minecraft_repo/versions" / minecraft / "server_mappings.txt"]
        sources.extend((args.gradle_cache / "ng_execute").glob("*/server_mappings.txt"))
        supply(contained(libraries, relative), mappings_entry["sha1"], sources,
               mappings_entry["url"], f"Minecraft {minecraft} server mappings")

    report = dict(minecraft=minecraft, installer=str(args.installer), installerSha1=args.installer_sha1.lower(),
                  mojangManifestUrl=manifest_url, minecraftMetadataUrl=metadata_entry["url"],
                  minecraftMetadataSha1=metadata_entry["sha1"], artifacts=records)
    (verification / "seed-report.json").write_text(json.dumps(report, indent=2))
    print(f"Prepared {len(records)} SHA-1 checked artifacts for Minecraft {minecraft}.", flush=True)


if __name__ == "__main__":
    main()
