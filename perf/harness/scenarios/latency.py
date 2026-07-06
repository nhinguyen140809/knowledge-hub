"""Query latency: uncached p50/p95 over the full pipeline, plus cached-repeat p95.

Cold samples force a cache miss (distinct query text -> distinct cache key + a fresh
query embedding), so they exercise the whole retrieval pipeline. Warm samples repeat
an identical query, which the result cache serves without re-running the pipeline;
that is the sub-100ms target in the report.
"""

from __future__ import annotations

from ..context import Context
from ..metrics import ScenarioResult, check, summarize


def run(ctx: Context) -> ScenarioResult:
    cfg = ctx.scenario_cfg("latency")
    th = ctx.thresholds["latency"]
    client = ctx.client_for(cfg["principal"])
    top_k = cfg.get("top_k", 10)
    iterations = cfg.get("iterations", 1)
    queries = ctx.queries

    # Warmup — untimed, so JIT and connection setup do not skew the first samples.
    for q in queries[: cfg.get("warmup", 5)]:
        client.query(q, top_k=top_k)

    # Cold pass: unique text each time -> always a cache miss.
    cold: list[float] = []
    for it in range(iterations):
        for q in queries:
            text = q if it == 0 else f"{q} (run {it})"
            _, ms = client.timed_query(text, top_k=top_k)
            cold.append(ms)

    # Warm pass: prime once, then time the identical repeat -> cache hit.
    warm: list[float] = []
    for q in queries:
        client.query(q, top_k=top_k)  # prime
        _, ms = client.timed_query(q, top_k=top_k)
        warm.append(ms)

    cold_stats = summarize(cold)
    warm_stats = summarize(warm)
    result = ScenarioResult(
        name="latency",
        metrics={"cold_ms": cold_stats, "cached_ms": warm_stats},
    )
    result.checks = [
        check("uncached p50", cold_stats["p50"], th["p50_ms"], " ms"),
        check("uncached p95", cold_stats["p95"], th["p95_ms"], " ms"),
        check("cached p95", warm_stats["p95"], th["cache_p95_ms"], " ms"),
    ]
    result.notes.append(
        "Uncached latency includes the synchronous query-embedding call; use a local "
        "embedding model for an embedding-provider-independent number."
    )
    return result
