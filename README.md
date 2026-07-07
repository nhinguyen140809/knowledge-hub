# Knowledge Hub for AI Agents in SDLC

A **Knowledge Hub (GraphRAG)** service that acts as **long-term memory** and **context provider**
for AI agents across the software development life cycle. It ingests a software product's source
code, documents and git history, links them into a **knowledge graph + vector store**, and serves
**hybrid retrieval** to agents over **REST** and **MCP** — so an agent gets the exact code/doc
context it needs instead of re-discovering the project every session.

**Scope:** one shared hub per *product* (which may span multiple repositories), used by a single
team. Knowledge is shared across the team; access is controlled per source via an admin-managed ACL.

## Features

- **Ingestion** — source code, documents (Markdown / PDF / images), and git history, from Git
  repositories or plain filesystem folders.
- **Processing & indexing** — AST-aware chunking for code, structure-aware chunking for docs,
  embeddings via an external API, stored as vectors + a knowledge graph.
- **Knowledge linking** — structural code relations (calls, imports, inheritance…) and cross-artifact
  links (doc $\leftrightarrow$ code, requirement $\leftrightarrow$ code, commit $\leftrightarrow$ code), including cross-source links.
- **Hybrid retrieval** — semantic + keyword + graph traversal, fused into a single ranked result.
- **Sync & update** — incremental, content-hash based; evicts stale knowledge.
- **Access control** — authentication on every endpoint; per-source authorization (ACL).
- **Interfaces** — REST (query + admin) and an MCP server for agents.

## Architecture

- **Clean Architecture**, **packaged by feature** — each feature has `domain` (model + ports),
  `application` (services), and `infrastructure` (adapters) layers; dependencies point inward.
- **GraphRAG storage** — **Neo4j** holds the graph + keyword (BM25) index; **Qdrant** holds the
  vectors (behind a `VectorStore` port), linked by a shared `chunkId`.
- **Embedding** — external API (no local model), configurable provider/model.
- **Deployment** — modular monolith, one Docker image, runs via a single `docker-compose`.

Diagrams: see [`docs/diagrams/`](docs/diagrams) (deployment + logical architecture).

## Tech stack

| Area | Choice |
|---|---|
| Language / runtime | **Java 21** |
| Framework | **Spring Boot 3.5** |
| Build | **Maven** (wrapper `./mvnw` included) |
| Web / API | Spring Web, springdoc-openapi (Swagger UI) |
| Graph + vector | Spring Data **Neo4j** (graph + BM25), **Qdrant** (vectors) |
| AI / RAG | **Spring AI 1.1** — embedding, Qdrant vector store, MCP server, PDF/Markdown/Tika readers |
| Validation / ops | Bean Validation, Spring Boot Actuator |
| Testing | JUnit 5, **Testcontainers** (real Neo4j + Qdrant) |
| Evaluation | Python harness (`perf/`) — retrieval quality + performance over REST |
| Quality | Spotless (google-java-format), Checkstyle |
| Container | Docker, Docker Compose |
| CI | GitHub Actions |

## Prerequisites

