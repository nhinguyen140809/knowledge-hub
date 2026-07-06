#!/usr/bin/env python3
"""Entry point for the Knowledge Hub performance / evaluation harness.

Usage:
  python run.py seed                     # register sources, index, create principals
  python run.py bench                    # run all enabled scenarios, write reports
  python run.py bench --only latency,quality
  python run.py bench --skip availability
  python run.py all                      # seed then bench
  python run.py doctor                   # check the stack is reachable

Config lives in perf/config, data in perf/data. Nothing here is hardcoded that a YAML
file could hold instead.
"""

from __future__ import annotations

import argparse
import sys
import traceback

from harness import report as report_mod
from harness.context import Context
from harness.metrics import ScenarioResult
from harness.scenarios import ORDER, REGISTRY
from harness.seed import seed as run_seed


def _select(ctx: Context, only: str | None, skip: str | None) -> list[str]:
    names = list(ORDER)
    if only:
        wanted = {s.strip() for s in only.split(",") if s.strip()}
        names = [n for n in names if n in wanted]
    if skip:
        drop = {s.strip() for s in skip.split(",") if s.strip()}
        names = [n for n in names if n not in drop]
    return names


def cmd_seed(ctx: Context, _args: argparse.Namespace) -> int:
    print("Seeding …")
    run_seed(ctx)
    print("Seed complete.")
    return 0


def cmd_bench(ctx: Context, args: argparse.Namespace) -> int:
    results: list[ScenarioResult] = []
    for name in _select(ctx, args.only, args.skip):
        cfg = ctx.scenario_cfg(name)
        if not cfg.get("enabled", True):
            results.append(ScenarioResult(name=name, skipped=True, notes=["disabled in config"]))
            continue
        print(f"\n▶ running {name} …", flush=True)
        try:
            results.append(REGISTRY[name](ctx))
        except Exception as e:  # noqa: BLE001
            traceback.print_exc()
            results.append(ScenarioResult(name=name, error=f"{type(e).__name__}: {e}"))

    ok = report_mod.console(results)
    json_path, md_path = report_mod.write_reports(results, ctx.output_dir)
    print(f"\nReports: {json_path}  |  {md_path}")
    return 0 if ok else 1


def cmd_all(ctx: Context, args: argparse.Namespace) -> int:
    rc = cmd_seed(ctx, args)
    if rc != 0:
        return rc
    return cmd_bench(ctx, args)


def cmd_doctor(ctx: Context, _args: argparse.Namespace) -> int:
    admin = ctx.admin()
    healthy = admin.health()
    print(f"base_url         : {ctx.base_url}")
    print(f"admin_token set  : {bool(ctx.settings.get('admin_token'))}")
    print(f"health (UP)      : {healthy}")
    print(f"state file       : {ctx.state_path} ({'present' if ctx.state_path.exists() else 'missing'})")
    return 0 if healthy else 1


def main() -> int:
    parser = argparse.ArgumentParser(description="Knowledge Hub performance/evaluation harness")
    parser.add_argument("--config-dir", default="config")
    parser.add_argument("--data-dir", default="data")
    sub = parser.add_subparsers(dest="command", required=True)

    p_seed = sub.add_parser("seed", help="register sources, index, create principals")
    p_seed.set_defaults(func=cmd_seed)

    for name in ("bench", "all"):
        p = sub.add_parser(name, help="run scenarios" if name == "bench" else "seed then bench")
        p.add_argument("--only", help="comma-separated scenario names to run")
        p.add_argument("--skip", help="comma-separated scenario names to skip")
        p.set_defaults(func=cmd_bench if name == "bench" else cmd_all)

    p_doc = sub.add_parser("doctor", help="check the stack is reachable")
    p_doc.set_defaults(func=cmd_doctor)

    args = parser.parse_args()
    ctx = Context(config_dir=args.config_dir, data_dir=args.data_dir)
    try:
        return args.func(ctx, args)
    finally:
        ctx.close()


if __name__ == "__main__":
    sys.exit(main())
