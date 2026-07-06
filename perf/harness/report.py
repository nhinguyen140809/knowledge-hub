"""Aggregate scenario results into a console summary, JSON and Markdown reports."""

from __future__ import annotations

import json
import time
from pathlib import Path

from .metrics import ScenarioResult


def _fmt_metric(v: object) -> str:
    if isinstance(v, float):
        return f"{v:.2f}"
    return str(v)


def console(results: list[ScenarioResult]) -> bool:
    print("\n" + "=" * 70)
    print("PERFORMANCE / EVALUATION SUMMARY")
    print("=" * 70)
    all_pass = True
    for r in results:
        if r.skipped:
            print(f"\n• {r.name}: SKIPPED ({'; '.join(r.notes) or 'disabled'})")
            continue
        status = "PASS" if r.passed else "FAIL"
        all_pass = all_pass and r.passed
        print(f"\n• {r.name}: {status}")
        if r.error:
            print(f"    ERROR: {r.error}")
        for c in r.checks:
            print(c.line())
        for n in r.notes:
            print(f"    note: {n}")
    print("\n" + "=" * 70)
    print("OVERALL:", "PASS" if all_pass else "FAIL")
    print("=" * 70)
    return all_pass


def write_reports(results: list[ScenarioResult], out_dir: Path) -> tuple[Path, Path]:
    stamp = time.strftime("%Y-%m-%d %H:%M:%S")
    payload = {
        "generated_at": stamp,
        "overall_pass": all(r.passed for r in results if not r.skipped),
        "scenarios": [r.to_dict() for r in results],
    }
    json_path = out_dir / "report.json"
    json_path.write_text(json.dumps(payload, indent=2))

    lines = [f"# Performance / evaluation report", "", f"_Generated {stamp}_", ""]
    lines.append("| Scenario | Result | Checks |")
    lines.append("|---|---|---|")
    for r in results:
        if r.skipped:
            lines.append(f"| {r.name} | skipped | {'; '.join(r.notes)} |")
            continue
        status = "✅ pass" if r.passed else "❌ fail"
        checks = "<br>".join(
            f"{'✓' if c.passed else '✗'} {c.label}: {_fmt_metric(c.measured)}{c.unit} "
            f"(need {c.op} {c.threshold}{c.unit})"
            for c in r.checks
        ) or (r.error or "—")
        lines.append(f"| {r.name} | {status} | {checks} |")
    md_path = out_dir / "report.md"
    md_path.write_text("\n".join(lines) + "\n")
    return json_path, md_path
