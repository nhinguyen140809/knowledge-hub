# Usage Guide

How to run Knowledge Hub and operate it for two audiences:

- **Admin** — runs the hub, registers sources, and hands out access.
- **Developer** — consumes the hub from an AI agent (e.g. Claude Code) over MCP.

Everything except health and API docs is a bearer-token API: each request carries
`Authorization: Bearer <secret>`. The MCP server and the REST admin API share that same
security chain.

---

## 1. Running the hub on GitHub Codespaces

A local machine with little RAM cannot run the full stack (the app, Neo4j and Qdrant need
several GB together). Codespaces gives at least an 8 GB machine and Docker out of the box, so
it is the recommended way to run it. The repo ships a ready
[devcontainer](../../.devcontainer/devcontainer.json).

### 1.1 Secrets (set once, per user)

In **GitHub → Settings → Codespaces → Secrets**, add these and allow the repository to read
them. They are injected as environment variables and written into `.env` on first boot by
[post-create.sh](../../.devcontainer/post-create.sh); you never copy `.env` by hand.

| Secret | Purpose |
| --- | --- |
| `NEO4J_PASSWORD` | Neo4j password (any value; must match across restarts). |
| `EMBEDDING_API_KEY` | Embedding provider key (OpenAI, Voyage, …). Set `EMBEDDING_BASE_URL` / `EMBEDDING_MODEL` / `EMBEDDING_DIMENSION` too if not using the OpenAI defaults. |
| `API_KEY` | Bootstrap admin secret. On first startup it seeds the admin principal, and its value is the admin's bearer token (see §3.1). |

### 1.2 Start the Codespace

1. On GitHub: **Code → Codespaces → New with options**.
2. Pick the branch and a **4-core / 16 GB** machine (the smallest 2-core / 8 GB works but is
   tight while Maven builds and all three containers run).
3. Wait for the container to build; `post-create.sh` installs `just`, writes `.env`, and warms
   the Maven cache.

### 1.3 Start the stack

```bash
just up      # build + run app + Neo4j + Qdrant (docker compose)
just logs    # tail the app logs
just ps      # container status
just down    # stop everything
```

The **Ports** tab forwards:

| Port | Service |
| --- | --- |
| 8000 | app — REST API + MCP endpoint (`/mcp`) |
| 7474 | Neo4j browser |
| 6333 | Qdrant dashboard |

From inside the Codespace terminal the app is `http://localhost:8000`. To reach it from your
own browser, use the forwarded URL shown in the Ports tab.

> Running locally instead? The same commands work anywhere with Docker and JDK 21; copy
> `.env.example` to `.env`, fill the keys, then `just up`. See
> [GETTING-STARTED.md](../development/GETTING-STARTED.md).

---

## 2. Admin guide

The admin registers sources, onboards developers, and controls who can read what. All admin
routes live under `/api/v1/admin/**`, require the `ADMIN` role, and take the admin bearer token.

> **Two ways to run these calls — pick either.**
> - **Swagger UI (interactive, easiest).** Open **`/docs`**, click **Authorize**, paste the admin
>   bearer token (§2.1), and use *Try it out* on the **admin** group. Best for one-off operations;
>   the form shows every field and response. On Codespaces `/docs` is the forwarded **port-8000**
>   URL from the Ports tab (private — only you as the codespace owner); locally it is
>   `http://localhost:8000/docs`.
> - **`curl` (scriptable).** The snippets below issue the exact same requests and document the
>   request bodies, so they double as a field reference and as automation.

### 2.1 Bootstrap the admin

Set `API_KEY` before the first startup. On boot, if no admin exists yet, the app creates the
`bootstrap-admin` principal and stores the **hash** of `API_KEY` as its credential. This is
idempotent — a restart never re-seeds. From then on, **the value of `API_KEY` is the admin's
bearer token**. Leaving `API_KEY` blank disables seeding (no admin, no way in).

```bash
export BASE=http://localhost:8000/api/v1
export ADMIN="Authorization: Bearer $API_KEY"   # the value you set as the API_KEY secret
```

### 2.2 Onboard a developer (3 steps)

**Step 1 — create a principal for the developer** (`type`: `SUBJECT` | `GROUP`,
`role`: `ADMIN` | `MEMBER`):

```bash
curl -s -X POST "$BASE/admin/principals" -H "$ADMIN" -H 'Content-Type: application/json' \
  -d '{"principalId":"dev-alice","type":"SUBJECT","role":"MEMBER"}'
```

**Step 2 — issue a credential.** The plaintext secret is returned **once** and only stored
hashed; copy it now and give it to the developer over a secure channel.

