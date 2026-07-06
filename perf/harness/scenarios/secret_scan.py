"""Secret scan: regex sweep of application source for hardcoded credentials.

Static, so it needs no running system. Patterns and placeholder allow-substrings live in
data/secret-patterns.yaml. A finding is any pattern match not cleared by an allow
substring. The report requires zero findings. This is a lightweight gate — the README
shows how to swap in gitleaks/trufflehog for a thorough scan.
"""

from __future__ import annotations

import re
from pathlib import Path

from ..config import REPO_ROOT, load_yaml
from ..context import Context
from ..metrics import ScenarioResult, check

_TEXT_SUFFIXES = {".java", ".yml", ".yaml", ".properties", ".xml", ".kt", ".md", ".env", ".sh"}


def run(ctx: Context) -> ScenarioResult:
    cfg = ctx.scenario_cfg("secret_scan")
    th = ctx.thresholds["secret_scan"]
    rules = load_yaml(f"{ctx.data_dir}/secret-patterns.yaml")
    patterns = [(p["name"], re.compile(p["regex"])) for p in rules["patterns"]]
    allow = [s.lower() for s in rules.get("allow_substrings", [])]

    findings: list[dict[str, object]] = []
    for rel in cfg.get("scan_paths", ["src/main"]):
        root = REPO_ROOT / rel
        if not root.exists():
            continue
        for path in root.rglob("*"):
            if not path.is_file() or path.suffix.lower() not in _TEXT_SUFFIXES:
                continue
            try:
                text = path.read_text(encoding="utf-8", errors="ignore")
            except Exception:
                continue
            for lineno, line in enumerate(text.splitlines(), 1):
                low = line.lower()
                if any(a in low for a in allow):
                    continue
                for name, rx in patterns:
                    if rx.search(line):
                        findings.append({
                            "file": str(path.relative_to(REPO_ROOT)),
                            "line": lineno,
                            "rule": name,
                        })

    result = ScenarioResult(
        name="secret_scan",
        metrics={"scanned_paths": cfg.get("scan_paths"), "findings": findings, "count": len(findings)},
    )
    result.checks = [check("hardcoded-secret findings", len(findings), th["findings"])]
    for f in findings[:20]:
        result.notes.append(f"{f['file']}:{f['line']} [{f['rule']}]")
    return result
