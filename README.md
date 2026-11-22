# Harmoni POS AI Service

Spring Boot 3.4.8 / Java 21 service hosting the AI business-operations assistant
and recommendations. Extracted from the customer service in Phase 4.

Port: **8086**. Reached through the gateway at `/api/v1/ai/**` and through the
more specific `/api/v1/customer-sessions/*/chat` route.

## Architecture

Hexagonal (ports and adapters):

```
domain/         models (AiSession, ChatMessage, Recommendation) + exceptions
application/    port.in use cases, port.out interfaces, services (no framework I/O)
adapter/in/     Vaadin-independent REST controllers
adapter/out/    customer client, LLM adapter
config/         composition root (ChatClient wiring with MCP tools, @ConfigurationProperties)
```

The application services depend only on `port.out` interfaces, so they are unit
tested with Mockito and no Spring context (`AiChatServiceTest`).

## API

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/customer-sessions/{id}/chat` | Blocking AI chat (aliases: `/messages/ai`) |
| `POST` | `/api/v1/customer-sessions/{id}/chat/stream` | SSE stream (aliases: `/messages/ai/stream`) |
| `GET` | `/api/v1/ai/recommendations/{customerId}` | Generate and persist a recommendation |

## Dependencies

- **Customer service (8084)**: session validation, message persistence, customer
  lookup, metrics lookup and insight persistence (`POST /internal/v1/insights`).
- **MCP POS server via gateway (8080 → 8087)**: all catalog, order, promotion,
  customer and report operations over MCP SSE. The AI service does not call the
  menu or order services directly.
- **LLM**: Kenari (`https://kenari.id/v1`, model `deepseek-v4-flash`) by default;
  set `KENARI_API_KEY` (or `AI_API_KEY`/`AI_BASE_URL`/`AI_MODEL`). Use
  `-Dspring-boot.run.profiles=local` for a local Ollama (`qwen2.5:3b`).

## MCP client

The AI service connects to `harmoni.mcp.client.url` (default
`http://localhost:8080`, the gateway) at `/api/v1/mcp/sse`. Set
`MCP_POS_TOKEN` to a valid gateway JWT; the client sends it as
`Authorization: Bearer <token>` and the gateway injects the caller identity and
permissions before forwarding to the MCP server.

```bash
export MCP_POS_TOKEN='<access token from POST /api/v1/auth/login>'
```

Set `MCP_POS_ENABLED=false` to run without MCP. For a direct MCP connection,
set `MCP_POS_URL=http://localhost:8087` and leave `MCP_POS_TOKEN` empty; the
client then sends the configured `X-Username`, `X-Roles`, `X-Permissions` and
`X-Scopes` headers directly.

The configured identity includes `menu:edit` and `organization:read` for catalog
operations, plus `order:view`, `report:view`, `menu:view` and `customer:read`.
Do not grant delete, `rbac:manage` or other administrative permissions without
an explicit review.

The system prompt covers catalog writes, promotions, order calculation,
confirmation and payment. Inventory, order sessions, persistent promotion
application and close-day settlement are explicitly unsupported because the
downstream services do not expose them.

## State

Chat memory is in-memory and scoped to this process. It is lost on restart — by
design for this phase. Order and cart state is not held here; order workflows are
carried out through MCP tools.

## Running

Kenari is the default LLM provider. Export the key and start the service without
the `local` or `ollama-cloud` profile:

```bash
export KENARI_API_KEY='your-kenari-key'
mvn spring-boot:run
mvn test
```

Optional overrides: `AI_BASE_URL`, `AI_MODEL`, and `AI_API_KEY`. The default is
`https://kenari.id/v1` with `deepseek-v4-flash`.
