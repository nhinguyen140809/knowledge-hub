"""Config-change propagation: how long a new grant takes to take effect (no restart).

The scoped principal starts without read access to the target source; a query narrowed
to that source returns nothing. The harness grants access, then polls the same query and
times how long until hits appear. The report targets <= 5s, governed by the ACL cache
TTL. The grant is revoked afterwards so the run is repeatable.
"""

from __future__ import annotations

import time

from ..context import Context
from ..metrics import ScenarioResult, check


def run(ctx: Context) -> ScenarioResult:
    cfg = ctx.scenario_cfg("config_propagation")
    th = ctx.thresholds["config_propagation"]
    admin = ctx.admin()
    principal = cfg["principal"]
    target = cfg["target_source"]
    probe = cfg["probe_query"]
    interval = cfg.get("poll_interval_s", 0.5)
    timeout = cfg.get("timeout_s", 20)
    client = ctx.client_for(principal)

    # Precondition: scoped caller cannot yet read the target source.
    before = client.query(probe, source_id=target, top_k=5)
    if before.get("hits"):
        # Clean up a leftover grant from an interrupted previous run, then retry.
        admin.revoke_grant(principal, [target])
        time.sleep(th["effective_within_s"]["value"] + 2)
        before = client.query(probe, source_id=target, top_k=5)
        if before.get("hits"):
            return ScenarioResult(
                name="config_propagation",
                error=f"{principal} already reads {target}; cannot measure propagation.",
            )

    elapsed = float("nan")
    try:
        t0 = time.perf_counter()
        admin.grant(principal, [target])
        deadline = t0 + timeout
        while time.perf_counter() < deadline:
            resp = client.query(probe, source_id=target, top_k=5)
            if resp.get("hits"):
                elapsed = time.perf_counter() - t0
                break
            time.sleep(interval)
    finally:
        admin.revoke_grant(principal, [target])

    result = ScenarioResult(
        name="config_propagation",
        metrics={"target_source": target, "effective_after_s": elapsed},
    )
    if elapsed != elapsed:  # nan -> never took effect
        result.error = f"grant did not take effect within {timeout}s"
    else:
        result.checks = [check("grant effective after", elapsed, th["effective_within_s"], " s")]
    return result
