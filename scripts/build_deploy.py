#!/usr/bin/env python3
"""
Build and deploy TopZurdo mod.
- Computes SHA-256 of mod source; if changed (or --force), runs Gradle build.
- Copies built JAR to MODS_DIR (from deploy.config or env MODS_DIR).
- Optionally removes old topzurdo-*.jar from mods folder before copy.
Usage:
  python build_deploy.py           # build if source changed, then deploy
  python build_deploy.py --force    # always build and deploy
  python build_deploy.py --build-only   # only build
  python build_deploy.py --hash-only    # only print current source hash
"""

import hashlib
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
ROOT = SCRIPT_DIR.parent
MOD_SRC = ROOT / "mod" / "src"
HASH_FILE = ROOT / "mod" / "build" / "mod-source.sha256"
HASH_PREV_FILE = ROOT / "mod" / ".mod-source.sha256.prev"
CONFIG_FILE = SCRIPT_DIR / "deploy.config"
MOD_BUILD_LIBS = ROOT / "mod" / "build" / "libs"


def get_mods_dir():
    if os.environ.get("MODS_DIR"):
        return Path(os.environ["MODS_DIR"]).expanduser().resolve()
    if CONFIG_FILE.exists():
        for line in CONFIG_FILE.read_text(encoding="utf-8").splitlines():
            line = line.split("#")[0].strip()
            if line.startswith("MODS_DIR="):
                value = line.split("=", 1)[1].strip()
                if value:
                    return Path(os.path.expandvars(value)).expanduser().resolve()
    return None


def source_hash():
    h = hashlib.sha256()
    for p in sorted(MOD_SRC.rglob("*")):
        if p.is_file() and p.suffix in (".java", ".json"):
            h.update(p.read_bytes())
    return h.hexdigest()


def build(force=False):
    current = source_hash()
    previous = HASH_PREV_FILE.read_text().strip() if HASH_PREV_FILE.exists() else ""
    if not force and current == previous and (MOD_BUILD_LIBS / f"topzurdo-1.0.0.jar").exists():
        print("No source changes, skip build.")
        return True
    print("Building mod...")
    gradle_cmd = "gradlew.bat" if os.name == "nt" else "./gradlew"
    r = subprocess.run([gradle_cmd, ":mod:build"], cwd=ROOT, shell=(os.name == "nt"))
    if r.returncode != 0:
        print("Build failed.")
        return False
    HASH_PREV_FILE.parent.mkdir(parents=True, exist_ok=True)
    HASH_PREV_FILE.write_text(current, encoding="utf-8")
    print("Build OK.")
    return True


def deploy():
    mods_dir = get_mods_dir()
    if not mods_dir:
        print("Set MODS_DIR in deploy.config or env. See scripts/deploy.config.example")
        return False
    if not mods_dir.exists():
        print(f"MODS_DIR does not exist: {mods_dir}")
        return False
    jars = list(MOD_BUILD_LIBS.glob("topzurdo-*.jar"))
    jars = [j for j in jars if not j.name.endswith("-sources.jar")]
    if not jars:
        print("No built JAR found. Run build first.")
        return False
    jar = jars[0]
    for old in mods_dir.glob("topzurdo-*.jar"):
        old.unlink()
        print(f"Removed old: {old.name}")
    dest = mods_dir / jar.name
    shutil.copy2(jar, dest)
    print(f"Deployed: {jar.name} -> {dest}")
    return True


def main():
    force = "--force" in sys.argv
    build_only = "--build-only" in sys.argv
    hash_only = "--hash-only" in sys.argv

    if hash_only:
        print(source_hash())
        return 0

    if not build(force=force):
        return 1
    if build_only:
        return 0
    if not deploy():
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
