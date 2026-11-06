# NexBusiness Cloud

An AI agent and RAG platform for small businesses, built with Java Spring Boot, React (Vite + Tailwind CSS), MySQL, and the Groq API (Llama-3).

## Modules

1. **SecureDoc RAG Engine:** project-based document namespaces, role-based access (PUBLIC vs ADMIN_ONLY), and page-level citations.
2. **OmniServe Action Agent:** tool-calling agent for POS, CRM and ERP tasks (for example stock checks), with a manager approval queue for sensitive actions.
3. **MemoryCore Portal:** long-term user fact extraction and switchable LLM models.

## Quick start

1. Copy the example environment file and add your own Groq key:

        cp .env.example .env

2. Start everything:

        docker compose up -d

3. Try the agent (the backend listens on port 8081):

        curl -s -X POST http://localhost:8081/api/omniserve/chat \
          -H "Content-Type: application/json" \
          -d '{"message":"How many Paper Cups do we have?"}'

## Demo data

Reset the demo inventory (Paper Cups drops to 3 units so the reorder flow can be shown):

    docker compose exec -T mysql-db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" nexbusiness_db' < scripts/demo-reset.sql

## Configuration

Secrets live in `.env`, which is never committed. See `.env.example` for the variables.

## Screenshots

![Agent steps](docs/screenshots/agent-steps.png)
![Approval queue](docs/screenshots/approvals.png)
![MemoryCore](docs/screenshots/memory.png)


### SecureDoc: same question, different answers by role

USER cannot see ADMIN_ONLY documents:

![USER view](docs/screenshots/docs-user.png)

ADMIN gets the answer with a page-level citation:

![ADMIN view](docs/screenshots/docs-admin.png)

Document list with delete (ADMIN only):

![Document list](docs/screenshots/docs-list.png)

## Authentication and roles

- Sign up and login with email and password (BCrypt-hashed). The API returns a JWT that the frontend sends on every request.
- Roles: `CUSTOMER` (default for every signup) and `ADMIN`. Signup can never create an admin.
- The first admin is created from environment variables on startup.
- Endpoint rules, enforced in Spring Security:
  - Approvals, document upload and document delete: `ADMIN` only
  - Chat, memory, document list and ask: any signed-in user
  - Everything else: denied
- Memory and the agent are scoped to the signed-in user (taken from the token, never from the URL).
- `ADMIN_ONLY` documents are filtered in the SQL query, so the model never sees text the user may not read.

### Environment variables

Copy `.env.example` to `.env` and set:

| Variable | Purpose |
|---|---|
| `GROQ_API_KEY` | Groq API key |
| `GROQ_MODEL` | Optional model override |
| `JWT_SECRET` | Random string, 32+ characters |
| `ADMIN_EMAIL` | Email of the seeded admin account |
| `ADMIN_PASSWORD` | Password for the seeded admin (8+ characters) |

## Known limitations

- Document search uses MySQL full-text search (keyword based). Vector embeddings are the planned upgrade.
- PDFs only, no OCR for scanned documents.
- JWT is stored in `localStorage`; a production deployment should use HTTPS and consider httpOnly cookies.
- Not deployed yet; runs locally with `docker compose up`.
