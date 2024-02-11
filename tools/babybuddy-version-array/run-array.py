import subprocess
import sys
import os
import signal
import time
from concurrent.futures import ThreadPoolExecutor

from pathlib import Path

script_dir = Path(__file__).absolute().parent

VERSION_ARRAY = []
with open("versions") as f:
    for line in f:
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        VERSION_ARRAY.append(line)

if __name__ == "__main__":
    START_PORT = 9000
    os.environ["DJANGO_SETTINGS_MODULE"] = "babybuddy.settings.development"

    processes = []
    try:
        with ThreadPoolExecutor() as tpool:
            for vi, version in enumerate(VERSION_ARRAY):
                print("Starting: " + version, "on port", START_PORT + vi)
                version_path = script_dir / version

                p = subprocess.Popen(
                    [
                        "pipenv",
                        "run",
                        "python3",
                        "manage.py",
                        "runserver",
                        f"0.0.0.0:{START_PORT + vi}",
                    ],
                    cwd=str(version_path),
                )
                processes.append(p)
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("Killing processes...")
        for p in processes:
            os.kill(p.pid, signal.SIGINT)
            p.wait()
        sys.exit(0)
