# Diagrams

Annotated guide to every Knowledge Hub diagram. All diagrams are authored as **D2** source
(`d2/*.d2`) and rendered to images — regenerate e.g. `d2 d2/ERD.d2 erd.svg` (likewise for the
others, or `*.pdf` for LaTeX). Each source embeds `vars.d2-config` (layout engine + pad) so the D2
playground and CLI apply the same render settings.

Grounded in the SRS ([../notes/NOTE.md](../notes/NOTE.md)) and the chosen stack
([../design/TECH-STACK.md](../design/TECH-STACK.md)): Spring Boot + Spring AI + Spring Data Neo4j, an
external embedding API, and **Neo4j** (graph + keyword index) + **Qdrant** (vectors) — both always,
every scale. Code structure: [../development/ARCHITECTURE.md](../development/ARCHITECTURE.md).

---

## 1. Data model (ERD)

*Source: [d2/ERD.d2](d2/ERD.d2)*

Neo4j holds the knowledge graph + keyword index **and** the ACL; vectors live in Qdrant, linked back
by `chunk_id`. `Source` is the **bridge** — both the provenance root of all knowledge and the unit of
authorization in the ACL.

**Relationship categories**
- **Structural** — solid: CONTAINS, DECLARES, CALLS, IMPORTS, EXTENDS, IMPLEMENTS, OVERRIDES.
- **Deeper structural (optional)** — INSTANTIATES, READS/WRITES, REFERENCES, HAS_TYPE,
  ANNOTATED_WITH, THROWS, TESTS (between `CodeEntity` nodes).
- **Cross-artifact** — dashed: DESCRIBES, IMPLEMENTED_BY, VERIFIED_BY, MODIFIES, CONSUMES,
  LINKS_TO. Heuristic links carry a **confidence** score; cross-source links allowed.

**Hierarchy levels** are one `CodeEntity` table with a `level` enum + self `CONTAINS`, from fine
(constant/field) to coarse (project).

**`Source` is the bridge** — orange in both worlds: provenance root (knowledge) and authorization
unit (ACL). The hard ACL filter pushes the allowed `source_id` set into every retrieval path.

---

## 2. Ingestion & indexing pipeline (per data type)

![Ingestion & indexing pipeline](pipeline-ingestion.svg)

*Source: [d2/PIPELINE-INGESTION.d2](d2/PIPELINE-INGESTION.d2)*

**Notes**

- **Per-type branches.** Code is parsed into an AST and chunked by *function/class* while its
  structural relations are extracted; documents (Markdown / PDF) are chunked by
  *document structure* and SRS requirements are pulled out as entities; git history becomes *commit*
  nodes with `MODIFIES` edges. Each unit carries provenance downstream.
- **Shared from stage 3 on.** All text chunks reuse one embedding model — switching models
  means a full re-index. The **content-hash cache** skips re-embedding unchanged chunks (cost control).
- **Two stores, always** (no toggle): vectors live in **Qdrant**, the graph + keyword index in
  **Neo4j**, behind the `VectorStore` port. Storage **dual-writes** each chunk and they are
  linked by a shared `chunk_id`. The trade-off vs an all-in-Neo4j design — no single-Cypher
  "vector search → graph traverse", a standing dual-write/eviction sync concern — is accepted at every
  scale (see TECH-STACK §3/§6).
- **Graph + vectors are linked by `chunk_id`**: a chunk is an embedded vector (Qdrant) *and* a graph
  entity (Neo4j), so retrieval fuses a semantic hit (Qdrant) with keyword/graph hits (Neo4j) via RRF and
  expands into related code/docs.

---

## 3. Incremental sync & eviction (insert / update / delete)

Triggered on demand via REST or an MCP tool; idempotent and incremental.

![Incremental sync & eviction](pipeline-sync.svg)

*Source: [d2/PIPELINE-SYNC.d2](d2/PIPELINE-SYNC.d2)*

---

## 4. Retrieval pipeline (where processed data is served)

The query side that consumes the indexed knowledge — hybrid search with a hard ACL filter.

![Retrieval pipeline](pipeline-retrieval.svg)

*Source: [d2/PIPELINE-RETRIEVAL.d2](d2/PIPELINE-RETRIEVAL.d2)*

---

## 5. Logical architecture (code structure)

*Source: [d2/LOGICAL-ARCHITECTURE.d2](d2/LOGICAL-ARCHITECTURE.d2)*

Clean Architecture + pipes-&-filters for the `knowledge` context (write side): `domain` /
`application` / `infrastructure`, dependencies pointing inward, infrastructure realizing domain ports.
The application layer is a pipeline of single-responsibility stages over a shared context. The
`retrieval` context mirrors this as a parallel (scatter-gather) pipeline — see §4.

---

## 6. Deployment

*Source: [d2/DEPLOYMENT-ARCHITECTURE.d2](d2/DEPLOYMENT-ARCHITECTURE.d2)*

Physical view: the Spring Boot app, Neo4j, and Qdrant run inside a private network; the embedding API
is an external service; clients (AI agent, admin) reach the app over HTTPS with a credential token.

---

## Shape legend (data-flow diagrams §2–4)

| Shape / edge | Meaning |
|---|---|
| Cylinder | Datastore (source, vector store, graph) |
| Oval | External service (embedding API) |
| Diamond | Decision / routing gate |
| Rectangle | Processing step |
| Solid arrow | Primary data flow |
| Dashed arrow | Discarded / filtered-out path |
| Edge label | Data type or branch condition |

> All data-flow diagrams are **logical** (what happens to data), independent of physical deployment
> (§6) and code structure (§5).
