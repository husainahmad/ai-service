# Harmoni POS AI Service

Spring Boot 3.4.8 / Java 21 service hosting the AI ordering assistant, RAG and
recommendations. Extracted from the customer service in Phase 4.

Port: **8086**. Reached through the gateway at `/api/v1/ai/**` and through the
more specific `/api/v1/customer-sessions/*/chat` and `.../cart` routes.

## Architecture

Hexagonal (ports and adapters):

```
domain/         models (AiSession, ChatMessage, CartLine, Recommendation) + exceptions
application/    port.in use cases, port.out interfaces, services (no framework I/O)
adapter/in/     Vaadin-independent REST controllers
adapter/out/    customer/menu/order clients, LLM adapter, in-memory cart, AI @Tools
config/         composition root (ChatClient wiring, @ConfigurationProperties)
```

The application services depend only on `port.out` interfaces, so they are unit
tested with Mockito and no Spring context (`AiChatServiceTest`).

## API

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/customer-sessions/{id}/chat` | Blocking AI chat (aliases: `/messages/ai`) |
| `POST` | `/api/v1/customer-sessions/{id}/chat/stream` | SSE stream (aliases: `/messages/ai/stream`) |
| `POST` | `/api/v1/customer-sessions/{sessionId}/cart/items` | Add a cart line |
| `GET` | `/api/v1/customer-sessions/{sessionId}/cart` | View cart |
| `DELETE` | `/api/v1/customer-sessions/{sessionId}/cart/items/{productId}` | Remove a cart line |
| `GET` | `/api/v1/ai/recommendations/{customerId}` | Generate and persist a recommendation |

## Dependencies

- **Customer service (8084)**: session validation, message persistence, customer
  lookup, metrics lookup and insight persistence (`POST /internal/v1/insights`).
- **Menu service (8082)** and **Order service (8083)**: authoritative catalogue
  data and order creation through the `@Tool` methods.
- **LLM**: Kenari (`https://kenari.id/v1`, model `deepseek-v4-flash`) by default;
  set `KENARI_API_KEY` (or `AI_API_KEY`/`AI_BASE_URL`/`AI_MODEL`). Use
  `-Dspring-boot.run.profiles=local` for a local Ollama (`qwen2.5:3b`, RAG on).

## State

Chat memory and carts are in-memory and scoped to this process. They are lost on
restart — by design for this phase.

## Running

```bash
mvn spring-boot:run
mvn test
```
