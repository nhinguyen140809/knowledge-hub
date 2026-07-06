"""ACL latency overhead: p95 of a scoped (restricted) caller vs a broad caller.

Both callers are authenticated members, so the ACL pre-filter is present in both arms —
the public API has no filter-off path. This measures the marginal cost of a restrictive
readable-source set over a permissive one. The report targets <= 10% overhead.
"""

from __future__ import annotations

from ..context import Context
from ..metrics import ScenarioResult, check, summarize


def _measure(ctx: Context, principal: str, top_k: int, iterations: int) -> list[float]:
    client = ctx.client_for(principal)
    out: list[float] = []
    for it in range(iterations):
        for q in ctx.queries:
            text = q if it == 0 else f"{q} (acl {it})"
            _, ms = client.timed_query(text, top_k=top_k)
            out.append(ms)
    return out


def run(ctx: Context) -> ScenarioResult:
    cfg = ctx.scenario_cfg("acl")
    th = ctx.thresholds["acl"]
    top_k = cfg.get("top_k", 10)
    iterations = cfg.get("iterations", 3)

    broad = summarize(_measure(ctx, cfg["broad_principal"], top_k, iterations))
    scoped = summarize(_measure(ctx, cfg["scoped_principal"], top_k, iterations))

    overhead_pct = ((scoped["p95"] - broad["p95"]) / broad["p95"] * 100.0) if broad["p95"] else float("nan")
    result = ScenarioResult(
        name="acl_overhead",
        metrics={"broad_ms": broad, "scoped_ms": scoped, "overhead_pct": overhead_pct},
    )
    result.checks = [check("scoped p95 overhead vs broad", overhead_pct, th["overhead_pct"], " %")]
    result.notes.append(
        "Both arms apply the ACL pre-filter; the API exposes no filter-off baseline, so "
        "this is the cost of a restrictive vs permissive readable set, not filter on/off."
    )
    return result