```bash
curl -s -X POST "$BASE/admin/principals/dev-alice/credentials" -H "$ADMIN"
# => {"credentialId":"<uuid>","secret":"<256-bit url-safe base64>"}   <-- shown once
```

**Step 3 — grant read access** to the sources the developer may see (by source id):

```bash
curl -s -X POST "$BASE/admin/grants" -H "$ADMIN" -H 'Content-Type: application/json' \
  -d '{"principalId":"dev-alice","sourceIds":["repo-payments","docs-handbook"]}'
```

The developer now authenticates with the `secret` from step 2 and can query only the granted
sources.

### 2.3 Groups (optional)

Create a `GROUP` principal, grant it access once, then add developers as members — they inherit
the group's grants:

```bash
curl -s -X POST "$BASE/admin/principals" -H "$ADMIN" -H 'Content-Type: application/json' \
  -d '{"principalId":"team-payments","type":"GROUP","role":"MEMBER"}'
curl -s -X POST "$BASE/admin/principals/team-payments/members" -H "$ADMIN" \
  -H 'Content-Type: application/json' -d '{"memberId":"dev-alice"}'
```

### 2.4 Inspect and revoke

```bash
# what can a principal actually read?
curl -s "$BASE/admin/principals/dev-alice/effective-permissions" -H "$ADMIN"

# revoke read access to some sources
curl -s -X POST "$BASE/admin/grants/revoke" -H "$ADMIN" -H 'Content-Type: application/json' \
  -d '{"principalId":"dev-alice","sourceIds":["repo-payments"]}'

# revoke a credential (the next request using it fails immediately)
curl -s -X DELETE "$BASE/admin/credentials/<credentialId>" -H "$ADMIN"
```

Full request/response shapes are in the Swagger UI at `http://localhost:8000/docs`.

---

## 3. Developer guide (using the hub through an agent)

You receive two things from your admin:

1. A **secret** (your bearer token).
2. The **MCP URL** — `http://localhost:8000/mcp` when the app runs in your own Codespace, or the
   forwarded/hosted URL your team publishes.

### 3.1 Connect Claude Code to the MCP server

The app exposes its tools as an MCP server over Streamable HTTP at `/mcp`. Register it, passing
your secret as the bearer header:

```bash
claude mcp add --transport http knowledge-hub \
  http://localhost:8000/mcp \
  --header "Authorization: Bearer <your-secret>"

claude mcp list        # verify it connects
```

Prefer a project file? Add `.mcp.json` at the repo root and keep the secret in an environment
variable so it is never committed:

```json
{
  "mcpServers": {
    "knowledge-hub": {
      "type": "http",
      "url": "http://localhost:8000/mcp",
      "headers": { "Authorization": "Bearer ${KH_TOKEN}" }
    }
  }
}
```

Then `export KH_TOKEN=<your-secret>` before starting the agent.

### 3.2 Use it

Once connected, the agent can call the hub's tools to query the code and documentation
knowledge graph. You only ever see sources your principal was granted; anything else is
invisible. If a request returns `401`, your credential is missing or revoked; a `403` means you
lack a grant for that source — ask your admin.

---

## 4. Credentials & security notes

- **Bearer everywhere.** Health (`/actuator/health`) and docs (`/docs`, `/v3/api-docs`) are
  public; every other route, REST and `/mcp` alike, needs a valid token.
- **Secrets are shown once** and stored only as SHA-256 hashes in Neo4j. If lost, issue a new
  credential and revoke the old one — they cannot be recovered.
- **Least privilege.** A developer's principal should be `MEMBER` and hold only the grants it
  needs. Reserve `ADMIN` for operators.
- **Never commit secrets.** `.env` and `*.env` are gitignored; keep tokens in Codespaces
  secrets or local env vars, not in `.mcp.json` literals.
- **Rotation.** Revoke and re-issue credentials to rotate; revoked credentials are purged after
  the configured retention window.

---

## Reference — key endpoints

| Method & path (`/api/v1` prefix) | Role | Purpose |
| --- | --- | --- |
| `POST /admin/principals` | ADMIN | Create a principal (subject or group) |
| `POST /admin/principals/{id}/credentials` | ADMIN | Issue a credential (secret returned once) |
| `GET /admin/principals/{id}/effective-permissions` | ADMIN | Resolve what a principal can read |
| `POST /admin/principals/{id}/members` | ADMIN | Add a member to a group |
| `POST /admin/grants` | ADMIN | Grant read access to sources |
| `POST /admin/grants/revoke` | ADMIN | Revoke read access to sources |
| `DELETE /admin/credentials/{id}` | ADMIN | Revoke a credential |
| `POST /mcp` (Streamable HTTP) | any authenticated | MCP tools for AI agents |
| `GET /docs` | public | Swagger UI (full API reference) |
