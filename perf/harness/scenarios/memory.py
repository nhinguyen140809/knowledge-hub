"""Memory usage: a snapshot of container RAM via `docker stats` (no hard threshold).

Reads the resident memory of the app, graph and vector containers at the current load.
The report records these numbers rather than gating on them, so this scenario reports
only. Requires the docker CLI on the host; it is skipped cleanly when unavailable.
"""

from __future__ import annotations

import shutil
import subprocess

from ..context import Context
from ..metrics import ScenarioResult


def run(ctx: Context) -> ScenarioResult:
    if shutil.which("docker") is None:
        return ScenarioResult(name="memory", skipped=True, notes=["docker CLI not found"])

    project = ctx.settings.get("compose_project", "knowledge-hub")
    services = ctx.settings.get("services", {})
    try:
        out = subprocess.run(
            ["docker", "stats", "--no-stream", "--format", "{{.Name}}\t{{.MemUsage}}\t{{.MemPerc}}"],
            capture_output=True, text=True, timeout=30, check=True,
        ).stdout
    except Exception as e:  # noqa: BLE001
        return ScenarioResult(name="memory", skipped=True, notes=[f"docker stats failed: {e}"])

    wanted = {v for v in services.values()}
    rows: dict[str, dict[str, str]] = {}
    for line in out.strip().splitlines():
        parts = line.split("\t")
        if len(parts) != 3:
            continue
        name, usage, perc = parts
        # container names look like "<project>-<service>-1"
        for svc in wanted:
            if svc in name and (project in name or True):
                rows[svc] = {"container": name, "mem": usage.strip(), "mem_pct": perc.strip()}
    result = ScenarioResult(name="memory", metrics={"containers": rows})
    if not rows:
        result.notes.append("No matching containers found; is the stack running?")
    else:
        for svc, r in rows.items():
            result.notes.append(f"{svc}: {r['mem']} ({r['mem_pct']})")
    return result
