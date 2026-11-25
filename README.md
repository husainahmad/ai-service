# Harmoni POS AI Service

Spring Boot 3.4.8 / Java 21 service hosting the per-page dashboard AI assistant.
Extracted from the customer service in Phase 4.

Port: **8086**. Reached through the gateway at `/api/v1/ai/**`.

## Architecture

Hexagonal (ports and adapters):

```
application/    port.in use cases, port.out interfaces, services (no framework I/O)
adapter/in/     REST controllers
adapter/out/    LLM adapter
config/         composition root (ChatClient wiring, @ConfigurationProperties)
```

The application services depend only on `port.out` interfaces, so they are unit
tested with Mockito and no Spring context (`DashboardSummaryServiceTest`).

## API

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/ai/dashboard/summary-chat` | Answer a question using the supplied page context |

## Dependencies

- **MCP POS server via gateway (8080 → 8087)**: all catalog, order, promotion,
  customer and report operations over MCP SSE. The AI service does not call the
  menu or order services directly.
- **LLM**: Kenari (`https://kenari.id/v1`, model `grok-4-5`) by default;
  set `KENARI_API_KEY` (or `AI_API_KEY`/`AI_BASE_URL`/`AI_MODEL`). Use
  `-Dspring-boot.run.profiles=local` for a local Ollama (`qwen2.5:3b`).

## MCP client

The AI service connects directly to `harmoni.mcp.client.url` (default
`http://localhost:8087`) at `/api/v1/mcp/sse`. Without a token it sends the
configured `X-Username`, `X-Roles`, `X-Permissions` and `X-Scopes` headers, so
no gateway login is needed for local development.

To route through the gateway instead, set `MCP_POS_URL=http://localhost:8080`
and `MCP_POS_TOKEN` to a valid gateway JWT; the client then sends
`Authorization: Bearer <token>` and the gateway injects the caller identity and
permissions before forwarding to the MCP server.

```bash
export MCP_POS_URL=http://localhost:8080
export MCP_POS_TOKEN='<access token from POST /api/v1/auth/login>'
```

Set `MCP_POS_ENABLED=false` to run without MCP. If the MCP server cannot be
reached, ai-service still starts and reports a warning; POS tools are then
unavailable for that run.

The configured identity includes `menu:edit` and `organization:read` for catalog
operations, plus `order:view`, `report:view`, `menu:view` and `customer:read`.
Do not grant delete, `rbac:manage` or other administrative permissions without
an explicit review.

The page prompt restricts answers to the supplied page context and never invents
business data or exposes customer personal data.

## State

The page assistant is stateless: it receives page context with every request and
keeps no conversation memory. Order and cart state is not held here; order
workflows are carried out through MCP tools.

## Running

Kenari is the default LLM provider. Export the key and start the service without
the `local` or `ollama-cloud` profile:

```bash
export KENARI_API_KEY='your-kenari-key'
mvn spring-boot:run
mvn test
```

Optional overrides: `AI_BASE_URL`, `AI_MODEL`, and `AI_API_KEY`. The default is
`https://kenari.id/v1` with `grok-4-5`.
