"""Incremental-update time: change N files, then time one incremental sync.

The incremental source is bind-mounted, so writing to its files on the host is what the
app re-reads. sync() is synchronous, so its wall-clock is the update latency. The report
targets <= 2 minutes for a change of <= 50 files (excluding embedding-provider latency).
"""

from __future__ import annotations

import time

from ..context import Context
from ..metrics import ScenarioResult, check


def run(ctx: Context) -> ScenarioResult:
    cfg = ctx.scenario_cfg("incremental")
    th = ctx.thresholds["incremental"]
    admin = ctx.admin()
    source_id = cfg["source"]
    n = cfg.get("change_files", 25)

    src_dir = ctx.source_path_on_host(source_id)
    files = sorted(src_dir.glob("*.md"))
    if not files:
        return ScenarioResult(name="incremental", error=f"no files under {src_dir}")

    # Touch up to n files with fresh content so the sync has real work to do.
    stamp = time.strftime("%H:%M:%S")
    changed = files[:n]
    for i, f in enumerate(changed):
        f.write_text(
            f"# incremental doc\n\nRevision {stamp} change {i}. "
            f"Line added to force a content-hash change and re-embed.\n"
        )

    t0 = time.perf_counter()
    resp = admin.sync(source_id)
    seconds = time.perf_counter() - t0

    result = ScenarioResult(
        name="incremental",
        metrics={"changed_files": len(changed), "sync_seconds": seconds, "sync_response": resp},
    )
    result.checks = [check(f"sync of {len(changed)} changed files", seconds, th["sync_seconds"], " s")]
    return result
