"""Compare decompressed JAR entries, ignoring ZIP timestamps and compression."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import zipfile


def payload(path: Path) -> dict[str, str]:
    with zipfile.ZipFile(path) as archive:
        entries = [entry for entry in archive.infolist() if not entry.is_dir()]
        names = [entry.filename for entry in entries]
        if len(names) != len(set(names)):
            raise ValueError(f"Duplicate JAR entries: {path}")
        return {
            entry.filename: hashlib.sha256(archive.read(entry)).hexdigest()
            for entry in entries
        }


def compare(reference: Path, rebuilt: Path) -> dict:
    before, after = payload(reference), payload(rebuilt)
    missing = sorted(before.keys() - after.keys())
    added = sorted(after.keys() - before.keys())
    changed = sorted(name for name in before.keys() & after.keys() if before[name] != after[name])
    return {
        "reference": str(reference.resolve()),
        "rebuilt": str(rebuilt.resolve()),
        "referenceJarSha256": hashlib.sha256(reference.read_bytes()).hexdigest(),
        "rebuiltJarSha256": hashlib.sha256(rebuilt.read_bytes()).hexdigest(),
        "referenceEntries": len(before),
        "rebuiltEntries": len(after),
        "payloadIdentical": not (missing or added or changed),
        "missing": missing,
        "added": added,
        "changed": changed,
        "changedEntryHashes": {
            name: {"reference": before[name], "rebuilt": after[name]} for name in changed
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("reference", type=Path)
    parser.add_argument("rebuilt", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = compare(args.reference, args.rebuilt)
    serialized = json.dumps(result, indent=2)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(serialized + "\n", encoding="utf-8")
    print(serialized)
    return 0 if result["payloadIdentical"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