- **JDK 21** — the Maven Wrapper (`./mvnw`) downloads Maven itself, so Maven need not be installed.
- **Docker + Docker Compose** — for the database, integration tests, and the full stack.
- *(optional)* [`just`](https://github.com/casey/just) — task runner (see [Dev scripts](#dev-scripts)).
  Install via `brew install just`, `cargo install just`, or the install script.

## Quickstart

```bash
# 1. configure secrets
cp .env.example .env            # set NEO4J_PASSWORD and EMBEDDING_API_KEY

# 2a. run the whole stack (app + Neo4j) in containers
just up                         # = docker compose up -d --build

# 2b. or run just the app locally against a local/compose Neo4j — hot-reloads on save
just dev                        # = ./mvnw spring-boot:run  (Spring Boot DevTools)

# verify
curl http://localhost:8000/actuator/health
open http://localhost:8000/docs # Swagger UI
```

## Project structure

```
.
├── src/
│   ├── main/
│   │   ├── java/com/knowledgehub/        # application code, packaged by feature
│   │   │   └── <feature>/                # e.g. ingestion / retrieval / graph / mcp
│   │   │       ├── domain/               #   model + ports (interfaces)
│   │   │       ├── application/          #   use-case services
│   │   │       └── infrastructure/       #   adapters (Neo4j, embedding API, REST, MCP)
│   │   └── resources/
│   │       ├── application.yml           # base config
│   │       └── application-{dev,prod,cloud}.yml
│   └── test/                             # Testcontainers-based tests
├── docs/                                 # SRS, design notes, diagrams, LaTeX report
├── perf/                                 # Python evaluation & performance harness
├── Dockerfile                            # multi-stage build → runtime image
├── docker-compose.yml                    # app + Neo4j + Qdrant
├── justfile                              # dev task runner
├── .mcp.json                             # MCP server config for agents (e.g. Claude Code)
├── .githooks/pre-commit                  # auto-format on commit
└── .github/workflows/ci.yml              # CI: format check + build + tests
```

## Dev scripts

Run `just` to list tasks.

| just | What it does |
|---|---|
| `just dev` | run the app locally with hot reload (auto-restart on code change) |
| `just build` | package the app (skips tests) |
| `just test` | run the test suite (Testcontainers; mocked embeddings) |
| `just test-real` | run the eval tests against the **real** embedding API (needs a key) |
| `just test-git-live` | live Git-indexing test against a real public repo (needs network) |
| `just lint` | check formatting + style (Spotless + Checkstyle) |
| `just format` | auto-format the code |
| `just verify` | format check + build + tests |
| `just deps` | print the dependency tree |
| `just up` / `just down` | start / stop the docker-compose stack (`down-v` also wipes data volumes) |
| `just pause` / `just resume` | stop / start containers **without** removing them (saves RAM/CPU when idle) |
| `just logs` / `just ps` | tail app logs / list stack containers |
| `just hooks` | enable the pre-commit format hook |

> `.env` is auto-loaded by `just` (via `dotenv-load`), so tasks pick up `EMBEDDING_API_KEY` etc.

> **Hot reload:** `just dev` runs the app on the host with Spring Boot **DevTools** — saving a source
> file triggers a fast in-JVM restart (~1s, not a container or full-JVM restart), so changes apply
> without a manual rerun. This covers the host run only; the app running *inside* a container
> (`just up`) needs `docker compose up --build` to pick up code changes.

> Note: Maven is **declarative** — dependencies live in `pom.xml` and `./mvnw` resolves them on build.
> Add a dependency by editing `pom.xml` (or via Spring Initializr / your IDE), not via a CLI command.

## Configuration

Behaviour is driven by **Spring Profiles** + environment variables (one artifact, config injected at
runtime). Precedence: env var > `application-<profile>.yml` > `application.yml`.

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` (`.env.example`) | Active profile: `dev` / `prod` / `cloud` |
| `SERVER_PORT` | `8000` | HTTP port |
| `API_KEY` | — | Bootstrap admin secret; also the bearer token for `/mcp` (blank = no admin seeded) |
| `SPRING_NEO4J_URI` | `bolt://localhost:7687` | Neo4j connection |
| `NEO4J_USERNAME` / `NEO4J_PASSWORD` | `neo4j` / `knowledgehub` | Neo4j credentials |
| `SPRING_AI_VECTORSTORE_QDRANT_HOST` / `_PORT` | `localhost` / `6334` | Qdrant (gRPC) connection |
| `QDRANT_COLLECTION` | `knowledge-embeddings` | Qdrant collection name |
| `EMBEDDING_PROVIDER` | `api` | `api` (remote) or `local` (OpenAI-compatible server) |
| `EMBEDDING_MODEL` | `text-embedding-3-small` | Embedding model (e.g. `voyage-3`) |
| `EMBEDDING_BASE_URL` | `https://api.openai.com` | OpenAI-compatible endpoint host (OpenAI / Voyage / local) |
| `EMBEDDING_API_KEY` | — | Embedding provider key (blank for a keyless local server) |
| `EMBEDDING_DIMENSION` | `1536` | Vector dimension — must match the model; sizes the Qdrant collection |
| `EMBEDDING_BATCH_SIZE` | `1000` | Max texts per embedding request (Voyage AI caps at 1000) |

Switch embedding provider/model by editing these env vars only — no code change or image rebuild.
Secrets (`EMBEDDING_API_KEY`, `NEO4J_PASSWORD`, bootstrap `API_KEY`) come from env / secret manager —
**never commit them** (`.env` is git-ignored; see `.env.example`).

## Testing

```bash
just test           # JUnit 5 + Testcontainers (spins real Neo4j + Qdrant containers; needs Docker)
just test-real      # eval tests against the real embedding API (needs EMBEDDING_API_KEY)
just test-git-live  # index a real public Git repo end-to-end (needs network)
```

The context test boots the full Spring context against throwaway Neo4j + Qdrant containers, so it
exercises the real wiring (vector store, MCP server, repositories). Retrieval-quality and
performance are evaluated separately by the Python harness in [`perf/`](perf) (see its README),
which drives the running stack over REST like a real client.

## Deployment (single docker-compose, VM or local)

```bash
cp .env.example .env
just up          # docker compose up -d --build
```

- Runs **app + Neo4j + Qdrant**; data persists in **named volumes** (`neo4j-data`, `neo4j-logs`,
  `qdrant-data`).
- Neo4j heap/pagecache and the app heap are **RAM-capped** in `docker-compose.yml` for local/small
  hosts — raise them on a real server.
- Self-host this compose on a VM (e.g. a small cloud VM) to avoid using laptop RAM; attach a block
  volume + backups if the VM can be recreated.
- On a resource-limited host (e.g. a Codespace), `just pause` stops the containers without deleting
  them so an idle stack stops burning RAM/CPU; `just resume` brings it back quickly.

## CI

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on **every pull request** (and pushes to
`main`): Spotless format check + `./mvnw verify` (build + Testcontainers tests).

## Documentation

Design docs (SRS, tech-stack decisions, research, diagrams) and the LaTeX report live under
[`docs/`](docs). The evaluation & performance harness has its own guide in [`perf/`](perf).
