# AgriBid Nexus

### A reverse-auction procurement engine for agricultural commodities, with Gemini doing real work at three specific points in the pipeline — not bolted on as a chatbot.

---

## Table of Contents

1. [The Problem](#the-problem)
2. [What This Actually Is](#what-this-actually-is)
3. [Key Features](#key-features)
4. [System Architecture](#system-architecture)
5. [Database Schema](#database-schema)
6. [Security Model](#security-model)
7. [The AI Pipeline in Detail](#the-ai-pipeline-in-detail)
8. [Tech Stack](#tech-stack)
9. [Project Structure](#project-structure)
10. [Getting Started](#getting-started)
11. [Environment Variables](#environment-variables)
12. [API Reference](#api-reference)
13. [Testing](#testing)
14. [Known Limitations](#known-limitations)
15. [Roadmap](#roadmap)
16. [License](#license)

---

## The Problem

Across most of India's agricultural markets, a farmer doesn't sell into a market — they sell into a fog. They bring their harvest to the mandi with no real visibility into what a fair price actually is, and the commission agent standing between them and the buyer knows exactly how much of that fog they can profit from. This isn't a rare inefficiency. It's one of the most persistent, well-documented drivers of rural poverty in agrarian economies, not just in India but across South Asia, Sub-Saharan Africa, and Latin America.

Most attempts to fix this build a marketplace. A storefront. A listing page with a "contact seller" button. That doesn't fix price discovery — it just moves the same broken negotiation onto a screen.

## What This Actually Is

AgriBid Nexus is not a marketplace. It's a live, competitive reverse-auction engine. A farmer lists a harvest lot. Verified distributors bid on it, openly, against each other, in real time. The highest bid converts into a locked forward contract with a real delivery deadline, which then gets fulfilled through tranche-based deliveries against a real order record. Every price is visible. Every step is auditable.

And baked directly into that pipeline — not bolted on as a chatbot widget — Gemini does three specific jobs:

- **Grades crops from a photo.** A farmer uploads an image, Gemini's vision model assesses quality, shelf life, and pest markers, and that assessment becomes a real, persisted database record — never just chat text that disappears.
- **Suggests a fair reserve price, grounded in real data.** Before a farmer sets their asking price, a RAG pipeline retrieves actual historical mandi bulletins and MSP circulars from a Qdrant vector store and generates a recommendation from *that*, not from whatever Gemini happened to memorize during training.
- **Acts as a negotiation co-pilot for distributors** — one that calls real backend logic (tax calculators, live pricing lookups, warehouse capacity checks) mid-conversation instead of making numbers up, and can reach into an externally-hosted MCP tool server for weather-risk data the same way it calls its own internal tools.

## Key Features

**Marketplace mechanics**
- Farmer-listed crop lots with category taxonomy, quantity tracking, and a lifecycle state machine (`DRAFT to GRADED to LISTED to SOLD/EXPIRED`)
- Live reverse-auction bidding with a real minimum-tick-size validator
- Automatic auction closing via a scheduled job, not manual intervention
- Atomic, database-enforced conversion of a winning bid into a locked forward contract — structurally impossible for one auction to spawn two competing contracts
- Tranche-based order fulfillment, reflecting that bulk agricultural delivery almost never happens in one shipment

**AI capabilities**
- Multimodal crop grading via Gemini vision, with structured output mapped directly into persisted entities
- RAG-grounded reserve price recommendations backed by a real document ingestion pipeline
- A tool-calling negotiation assistant with persistent, per-auction chat memory
- An MCP server exposing internal data (warehouse capacity, contract terms) to external agents
- An MCP client consuming an external weather-risk service through the same interface as internal tools

**Engineering rigor**
- Optimistic locking (`@Version`) on live auction listings, verified with an actual concurrency test firing simultaneous bids at one listing
- Keyset (cursor-based) pagination specifically on the one endpoint where offset pagination would actually degrade — the live bid stream — while everything else uses standard, simpler offset pagination
- JWT-based authentication with a custom `AuthorizationManager` enforcing KYC verification at the Spring Security filter-chain layer, before any controller code runs
- Method-security ownership checks (`@PreAuthorize` + a custom `SecurityEvaluator`) on every farmer-owned-resource endpoint
- RFC-7807 `ProblemDetail` error responses across the entire API, so every failure mode is typed and machine-parseable, not a raw stack trace or an inconsistent ad-hoc error shape

## System Architecture

The backend is organized by responsibility, not by feature — each package has exactly one job, and dependencies flow in one direction.

```
controller/     -> thin REST layer, binds HTTP to services, enforces @PreAuthorize
service/        -> business logic, transaction boundaries, zero AI dependency
service/impl/   -> the actual implementations
domain/         -> JPA entities, grouped by bounded context (user, crop, auction, contract, logistics)
repository/     -> Spring Data JPA interfaces + Specification-based dynamic search
dto/            -> request/response records, kept separate from entities
security/       -> JWT filter, token provider, custom KYC authorization manager, ownership evaluator
validation/     -> custom Bean Validation constraints (e.g. minimum bid increment)
exception/      -> typed exceptions + a single centralized handler
util/           -> cross-cutting helpers (file storage)
config/         -> Spring configuration beans (security, AI, Swagger, scheduling)
scheduler/      -> the automatic auction-close job
ai/
  vision/       -> Gemini crop-grading pipeline
  pricing/      -> RAG-grounded reserve price advisor
  negotiation/  -> memory-backed negotiation co-pilot
  rag/          -> document ingestion into Qdrant
  tools/        -> internal @Tool beans Gemini can invoke
  mcp/server/   -> MCP tools/resources exposed to external agents
  mcp/client/   -> configuration for consuming an external MCP server
```

**Why the AI package is isolated the way it is:** `CropLotServiceImpl` and every other business service has zero dependency on `ChatClient` or anything Gemini-related. The only place a `ChatClient` is ever injected is inside `ai/`. Controllers are the seam — they call a plain business service for things like ownership checks, and separately call into `ai/` for the actual model invocation. This keeps the core domain logic testable and swappable independent of whatever AI provider is in use.

## Database Schema

Managed entirely through Flyway migrations, applied automatically on startup — no manual schema setup required.

| Migration | Creates |
|---|---|
| `V1` | `users` (joined-inheritance base table), `farmer_profiles`, `distributor_profiles`, `agronomist_profiles` |
| `V2` | `categories`, `quality_grades`, `pest_tags`, `crop_lots`, `crop_lot_pest_tag` (join table) |
| `V3` | `bid_listings` (with the `@Version` optimistic-lock column), `bids` |
| `V4` | `forward_contracts`, `orders`, `order_fulfillments` |
| `V5` | `warehouses`, `msp_rates`, plus seed reference data (crop categories, pest tags, sample MSP rates, sample warehouses) |
| `V6` | `admin_profiles` |

**A deliberate schema decision worth calling out:** `User` uses `InheritanceType.JOINED` rather than `SINGLE_TABLE`. It's the less lazy choice — it keeps each role's schema normalized rather than forcing one sparse `users` table full of nullable, role-specific columns — but it means every role, including `ADMIN`, needs its own concrete subclass and its own (sometimes minimal) table for Hibernate to be able to instantiate a row at all.

## Security Model

**Roles:** `FARMER`, `DISTRIBUTOR`, `AGRONOMIST`, `ADMIN`.

**Authentication flow:** login issues a JWT (HS512, via jjwt) carrying the user's ID, email, role, and `kycVerified` status as claims. Every subsequent request passes through `JwtAuthFilter`, which validates the token and — critically — reloads the user fresh from the database rather than trusting the token's embedded claims for authorization decisions. This means a KYC status change takes effect on a user's very next request, not only after their token expires.

**KYC gating:** distributor bidding is gated by a custom `AuthorizationManager` operating inside the Spring Security filter chain, matched specifically against the bidding endpoint's URI pattern. An unverified distributor's bid request is rejected before it ever reaches a controller — the fraud/impersonation risk is denied at the network boundary, not caught reactively in a service method.

**Ownership enforcement:** farmer-owned resources (crop lots, listings) are protected by `@PreAuthorize` expressions referencing a custom `SecurityEvaluator` bean, checked at the method-security layer before the method body ever executes.

**Admin provisioning is intentionally not self-service.** `ADMIN` and `AGRONOMIST` roles cannot register through the public `/auth/register` endpoint — allowing that would mean anyone reaching the API could grant themselves administrative access. Admin accounts are provisioned directly, deliberately, out-of-band.

## The AI Pipeline in Detail

### Crop Grading
A farmer uploads a photo. `CropGradingService` sends it to Gemini's vision-capable chat model alongside an externalized prompt template, and the response is coerced via `.entity(CropGradeAssessment.class)` directly into a strongly-typed record — never stored or displayed as raw model text. That record populates a persisted `QualityGrade` row and normalizes detected pest labels against a `PestTag` reference table (by code, to avoid near-duplicate tags from minor wording variation across grading calls).

### Reserve Price Advisor (RAG)
Historical mandi bulletins and MSP circulars, ingested as PDFs through an admin-only endpoint, get chunked and embedded into a Qdrant vector store. When a farmer requests a reserve-price suggestion, a `QuestionAnswerAdvisor` retrieves the top-k semantically similar chunks *before* Gemini generates a single word of recommendation — the model is synthesizing from retrieved, citable context, not from parametric memory.

### Negotiation Co-Pilot
A distributor can chat with a co-pilot scoped to a specific listing, backed by persistent, JDBC-stored chat memory. The co-pilot has four internal `@Tool` beans (mandi tax calculation, live MSP lookup, warehouse capacity check, nearest-fulfillment-center matching) plus whatever tools a connected external MCP server exposes — Gemini invokes all of them through the identical interface, unable to distinguish an internal Java method call from a tool running on a completely separate service.

### MCP — Both Directions
The backend exposes its own warehouse-inventory and contract-terms data as MCP tools/resources via Spring AI's declarative `@McpTool`/`@McpResource` annotations, so an external logistics partner's own AI agent can query it using the standard protocol instead of a bespoke API integration. Separately, it's configured as an MCP *client* consuming an external weather-risk service's tools, folding them into the same negotiation `ChatClient`.

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Web | Spring MVC (`spring-boot-starter-webmvc`) |
| Persistence | Spring Data JPA, Hibernate ORM 7.4.1 |
| Database | H2 2.4.240 (file-based, swappable to Postgres) |
| Migrations | Flyway 12.4.0 |
| Security | Spring Security 7.1.0, jjwt 0.13.0, BCrypt |
| AI Framework | Spring AI 2.0.0 |
| LLM | Google Gemini (chat, vision, and embeddings via Google GenAI) |
| Vector Store | Qdrant |
| Protocol Federation | Model Context Protocol SDK 2.0.0 |
| API Docs | springdoc-openapi 3.0.3 (Swagger UI) |
| Boilerplate Reduction | Lombok |

## Project Structure

```
agribid-nexus/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/agribid/nexus/
│   │   │   ├── AgriBidNexusApplication.java
│   │   │   ├── ai/
│   │   │   │   ├── vision/            (Gemini crop grading)
│   │   │   │   ├── pricing/           (RAG reserve price advisor)
│   │   │   │   ├── negotiation/       (co-pilot chat)
│   │   │   │   ├── rag/               (document ingestion)
│   │   │   │   ├── tools/             (internal @Tool beans)
│   │   │   │   └── mcp/
│   │   │   │       ├── server/        (exposed MCP tools/resources)
│   │   │   │       └── client/        (external MCP consumption notes)
│   │   │   ├── config/                (Security, Spring AI, Swagger, Scheduling, Qdrant, JWT properties)
│   │   │   ├── controller/
│   │   │   ├── domain/
│   │   │   │   ├── user/
│   │   │   │   ├── crop/
│   │   │   │   ├── auction/
│   │   │   │   ├── contract/
│   │   │   │   └── logistics/
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   ├── response/
│   │   │   │   └── mapper/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   │   └── specification/
│   │   │   ├── scheduler/
│   │   │   ├── security/
│   │   │   │   └── evaluator/
│   │   │   ├── service/
│   │   │   │   └── impl/
│   │   │   ├── util/
│   │   │   └── validation/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       ├── db/migration/          (V1 through V6)
│   │       └── prompts/               (externalized .st prompt templates)
│   └── test/
│       ├── java/com/agribid/nexus/service/
│       └── resources/application.properties
```

## Getting Started

**Prerequisites:**
- Java 21+
- Maven
- Docker (for Qdrant)
- A Google Gemini API key — get one at [aistudio.google.com](https://aistudio.google.com/apikey)

**1. Start Qdrant** (once — reuse the same container afterward so ingested data persists):
```bash
docker run -d -p 6333:6333 -p 6334:6334 --name agribid-qdrant qdrant/qdrant
```
On every subsequent run, just: `docker start agribid-qdrant`

**2. Set your Gemini API key** as an environment variable, or in your IDE's run configuration:
```bash
export GEMINI_API_KEY=your-key-here
```

**3. Run the application:**
```bash
mvn spring-boot:run
```

H2 runs embedded — no separate database server to install or configure. Flyway applies every migration automatically on first boot, including seed reference data (crop categories, pest tags, sample MSP rates, sample warehouses), so there's real data to test against immediately.

**Swagger UI:** `http://localhost:8080/swagger-ui.html`
**H2 Console** (requires `--spring.profiles.active=dev`): `http://localhost:8080/h2-console`

## Environment Variables

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `GEMINI_API_KEY` | **Yes** | none | Google Gemini API access for chat, vision, and embeddings |
| `JWT_SECRET` | No | a local-dev-only fallback (baked into `application.properties`) | HMAC signing secret for JWTs — must be at least 64 characters if overridden |
| `DB_PASSWORD` | No | blank | H2 database password |

## API Reference

The full API surface is documented in two companion artifacts included alongside this repo:
- A ready-to-import **Postman collection** covering all 20 endpoints, with auth tokens and IDs auto-captured between requests via test scripts
- A printable **PDF reference** with sample request/response bodies for every endpoint

Quick summary of the surface:

| Resource | Endpoints |
|---|---|
| Auth | register, login |
| Crop Lots | create, attach image, grade (Gemini), reserve-price-suggestion (RAG), get by ID, get mine (paginated) |
| Bid Listings | publish, search (filtered + paginated), get by ID, convert to contract |
| Bids | place bid, stream bids (keyset-paginated) |
| Forward Contracts | get, create order |
| Order Fulfillments | record tranche, mark delivered, get fulfillments (paginated) |
| Negotiation | send message (AI, tool-calling) |
| RAG Admin | ingest market document (PDF) |

## Testing

One concurrency test exists today: `BidServiceConcurrencyTest` fires 20 simultaneous bids at a single listing and asserts that every bid reported as successful actually has a corresponding persisted row, and that the listing's final `currentHighestBid` matches the true maximum among successful bids — not just that the code executes without throwing. It runs against H2 directly rather than a Postgres Testcontainer; that's a deliberate tradeoff documented in the test's own comments, since the two databases' locking implementations aren't identical even though both correctly implement JPA's `@Version` semantics.

Everything else in the request/response flow has been verified manually end-to-end through Postman rather than covered by automated integration tests yet.

## Known Limitations

- No OCR step in the RAG ingestion pipeline — a scanned/image-based PDF with no real text layer will fail to ingest, since only digitally-typeset PDFs have extractable text
- File-based H2 doesn't support multiple application instances writing concurrently — fine for a single-instance deployment, not for horizontal scaling
- No frontend yet — every capability described above is verified through the API directly
- Automated integration test coverage beyond the one concurrency test is still to be built

## Roadmap

- Automated integration tests across the full auction-to-contract-to-fulfillment lifecycle
- A proper admin-provisioning flow that doesn't require direct database access
- OCR support for scanned document ingestion
- A frontend

## License

This project is available under the MIT License.

---

Built as a real attempt at what "AI-powered marketplace" should mean when taken seriously — arguing over optimistic locking semantics and JWT claim design, not just shipping a demo that looks good on stage.