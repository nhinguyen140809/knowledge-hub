"""Concurrent-query latency: p95 under parallel load vs a serial baseline.

Fires total_requests queries across `workers` threads (each thread its own client and
its own query text, so every request is a cache miss) and compares the concurrent p95
to a short serial p95. The report targets no more than a 2x slowdown at >= 20 callers.
"""

from __future__ import annotations

import itertools
from concurrent.futures import ThreadPoolExecutor

from ..context import Context
from ..metrics import ScenarioResult, check, summarize


def _fire(ctx: Context, principal: str, texts: list[str], top_k: int) -> list[float]:
    client = ctx.new_client_for(principal)
    out: list[float] = []
    try:
        for t in texts:
            _, ms = client.timed_query(t, top_k=top_k)
            out.append(ms)
    finally:
        client.close()
    return out


def run(ctx: Context) -> ScenarioResult:
    cfg = ctx.scenario_cfg("concurrency")
    th = ctx.thresholds["concurrency"]
    principal = cfg["principal"]
    top_k = cfg.get("top_k", 10)
    workers = cfg.get("workers", 20)
    total = cfg.get("total_requests", 200)

    # Distinct texts so nothing is served from cache under load.
    base = ctx.queries
    texts = [f"{base[i % len(base)]} #c{i}" for i in range(total)]

    # Serial baseline over a small slice.
    serial = _fire(ctx, principal, texts[: min(30, total)], top_k)
    serial_stats = summarize(serial)

    # Concurrent run: split the workload across worker threads.
    chunks: list[list[str]] = [[] for _ in range(workers)]
    for text, bucket in zip(texts, itertools.cycle(range(workers))):
        chunks[bucket].append(text)
    concurrent: list[float] = []
    with ThreadPoolExecutor(max_workers=workers) as pool:
        for part in pool.map(lambda c: _fire(ctx, principal, c, top_k), chunks):
            concurrent.extend(part)
    conc_stats = summarize(concurrent)

    ratio = conc_stats["p95"] / serial_stats["p95"] if serial_stats.get("p95") else float("nan")
    result = ScenarioResult(
        name="concurrency",
        metrics={
            "workers": workers,
            "total_requests": total,
            "serial_ms": serial_stats,
            "concurrent_ms": conc_stats,
            "p95_slowdown_ratio": ratio,
        },
    )
    result.checks = [check("concurrent p95 / serial p95", ratio, th["p95_slowdown_ratio"], "x")]
    result.notes.append(
        f"serial p95={serial_stats['p95']:.0f} ms, concurrent p95={conc_stats['p95']:.0f} ms "
        f"at {workers} workers"
    )
    return result
