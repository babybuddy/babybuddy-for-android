import subprocess
import sys
import datetime

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
    ref_repo_path = script_dir / "babybuddy-repo"
    if not ref_repo_path.exists():
        subprocess.check_call(
            [
                "git",
                "clone",
                "https://github.com/babybuddy/babybuddy.git",
                str(ref_repo_path),
            ],
            cwd=str(script_dir),
        )
    else:
        subprocess.check_call(["git", "pull"], cwd=str(ref_repo_path))

    for version in VERSION_ARRAY:
        print("Building babybuddy version: " + version)
        version_path = script_dir / version
        if not version_path.exists():
            subprocess.check_call(
                ["git", "clone", str(ref_repo_path), str(version_path)],
                cwd=str(script_dir),
            )
            subprocess.check_call(
                ["git", "checkout", "-b", "testbranch", version], cwd=str(version_path)
            )

        subprocess.check_call(
            ["/bin/bash", str(script_dir / "compile.sh")], cwd=str(version_path)
        )
