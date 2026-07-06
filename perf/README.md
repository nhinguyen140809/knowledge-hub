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
- **Outbound network access to GitHub** — `eval-corpus` is a real public repo (see
  `data/sources.yaml`); the app container clones it on `seed`.

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
- **`seed`** is the data setup: it registers the two sources, indexes them (the slow,
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
    sources.yaml        # sources to register: eval-corpus is a real pinned GIT repo,
                         # perf-incremental an FS folder under the /perf-data mount
    principals.yaml     # query principals + their grants
    queries.yaml        # latency/concurrency workload (free-text queries)
    gold-set.json       # labelled queries for Recall@10 / MRR, hand-checked against
                         # eval-corpus's pinned commit (see sources.yaml)
    secret-patterns.yaml# regex rules for the secret scan
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

## Evaluation: two languages, two layers, three quality test setups

"Evaluation" here spans two languages on purpose — each tests something the other
structurally can't.

**Python (this harness, 9 scenarios total)** drives the **deployed** stack purely over its
public REST API — the same vantage point a real client has (auth, HTTP, connection
pooling, container boundaries all included). `quality` is one of the 9: it grades the
**live hybrid pipeline** against 28 hand-written queries
([`data/gold-set.json`](data/gold-set.json)), each checked by hand against a specific file
in a real, small (~9k LOC) public repository — [`sindresorhus/globby`], pinned to commit
[`47e7f65`][globby-commit] and registered as the `eval-corpus` source over the real Git
connector (see [`data/sources.yaml`](data/sources.yaml)). Gate: Recall@10 ≥ 0.85, MRR ≥
0.70. This is the number ch.7 of the report cites, because it's the only one measured
against the system as it's actually deployed.

**Java (`src/test/java/com/knowledgehub/…`) has three separate test classes**, each with
a distinct job — they look similar (all index a corpus and query it) but none is
redundant with the others or with the Python `quality` scenario:

| Test class | Embedding | Corpus | Gold set | What it actually proves |
|---|---|---|---|---|
| `eval.RetrievalEvalTests` | mocked (deterministic, offline) | curated markdown fixture, 26 files (`src/test/resources/eval/corpus`) | 52 queries (`eval/gold-set.json`) | The **scoring/ranking logic itself** is correct — hybrid must never rank worse than semantic-only. Runs in every `mvn test` / CI; embeddings are fake, so this says nothing about real quality. |
| `eval.RetrievalEvalThresholdTests` | **real** provider, opt-in (`EVAL_ASSERT_THRESHOLDS=true`) | the same pinned `globby` repo as the Python `quality` scenario | 28 queries (`eval/gold-set-git.json`) | Recall@10 ≥ 0.85, MRR ≥ 0.70, and hybrid's MRR beats semantic-only by ≥ 0.03 — but run as a plain JVM process, not the deployed container. Its job is catching **Spring/embedding-config wiring bugs** (API key, base URL, dimension, active profile) that only surface when the JVM itself talks to the real provider — the docker container resolves that config differently (via `.env`/`docker-compose`), so the Python harness can never see this class of bug. |
| `sync.GitSourceLiveIndexingTests` | mocked | live clone of a real public repo, opt-in (`GIT_LIVE_TEST=true`) | none (no Recall/MRR) | Purely **functional**: the Git connector's clone/checkout/commit-indexing path works end to end against real history, independent of embedding quality. |

`just test-mock` runs the always-on suite (includes `RetrievalEvalTests`); `just
test-real` and `just test-git-live` run the two opt-in ones.

[`sindresorhus/globby`]: https://github.com/sindresorhus/globby
[globby-commit]: https://github.com/sindresorhus/globby/commit/47e7f658b87c1f48a4e62600a47dc0eca6dae249

---

## Reproducing / resetting

`seed` is idempotent for sources and principals but issues a fresh credential each run.
For a clean slate, tear the stack down (this drops the volumes) and start over:

```bash
docker compose -f docker-compose.yml -f perf/compose.perf.yml down -v
```

Leaving three containers running for hours (e.g. an idle Codespace) burns RAM/CPU for
nothing. If you don't need a clean slate, `just pause` (repo root) stops all three without
deleting them or their data — `just resume` brings back the exact same stack in seconds,
no rebuild, no re-seed.
