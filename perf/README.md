# Performance & evaluation harness

Data-driven automation for the acceptance criteria in
[docs/report/7-evaluation.tex](../docs/report/7-evaluation.tex): query latency,
concurrency, incremental-update time, ACL overhead, config-change propagation,
retrieval quality (Recall@10 / MRR), availability during re-index, container memory,
and a hardcoded-secret scan.

The harness drives the **running** system over its REST API — it never reaches into the
database. Everything tunable (thresholds, workloads, sources, principals, secret
patterns) lives in [`config/`](config/) and [`data/`](data/); the Python in
[`harness/`](harness/) only reads those files and runs. Add a query, change a threshold
or add a source by editing YAML — no code change.

---

## What you need (the environment this runs in)

- **Docker + Docker Compose** — to run the app, Neo4j and Qdrant.
- **Python 3.10+** — the harness itself (deps: `httpx`, `PyYAML`).
- **An OpenAI-compatible embedding endpoint** — the app embeds every indexed chunk and
  every query. Set these in the repo-root `.env` (see `.env.example`):
  - `EMBEDDING_BASE_URL`, `EMBEDDING_API_KEY`, `EMBEDDING_MODEL`, `EMBEDDING_DIMENSION`
  - For a fully local, cost-free and **embedding-latency-stable** run, point these at a
    local server (LM Studio / TEI / llama.cpp) and set `EMBEDDING_PROVIDER=local`. This
    also makes the latency numbers independent of a cloud provider, as the report intends.
- **`API_KEY`** in the environment / `.env` — the bootstrap admin secret. The harness
  authenticates administration with it and reads it from `${API_KEY}`.

The harness assumes the app is reachable at `http://localhost:8000` (override with
`KH_BASE_URL`). Container memory reads need the `docker` CLI on the host (skipped if absent).

---

## Run it (two commands after the stack is up)

From the **repo root**, start the stack with the perf data mount:

```bash
docker compose -f docker-compose.yml -f perf/compose.perf.yml up -d --build
```

Then, from **`perf/`**:

```bash
python3 -m venv .venv && ./.venv/bin/pip install -r requirements.txt
source .venv/bin/activate

python run.py seed     # register sources, index them, create query principals (run once)
python run.py bench    # run every enabled scenario, write results/report.{json,md}
```

Or fold seed+bench into one: `python run.py all`.

A `Makefile` wraps these: `make up`, `make seed`, `make bench`, `make all`, `make down`.

`bench` exits non-zero if any threshold fails, so it drops straight into CI.

### Handy variants

```bash
python run.py doctor                    # is the stack reachable? is API_KEY set?
python run.py bench --only latency,quality
python run.py bench --skip availability,memory
```

---

## Why build and data-seed are separate

- **`up`** builds the image and starts the three containers. Do it once; leave it running.
- **`seed`** is the data setup: it registers the two FS sources, indexes them (the slow,
  embedding-bound step), creates the `member-broad` / `member-scoped` principals, grants
  them, and issues their credentials — writing the secrets to `results/state.json`.
- **`bench`** reads `state.json` and measures. It never re-indexes the corpus (except the
  small incremental/availability fixtures, by design), so you can iterate on scenarios and
  thresholds without paying the indexing cost again.

Keeping them apart means the expensive image build and corpus indexing happen once, while
the measurement step is cheap to re-run.

---

## Layout

```
perf/
  config/
    settings.yaml       # base URL, admin token (${API_KEY}), timeouts, service names
    thresholds.yaml     # the pass/fail numbers from ch.7 — edit to move the gate
    scenarios.yaml      # which scenarios run + their knobs (workers, iterations, …)
  data/
    sources.yaml        # FS sources to register (paths under the /perf-data mount)
    principals.yaml     # query principals + their grants
    queries.yaml        # latency/concurrency workload (free-text queries)
    gold-set.json       # labelled queries for Recall@10 / MRR (reused from the app eval)
    secret-patterns.yaml# regex rules for the secret scan
    corpus/             # knowledge corpus, indexed as source `eval-corpus`
    incremental/        # small mutable source for incremental + availability tests
  harness/              # the runner (config loader, HTTP client, scenarios, reporting)
  compose.perf.yml      # bind-mounts data/ into the app container
  run.py                # CLI: seed | bench | all | doctor
  results/              # report.json, report.md, state.json (gitignored)
```

---

## Scenarios and how each is measured

| Scenario | What it measures | Threshold (config) |
|---|---|---|
| `latency` | Uncached p50/p95 (full pipeline, unique query = cache miss) and cached p95 (identical repeat, served from the result cache) | p50 ≤ 800 ms, p95 ≤ 2 s, cached p95 ≤ 100 ms |
| `concurrency` | p95 under `workers` parallel callers vs a serial p95 baseline | slowdown ≤ 2× |
| `incremental` | Wall-clock of one incremental sync after editing N ≤ 50 files | ≤ 120 s |
| `acl` | p95 of a scoped (ACL-restricted) caller vs a broad caller | overhead ≤ 10 % |
| `config_propagation` | Time from a new grant until queries return the newly-readable source | ≤ 5 s |
| `quality` | Recall@10 and MRR over the labelled gold set (live hybrid pipeline) | Recall ≥ 0.85, MRR ≥ 0.70 |
| `availability` | Query success rate while a source re-indexes under load | ≥ 0.99 |
| `memory` | Container RAM snapshot via `docker stats` | reported only |
| `secret_scan` | Hardcoded-secret regex sweep of `../src/main` | 0 findings |

### Honest limitations

- **Latency includes the query-embedding call.** The API embeds the query text
  synchronously, so cold latency carries the embedding round-trip. Use a local embedding
  model (above) for the embedding-independent number the report describes; the **cached**
  latency already excludes embedding entirely (the cache short-circuits the pipeline).
- **ACL overhead is restrictive-vs-permissive, not filter on/off.** Every authenticated
  caller has the ACL pre-filter applied; the public API exposes no filter-off path. The
  scenario therefore compares a narrow readable set against a wide one.
- **`secret_scan` is a lightweight gate.** For a thorough sweep, run a dedicated tool
  instead and treat 0 findings as the same gate, e.g.:
  ```bash
  docker run --rm -v "$(git rev-parse --show-toplevel)":/repo zricethezav/gitleaks:latest detect -s /repo
  ```
- **Semantic-only vs hybrid quality** stays in the app's Java eval tests
  (`src/test/java/com/knowledgehub/eval`) — the semantic-only path is not exposed over the
  API. This harness covers the live hybrid Recall@10 / MRR.
- **Large-scale (millions of chunks) and network-isolation** experiments from the
  future-work chapter need a much larger corpus / a packet-capture environment and are out
  of scope here; the scenarios are shaped for a team-on-a-product deployment.

---

## Reproducing / resetting

`seed` is idempotent for sources and principals but issues a fresh credential each run.
For a clean slate, tear the stack down (this drops the volumes) and start over:

```bash
docker compose -f docker-compose.yml -f perf/compose.perf.yml down -v
```
