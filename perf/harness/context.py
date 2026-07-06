"""Shared run context passed to every scenario.

Holds parsed config/data and hands out API clients by principal id. The admin client
uses the bootstrap token; member clients use the credentials seed issued (read back
from results/state.json), so bench can run in a separate process from seed.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from .client import KHClient
from .config import PERF_DIR, load_json, load_yaml


class Context:
    def __init__(self, config_dir: str = "config", data_dir: str = "data"):
        self.settings = load_yaml(f"{config_dir}/settings.yaml")
        self.thresholds = load_yaml(f"{config_dir}/thresholds.yaml")
        self.scenarios = load_yaml(f"{config_dir}/scenarios.yaml")
        self.data_dir = data_dir
        self.sources = load_yaml(f"{data_dir}/sources.yaml")["sources"]
        self.principals = load_yaml(f"{data_dir}/principals.yaml")["principals"]
        self.queries = load_yaml(f"{data_dir}/queries.yaml")["queries"]
        self.gold_set = load_json(f"{data_dir}/gold-set.json")

        self.base_url = self.settings["base_url"]
        self.api_prefix = self.settings["api_prefix"]
        self.timeout = float(self.settings["request_timeout_s"])
        self.output_dir = PERF_DIR / self.settings["output_dir"]
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.state_path = PERF_DIR / self.settings["state_file"]
        self._state: dict[str, Any] | None = None
        self._clients: dict[str, KHClient] = {}

    # --- clients -----------------------------------------------------------

    def admin(self) -> KHClient:
        return self._client_cached("__admin__", self.settings["admin_token"])

    def client_for(self, principal_id: str) -> KHClient:
        state = self.state()
        secret = state["credentials"].get(principal_id)
        if not secret:
            raise RuntimeError(
                f"No credential for '{principal_id}' in {self.state_path}. Run `seed` first."
            )
        return self._client_cached(principal_id, secret)

    def _client_cached(self, key: str, token: str) -> KHClient:
        if key not in self._clients:
            self._clients[key] = KHClient(self.base_url, self.api_prefix, token, self.timeout)
        return self._clients[key]

    def new_client_for(self, principal_id: str) -> KHClient:
        """A fresh, independent client (for use inside worker threads)."""
        secret = self.state()["credentials"][principal_id]
        return KHClient(self.base_url, self.api_prefix, secret, self.timeout)

    def close(self) -> None:
        for c in self._clients.values():
            c.close()

    # --- state (issued credentials + source ids) ---------------------------

    def state(self) -> dict[str, Any]:
        if self._state is None:
            if not self.state_path.exists():
                raise RuntimeError(f"{self.state_path} missing. Run `seed` before `bench`.")
            self._state = json.loads(self.state_path.read_text())
        return self._state

    def write_state(self, state: dict[str, Any]) -> None:
        self.state_path.write_text(json.dumps(state, indent=2))
        self._state = state

    # --- helpers -----------------------------------------------------------

    def scenario_cfg(self, name: str) -> dict[str, Any]:
        return self.scenarios.get(name, {})

    def source_path_on_host(self, source_id: str) -> Path:
        """Map a seeded FS source's container path (/perf-data/...) to the host path."""
        for s in self.sources:
            if s["id"] == source_id:
                container = s["uri_or_path"]
                rel = container.replace("/perf-data/", "", 1).strip("/")
                return PERF_DIR / "data" / rel
        raise KeyError(source_id)
