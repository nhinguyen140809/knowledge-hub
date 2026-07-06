"""Availability during re-index: query success rate while a source re-syncs under load.

Worker threads query continuously for duration_s; midway, the harness triggers a full
re-sync of a source. The metric is the fraction of queries that succeed while the write
path runs — a proxy for how non-blocking reads are during indexing. Report target here
is a high success rate (>= 0.99 by default).
"""

from __future__ import annotations

import threading
import time

from ..context import Context
from ..metrics import ScenarioResult, check


def run(ctx: Context) -> ScenarioResult:
    cfg = ctx.scenario_cfg("availability")
    th = ctx.thresholds["availability"]
    principal = cfg["principal"]
    source_id = cfg["source"]
    workers = cfg.get("workers", 10)
    duration = cfg.get("duration_s", 30)
    top_k = cfg.get("top_k", 10)

    stop = threading.Event()
    counters = {"ok": 0, "fail": 0}
    lock = threading.Lock()
    base = ctx.queries

    def worker(wid: int) -> None:
        client = ctx.new_client_for(principal)
        i = 0
        try:
            while not stop.is_set():
                text = f"{base[i % len(base)]} #a{wid}-{i}"
                i += 1
                try:
                    client.query(text, top_k=top_k)
                    with lock:
                        counters["ok"] += 1
                except Exception:
                    with lock:
                        counters["fail"] += 1
        finally:
            client.close()

    threads = [threading.Thread(target=worker, args=(w,), daemon=True) for w in range(workers)]
    for t in threads:
        t.start()

    # Let load settle, trigger a re-index mid-window, then keep loading.
    time.sleep(duration * 0.3)
    admin = ctx.admin()
    reindex_ok = True
    reindex_seconds = float("nan")
    try:
        r0 = time.perf_counter()
        admin.sync(source_id)
        reindex_seconds = time.perf_counter() - r0
    except Exception as e:  # noqa: BLE001
        reindex_ok = False
        note = str(e)
    time.sleep(max(0.0, duration - duration * 0.3))
    stop.set()
    for t in threads:
        t.join(timeout=10)

    total = counters["ok"] + counters["fail"]
    success_rate = counters["ok"] / total if total else float("nan")
    result = ScenarioResult(
        name="availability",
        metrics={
            "workers": workers,
            "duration_s": duration,
            "queries": total,
            "ok": counters["ok"],
            "fail": counters["fail"],
            "success_rate": success_rate,
            "reindex_seconds": reindex_seconds,
            "reindex_ok": reindex_ok,
        },
    )
    result.checks = [check("query success rate during re-index", success_rate, th["success_rate"])]
    if not reindex_ok:
        result.notes.append(f"re-index call failed: {note}")
    return result
