#!/usr/bin/env python3
"""
Watch mod source and run build_deploy on change.
Usage: python watch_build_deploy.py [--poll]
  --poll: use polling instead of watchdog (fallback if watchdog not installed)
"""

import os
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MOD_SRC = ROOT / "mod" / "src"
BUILD_DEPLOY = Path(__file__).resolve().parent / "build_deploy.py"


def run_build_deploy():
    import subprocess
    r = subprocess.run([sys.executable, str(BUILD_DEPLOY)], cwd=ROOT)
    return r.returncode == 0


def hash_tree(p: Path):
    h = []
    for f in sorted(p.rglob("*")):
        if f.is_file() and f.suffix in (".java", ".json"):
            try:
                h.append((str(f), f.stat().st_mtime_ns))
            except OSError:
                pass
    return tuple(h)


def watch_poll(interval=1.0):
    print(f"Watching {MOD_SRC} (poll every {interval}s). Ctrl+C to stop.")
    last = hash_tree(MOD_SRC)
    while True:
        time.sleep(interval)
        current = hash_tree(MOD_SRC)
        if current != last:
            last = current
            print("Change detected.")
            run_build_deploy()


def watch_watchdog():
    try:
        from watchdog.observers import Observer
        from watchdog.events import FileSystemEventHandler
    except ImportError:
        print("Install watchdog: pip install watchdog")
        return False

    debounce_timer = [None]
    debounce_sec = 0.8

    class Handler(FileSystemEventHandler):
        def on_modified(self, event):
            if event.is_directory:
                return
            p = Path(event.src_path)
            if p.suffix not in (".java", ".json"):
                return
            import threading
            if debounce_timer[0] is not None:
                debounce_timer[0].cancel()
            def run():
                run_build_deploy()
            debounce_timer[0] = threading.Timer(debounce_sec, run)
            debounce_timer[0].start()

    observer = Observer()
    observer.schedule(Handler(), str(MOD_SRC), recursive=True)
    observer.start()
    print(f"Watching {MOD_SRC}. Ctrl+C to stop.")
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        observer.stop()
    observer.join()
    return True


def main():
    if "--poll" in sys.argv:
        watch_poll()
    else:
        if not watch_watchdog():
            print("Falling back to poll mode.")
            watch_poll()


if __name__ == "__main__":
    main()
