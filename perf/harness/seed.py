"""Seed step: register sources, index them, create query-side principals with grants.

Run once against a fresh stack (or re-run — source/principal creation is idempotent;
credentials are re-issued each time). Writes results/state.json so the separate bench
step can authenticate as the seeded members.
"""

from __future__ import annotations

import time

from .context import Context


def seed(ctx: Context) -> dict[str, object]:
    admin = ctx.admin()
    if not ctx.settings.get("admin_token"):
        raise RuntimeError(
            "admin_token is empty. Set API_KEY in the environment to the app's bootstrap key."
        )
    if not admin.health():
        raise RuntimeError(f"App not healthy at {ctx.base_url}. Start the stack first (see README).")

    # 1) Sources: register + index. sync() is synchronous, so it returns once indexed.
    for src in ctx.sources:
        print(f"  source {src['id']}: registering …")
        admin.create_source(src)
    for src in ctx.sources:
        print(f"  source {src['id']}: indexing …", flush=True)
        t0 = time.perf_counter()
        resp = admin.sync(src["id"])
        dt = time.perf_counter() - t0
        print(f"  source {src['id']}: indexed in {dt:.1f}s ({resp})")

    # 2) Principals + credentials + grants.
    credentials: dict[str, str] = {}
    stamp = time.strftime("%Y%m%d-%H%M%S")
    for p in ctx.principals:
        admin.create_principal(p["id"], p["type"], p["role"])
        if p.get("grants"):
            admin.grant(p["id"], p["grants"])
        secret = admin.issue_credential(p["id"], f"perf-{stamp}")
        credentials[p["id"]] = secret
        print(f"  principal {p['id']}: role={p['role']} grants={p.get('grants', [])}")

    state = {
        "seeded_at": stamp,
        "base_url": ctx.base_url,
        "sources": [s["id"] for s in ctx.sources],
        "credentials": credentials,
    }
    ctx.write_state(state)
    print(f"  state written to {ctx.state_path}")
    return state
