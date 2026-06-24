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
  links (doc ↔ code, requirement ↔ code, commit ↔ code), including cross-source links.
- **Hybrid retrieval** — semantic + keyword + graph traversal, fused into a single ranked result.
- **Sync & update** — incremental, content-hash based; evicts stale knowledge.
- **Access control** — authentication on every endpoint; per-source authorization (ACL).
- **Interfaces** — REST (query + admin) and an MCP server for agents.

## Architecture

- **Clean Architecture**, **packaged by feature** — each feature has `domain` (model + ports),
  `application` (services), and `infrastructure` (adapters) layers; dependencies point inward.
- **GraphRAG storage** — **Neo4j** holds both the graph and vectors by default; **Qdrant** can be
  added behind a `VectorStore` port when scaling (`APP_VECTORSTORE_MODE=neo4j+qdrant`).
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
| Graph + vector | Spring Data **Neo4j** |
| AI / RAG | **Spring AI 1.1** — embedding, Neo4j vector store, MCP server, PDF/Markdown/Tika readers |
| Validation / ops | Bean Validation, Spring Boot Actuator |
| Testing | JUnit 5, **Testcontainers** (real Neo4j) |
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
cp .env.example .env            # set NEO4J_PASSWORD and OPENAI_API_KEY

# 2a. run the whole stack (app + Neo4j) in containers
just up                         # = docker compose up -d --build

# 2b. or run just the app locally against a local/compose Neo4j
just dev                        # = ./mvnw spring-boot:run

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
├── docs/                                 # SRS, design notes, diagrams
├── Dockerfile                            # multi-stage build → runtime image
├── docker-compose.yml                    # app + Neo4j (+ Qdrant when scaling)
├── Makefile                              # dev task runner (npm-scripts style)
├── .githooks/pre-commit                  # auto-format on commit
└── .github/workflows/ci.yml              # CI: format check + build + tests
```

## Dev scripts

Run `just` to list tasks. Coming from npm, here is the mapping:

| npm | just | Runs |
|---|---|---|
| `npm run dev` | `just dev` | `./mvnw spring-boot:run` |
| `npm run build` | `just build` | `./mvnw -DskipTests package` |
| `npm test` | `just test` | `./mvnw test` (Testcontainers) |
| `npm run lint` | `just lint` | Spotless check + Checkstyle |
| `npm run format` | `just format` | `./mvnw spotless:apply` |
| — | `just verify` | format check + build + tests |
| — | `just deps` | print dependency tree |
| — | `just up` / `just down` | start / stop the docker-compose stack |
| — | `just logs` | tail app logs |
| — | `just hooks` | enable the pre-commit format hook |

> `.env` is auto-loaded by `just` (via `dotenv-load`), so tasks pick up `OPENAI_API_KEY` etc.

> Note: Maven is **declarative** — dependencies live in `pom.xml` (the `package.json` equivalent) and
> `./mvnw` resolves them on build. There is no `npm install <x>` CLI; add deps by editing `pom.xml`
> (or via Spring Initializr / your IDE).

## Configuration

Behaviour is driven by **Spring Profiles** + environment variables (one artifact, config injected at
runtime). Precedence: env var > `application-<profile>.yml` > `application.yml`.

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Active profile: `dev` / `prod` / `cloud` |
| `SERVER_PORT` | `8000` | HTTP port |
| `SPRING_NEO4J_URI` | `bolt://localhost:7687` | Neo4j connection |
| `NEO4J_USERNAME` / `NEO4J_PASSWORD` | `neo4j` / `knowledgehub` | Neo4j credentials |
| `OPENAI_API_KEY` | — | Embedding API key (required for ingestion/retrieval) |
| `EMBEDDING_MODEL` | `text-embedding-3-small` | Embedding model |
| `APP_VECTORSTORE_MODE` | `neo4j` | `neo4j` or `neo4j+qdrant` (added when scaling) |

Secrets (`OPENAI_API_KEY`, DB password) come from env / secret manager — **never commit them**
(`.env` is git-ignored; see `.env.example`).

## Testing

```bash
make test       # JUnit 5 + Testcontainers (spins a real Neo4j container; needs Docker)
```

The context test boots the full Spring context against a throwaway Neo4j container, so it exercises
the real wiring (vector store, MCP server, repositories).

## Deployment (single docker-compose, VM or local)

```bash
cp .env.example .env
just up          # docker compose up -d --build
```

- Runs **app + Neo4j**; data persists in **named volumes** (`neo4j-data`, `neo4j-logs`).
- Neo4j heap/pagecache and the app heap are **RAM-capped** in `docker-compose.yml` for local/small
  hosts — raise them on a real server.
- Self-host this compose on a VM (e.g. a small cloud VM) to avoid using laptop RAM; attach a block
  volume + backups if the VM can be recreated.
- Qdrant is included (commented) for scaling out.

## CI

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on **every pull request** (and pushes to
`main`): Spotless format check + `./mvnw verify` (build + Testcontainers tests).

## Documentation

Design docs (SRS, tech-stack decisions, research, diagrams) live under [`docs/`](docs).
