"""Scenario registry: name -> run(ctx) -> ScenarioResult.

Order here is the run order. Each module owns one acceptance criterion from the
evaluation chapter. Selection/skipping is by these names on the command line.
"""

from __future__ import annotations

from typing import Callable

from ..context import Context
from ..metrics import ScenarioResult
from . import (
    acl_overhead,
    availability,
    concurrency,
    config_propagation,
    incremental,
    latency,
    memory,
    quality,
    secret_scan,
)

# Config key in scenarios.yaml -> callable. Some modules cover a differently named key.
REGISTRY: dict[str, Callable[[Context], ScenarioResult]] = {
    "latency": latency.run,
    "concurrency": concurrency.run,
    "incremental": incremental.run,
    "acl": acl_overhead.run,
    "config_propagation": config_propagation.run,
    "quality": quality.run,
    "availability": availability.run,
    "memory": memory.run,
    "secret_scan": secret_scan.run,
}

ORDER = list(REGISTRY.keys())
