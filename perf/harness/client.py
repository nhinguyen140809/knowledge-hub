"""Thin HTTP client for the Knowledge Hub REST API.

One instance carries one bearer token, so the harness holds several: an admin client
for administration and one per query-side principal. Every method maps to a documented
endpoint; timed_query returns wall-clock latency in milliseconds alongside the body.
"""

from __future__ import annotations

import time
from typing import Any

import httpx


class ApiError(RuntimeError):
    def __init__(self, method: str, url: str, status: int, body: str):
        super().__init__(f"{method} {url} -> {status}: {body[:300]}")
        self.status = status


class KHClient:
    def __init__(self, base_url: str, api_prefix: str, token: str, timeout_s: float):
        self._api = f"{base_url.rstrip('/')}{api_prefix}"
        self._root = base_url.rstrip("/")
        headers = {"Authorization": f"Bearer {token}"} if token else {}
        headers["Content-Type"] = "application/json"
        self._http = httpx.Client(headers=headers, timeout=timeout_s)

    def close(self) -> None:
        self._http.close()

    def __enter__(self) -> "KHClient":
        return self

    def __exit__(self, *exc: object) -> None:
        self.close()

    # --- low level ---------------------------------------------------------

    def _request(self, method: str, path: str, *, root: bool = False, **kw: Any) -> httpx.Response:
        base = self._root if root else self._api
        url = f"{base}{path}"
        resp = self._http.request(method, url, **kw)
        if resp.status_code >= 400:
            raise ApiError(method, url, resp.status_code, resp.text)
        return resp

    # --- health ------------------------------------------------------------

    def health(self) -> bool:
        try:
            r = self._http.get(f"{self._root}/actuator/health", timeout=5)
            return r.status_code == 200 and r.json().get("status") == "UP"
        except Exception:
            return False

    # --- retrieval ---------------------------------------------------------

    def query(self, text: str, top_k: int | None = None, source_id: str | None = None,
              ref: str | None = None, type: str | None = None) -> dict[str, Any]:
        body = {"text": text, "topK": top_k, "sourceId": source_id, "ref": ref, "type": type}
        body = {k: v for k, v in body.items() if v is not None}
        return self._request("POST", "/query", json=body).json()

    def timed_query(self, text: str, **kw: Any) -> tuple[dict[str, Any], float]:
        """Return (response_body, elapsed_ms) for a single query."""
        start = time.perf_counter()
        body = self.query(text, **kw)
        elapsed_ms = (time.perf_counter() - start) * 1000.0
        return body, elapsed_ms

    # --- source admin (admin token) ---------------------------------------

    def create_source(self, spec: dict[str, Any]) -> None:
        body = {
            "id": spec["id"],
            "type": spec["type"],
            "uriOrPath": spec["uri_or_path"],
            "ref": spec.get("ref"),
            "include": spec.get("include", []),
            "ignore": spec.get("ignore", []),
            "name": spec.get("name"),
            "description": spec.get("description"),
        }
        body = {k: v for k, v in body.items() if v is not None}
        try:
            self._request("POST", "/admin/sources", json=body)
        except ApiError as e:
            if e.status not in (409,):  # already registered -> fine (idempotent seed)
                raise

    def sync(self, source_id: str) -> dict[str, Any]:
        return self._request("POST", f"/admin/sources/{source_id}/sync").json()

    def status(self, source_id: str) -> dict[str, Any]:
        return self._request("GET", f"/admin/sources/{source_id}/status").json()

    # --- access admin (admin token) ---------------------------------------

    def create_principal(self, principal_id: str, ptype: str, role: str) -> None:
        body = {"principalId": principal_id, "type": ptype, "role": role}
        try:
            self._request("POST", "/admin/principals", json=body)
        except ApiError as e:
            if e.status not in (409,):
                raise

    def issue_credential(self, principal_id: str, name: str) -> str:
        body = {"name": name}
        r = self._request("POST", f"/admin/principals/{principal_id}/credentials", json=body)
        return r.json()["secret"]

    def grant(self, principal_id: str, source_ids: list[str]) -> None:
        self._request("POST", "/admin/grants",
                      json={"principalId": principal_id, "sourceIds": source_ids})

    def revoke_grant(self, principal_id: str, source_ids: list[str]) -> None:
        self._request("POST", "/admin/grants/revoke",
                      json={"principalId": principal_id, "sourceIds": source_ids})
