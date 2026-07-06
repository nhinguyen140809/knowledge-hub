"""Load YAML config/data with ${ENV:default} interpolation.

Every string value of the form ${NAME} or ${NAME:default} is replaced with the
environment variable NAME, falling back to default (or "" when no default is given).
This keeps secrets (API keys) out of the committed YAML while still letting the files
document every knob.
"""

from __future__ import annotations

import os
import re
from pathlib import Path
from typing import Any

import yaml

_ENV_PATTERN = re.compile(r"\$\{([A-Za-z_][A-Za-z0-9_]*)(?::([^}]*))?\}")

# Repo layout: this file is perf/harness/config.py, so the repo root is three levels up.
HARNESS_DIR = Path(__file__).resolve().parent
PERF_DIR = HARNESS_DIR.parent
REPO_ROOT = PERF_DIR.parent


def _load_dotenv(path: Path) -> None:
    """Populate os.environ from a KEY=VALUE .env file, without overriding vars
    already exported in the shell (so `export API_KEY=…` still wins)."""
    if not path.exists():
        return
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        os.environ.setdefault(key.strip(), value.strip())


_load_dotenv(REPO_ROOT / ".env")


def _interpolate(value: Any) -> Any:
    if isinstance(value, str):
        def repl(match: re.Match[str]) -> str:
            name, default = match.group(1), match.group(2)
            return os.environ.get(name, default if default is not None else "")

        return _ENV_PATTERN.sub(repl, value)
    if isinstance(value, dict):
        return {k: _interpolate(v) for k, v in value.items()}
    if isinstance(value, list):
        return [_interpolate(v) for v in value]
    return value


def load_yaml(path: str | Path) -> Any:
    """Read a YAML file relative to perf/ (or an absolute path) with env interpolation."""
    p = Path(path)
    if not p.is_absolute():
        p = PERF_DIR / p
    with open(p, "r", encoding="utf-8") as f:
        raw = yaml.safe_load(f)
    return _interpolate(raw)


def load_json(path: str | Path) -> Any:
    import json

    p = Path(path)
    if not p.is_absolute():
        p = PERF_DIR / p
    with open(p, "r", encoding="utf-8") as f:
        return json.load(f)
