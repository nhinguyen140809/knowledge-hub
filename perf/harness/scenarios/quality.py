"""Retrieval quality: Recall@10 and MRR over the labelled gold set via the live API.

Mirrors the scoring in the app's EvalHarness (a hit counts when its path ends with the
gold-labelled relevant path). Runs the full hybrid pipeline through the query endpoint;
the semantic-only comparison stays in the app's Java eval tests (that path is not exposed
over the API). Report targets: Recall@10 >= 0.85, MRR >= 0.70.
"""

from __future__ import annotations

from ..context import Context
from ..metrics import ScenarioResult, check


def run(ctx: Context) -> ScenarioResult:
    cfg = ctx.scenario_cfg("quality")
    th = ctx.thresholds["quality"]
    client = ctx.client_for(cfg["principal"])
    top_k = cfg.get("top_k", 10)

    recall_sum = 0.0
    mrr_sum = 0.0
    misses: list[str] = []
    gold = ctx.gold_set
    for case in gold:
        resp = client.query(case["query"], top_k=top_k)
        paths = [h["metadata"].get("path") or "" for h in resp.get("hits", [])]
        rank = 0
        for i, p in enumerate(paths[:top_k]):
            if p.endswith(case["relevantPath"]):
                rank = i + 1
                break
        if rank > 0:
            recall_sum += 1
            mrr_sum += 1.0 / rank
        else:
            misses.append(case["id"])

    n = len(gold)
    recall = recall_sum / n if n else float("nan")
    mrr = mrr_sum / n if n else float("nan")
    result = ScenarioResult(
        name="quality",
        metrics={"gold_queries": n, "recall_at_10": recall, "mrr": mrr, "misses": misses},
    )
    result.checks = [
        check("Recall@10", recall, th["recall_at_10"]),
        check("MRR", mrr, th["mrr"]),
    ]
    if misses:
        result.notes.append(f"{len(misses)} queries had no relevant doc in top-{top_k}: {misses}")
    return result
