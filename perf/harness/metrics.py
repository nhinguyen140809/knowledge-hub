"""Statistics, result records, and threshold checks."""

from __future__ import annotations

import math
from dataclasses import dataclass, field
from typing import Any

_OPS = {
    "le": (lambda m, t: m <= t, "<="),
    "lt": (lambda m, t: m < t, "<"),
    "ge": (lambda m, t: m >= t, ">="),
    "gt": (lambda m, t: m > t, ">"),
}


def percentile(values: list[float], p: float) -> float:
    """Linear-interpolation percentile (p in 0..100). Empty -> nan."""
    if not values:
        return math.nan
    s = sorted(values)
    if len(s) == 1:
        return s[0]
    rank = (p / 100.0) * (len(s) - 1)
    lo = math.floor(rank)
    hi = math.ceil(rank)
    if lo == hi:
        return s[int(rank)]
    return s[lo] + (s[hi] - s[lo]) * (rank - lo)


def summarize(values: list[float]) -> dict[str, float]:
    if not values:
        return {"count": 0}
    return {
        "count": len(values),
        "min": min(values),
        "p50": percentile(values, 50),
        "p95": percentile(values, 95),
        "p99": percentile(values, 99),
        "max": max(values),
        "mean": sum(values) / len(values),
    }


@dataclass
class Check:
    """One measured value compared against a threshold from thresholds.yaml."""

    label: str
    measured: float
    op: str
    threshold: float
    unit: str = ""

    @property
    def passed(self) -> bool:
        fn = _OPS[self.op][0]
        if isinstance(self.measured, float) and math.isnan(self.measured):
            return False
        return fn(self.measured, self.threshold)

    def to_dict(self) -> dict[str, Any]:
        return {
            "label": self.label,
            "measured": self.measured,
            "op": self.op,
            "threshold": self.threshold,
            "unit": self.unit,
            "passed": self.passed,
        }

    def line(self) -> str:
        sym = _OPS[self.op][1]
        mark = "PASS" if self.passed else "FAIL"
        m = f"{self.measured:.1f}" if isinstance(self.measured, float) else str(self.measured)
        return f"    [{mark}] {self.label}: {m}{self.unit} (need {sym} {self.threshold}{self.unit})"


def check(label: str, measured: float, spec: dict[str, Any], unit: str = "") -> Check:
    """Build a Check from a thresholds.yaml entry {op, value}."""
    return Check(label, measured, spec["op"], spec["value"], unit)


@dataclass
class ScenarioResult:
    name: str
    metrics: dict[str, Any] = field(default_factory=dict)
    checks: list[Check] = field(default_factory=list)
    notes: list[str] = field(default_factory=list)
    skipped: bool = False
    error: str | None = None

    @property
    def passed(self) -> bool:
        if self.skipped:
            return True
        if self.error:
            return False
        return all(c.passed for c in self.checks)

    def to_dict(self) -> dict[str, Any]:
        return {
            "name": self.name,
            "skipped": self.skipped,
            "error": self.error,
            "passed": self.passed,
            "metrics": self.metrics,
            "checks": [c.to_dict() for c in self.checks],
            "notes": self.notes,
        }
