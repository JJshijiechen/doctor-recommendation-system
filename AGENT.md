# AGENT.md

This file gives coding agents project-specific guidance for the Doctor Recommendation System.

## Project Shape

- This is a full-stack doctor recommendation app.
- The runnable application lives in `backend/`, `frontend/`, and `infra/`.
- `Example/` is preserved source material. Do not modify it unless the task explicitly asks for changes there.
- This repository is not currently initialized as a Git repository in this workspace.

## Architecture

- Backend: Spring Boot 3.3, Java 21, Maven, REST APIs, Spring Data JPA, MySQL, Redis, OAuth2 resource-server support.
- Frontend: React 18, TypeScript, Vite, Vitest, Testing Library, Google OAuth sign-in.
- Matching flow:
  - `POST /api/recommendations` accepts symptoms, optional coordinates, radius, insurance, language, and telehealth preference.
  - `MedicalTermExtractionService` uses OpenAI when `OPENAI_API_KEY` is set.
  - If OpenAI is unavailable or fails, local dictionary extraction is the intended fallback.
  - `DoctorDiscoveryService` can import live provider/place listings from Google Places or SerpAPI when API keys and coordinates are present.
  - `TfIdfRankingService` ranks stored/imported doctors using terms, specialties, distance, insurance, language, rating, telehealth, and availability signals.
- Frontend API calls are centralized in `frontend/src/lib/api.ts`; shared response/request shapes are in `frontend/src/types/api.ts`.

## Directories To Avoid

Do not inspect or edit generated/vendor output unless directly necessary:

- `frontend/node_modules/`
- `frontend/dist/`
- `backend/target/`
- `.DS_Store`
- local `.env` files

Use `rg --files -g '!frontend/node_modules/**' -g '!backend/target/**' -g '!frontend/dist/**'` when mapping the codebase.

## Environment

- Start from `.env.example` for local configuration.
- Never commit real API keys, OAuth client IDs, database passwords, or provider credentials.
- External provider discovery requires `GOOGLE_PLACES_API_KEY` or `SERPAPI_API_KEY`.
- OpenAI extraction requires `OPENAI_API_KEY`; without it, preserve the local fallback behavior.
- Google OAuth requires `VITE_GOOGLE_CLIENT_ID`; backend JWT validation is controlled by `SECURITY_ENABLED`.

## Common Commands

Full stack with Docker:

```bash
cp .env.example .env
docker compose up --build
```

Backend with local H2 profile:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Backend tests:

```bash
cd backend
mvn test
```

Backend integration verification, including Failsafe/Testcontainers when Docker is available:

```bash
cd backend
mvn verify
```

Frontend setup and development:

```bash
cd frontend
npm install
npm run dev -- --host 127.0.0.1 --port 5173
```

Frontend checks:

```bash
cd frontend
npm test
npm run build
```

Health check after startup:

```bash
curl http://localhost:8080/api/health
```

## Backend Conventions

- Keep controller classes thin. Put business logic in `service/`.
- Keep persistence through Spring Data repositories in `repository/`.
- DTO records in `dto/` define API contracts. Update frontend `types/api.ts` when response/request shapes change.
- Domain entities live in `domain/`; avoid leaking JPA entities directly to frontend responses.
- Preserve transactional boundaries on recommendation generation and history reads.
- Keep external API integrations failure-tolerant. Google Places, SerpAPI, and OpenAI failures should not make the app unusable when a local fallback exists.
- The app is medical-adjacent, not diagnostic software. Keep disclaimers and avoid language that claims diagnosis, verified credentials, clinical accuracy, or guaranteed care quality.
- Treat Google Places and SerpAPI records as provider/place listings, not verified medical credential records.

## Frontend Conventions

- `frontend/src/App.tsx` currently owns the main patient matching, directory, and admin workflows.
- Keep API access in `frontend/src/lib/api.ts`; do not scatter raw `fetch` calls across components.
- Use `frontend/src/types/api.ts` as the TypeScript source of truth for backend contracts.
- Existing UI uses compact operational screens, lucide-react icons, and plain CSS in `frontend/src/styles.css`.
- Preserve accessible labels for form controls and buttons; tests rely on user-facing roles and labels.
- When adding location or recommendation UI, maintain both typed city selection and browser geolocation behavior.

## Testing Guidance

- For backend service logic, add focused JUnit tests under `backend/src/test/java/com/doctorrecommendation/service/`.
- For REST behavior, use MockMvc tests under `backend/src/test/java/com/doctorrecommendation/controller/`.
- For frontend behavior, use Vitest and Testing Library in `frontend/src/*.test.tsx`.
- If a change touches both backend DTOs and frontend types, run both backend and frontend checks.
- Testcontainers-based integration tests may skip automatically when Docker is unavailable; mention that if relevant in final notes.

## API Surface

Current public endpoints:

- `GET /api/health`
- `GET /api/doctors`
- `GET /api/doctors/{id}`
- `POST /api/admin/doctors`
- `GET /api/specialties`
- `POST /api/recommendations`
- `GET /api/recommendations/history`

When changing these endpoints, update:

- backend controller and DTO tests
- frontend API wrapper
- frontend TypeScript types
- README API documentation

## Deployment Notes

- `docker-compose.yml` is the local orchestration path for MySQL, Redis, backend, and frontend.
- `infra/ecs/` and `infra/eks/` are deployment templates with placeholders. Do not assume they are production-ready without replacing image, domain, database, cache, and secret values.
- Keep secrets in external secret stores for cloud deployments. Do not inline real secrets in `infra/`.

## Agent Working Rules

- Prefer small, scoped changes aligned with the existing structure.
- Read the relevant source and tests before editing.
- Do not rewrite unrelated files or generated output.
- Use structured parsers and framework APIs instead of ad hoc string handling where possible.
- After code changes, run the narrowest meaningful checks first, then broader checks when the touched surface is shared.
- If changing healthcare-facing copy, avoid overclaiming. Use cautious, patient-supportive language and preserve emergency-care guidance for urgent symptoms.
