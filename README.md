# Doctor Recommendation System

Full-stack doctor recommendation system built to support patient-guided provider lookup from symptoms, location, insurance, language, and telehealth preferences.

The original `Example/` folder is preserved as-is. The runnable project lives in `backend/`, `frontend/`, and `infra/`.

## Architecture

- **Backend:** Spring Boot 3, Java 21, REST APIs, JPA/Hibernate, MySQL, Redis, OAuth2 resource-server support.
- **Frontend:** React, TypeScript, Vite, Google OAuth 2.0 sign-in, symptom intake, doctor browsing, and admin profile management.
- **Matching:** OpenAI-powered medical-term extraction when `OPENAI_API_KEY` is present; local medical dictionary fallback when it is not.
- **Ranking:** TF-IDF scoring plus specialty, distance, insurance, language, rating, telehealth, and availability signals.
- **Location:** Natural-language location search plus coordinate-based Google Places discovery when `GOOGLE_PLACES_API_KEY` is present; optional SerpAPI Google Maps fallback through `SERPAPI_API_KEY`; no bundled fake doctor records.
- **Validation:** Recommendation feedback capture plus offline matching evaluation and Redis provider-cache benchmark scripts.
- **Deployment:** Docker Compose locally, with sample AWS ECS Fargate and EKS manifests under `infra/`.

## Local Run

```bash
cp .env.example .env
docker compose up --build
```

Then open:

- Frontend: `http://localhost:5173`
- Backend health: `http://localhost:8080/api/health`

The app runs without external API keys, but live provider lookup requires `GOOGLE_PLACES_API_KEY` or `SERPAPI_API_KEY`. Without those keys, recommendations rank only doctors imported earlier or created through the admin API.

If Docker/MySQL/Redis are not installed, run the backend with the local H2 profile:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Then run the frontend separately:

```bash
cd frontend
npm install
npm run dev -- --host 127.0.0.1 --port 5173
```

## Environment Variables

Backend:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `RECOMMENDATION_CACHE_TTL_MINUTES`
- `PROVIDER_LOOKUP_CACHE_TTL_MINUTES`
- `SECURITY_ENABLED`
- `CORS_ALLOWED_ORIGINS`
- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `GOOGLE_PLACES_API_KEY`
- `SERPAPI_API_KEY`
- `LOCATION_SEARCH_CACHE_TTL_MINUTES`

Frontend:

- `VITE_API_BASE_URL`
- `VITE_GOOGLE_CLIENT_ID`

For Google OAuth, create a Google OAuth web client and set `VITE_GOOGLE_CLIENT_ID`. When `SECURITY_ENABLED=true`, the backend validates Google-issued JWTs through Spring Security's OAuth2 resource server configuration.

For live provider discovery, set `GOOGLE_PLACES_API_KEY` for Google Places API (New) Text Search. Requests are sent only when the recommendation payload includes `latitude` and `longitude`; otherwise the system ranks stored admin/imported doctors. Set `SERPAPI_API_KEY` if you want SerpAPI Google Maps search as a fallback when Google Places is not configured or returns no provider results. The frontend location box can resolve natural-language locations through `GET /api/locations/search`; it uses Google Places first, then SerpAPI Google Maps, and also accepts direct `latitude,longitude` input without calling an external API.

Redis is used at three levels: `RECOMMENDATION_CACHE_TTL_MINUTES` caches full recommendation responses for identical requests, `PROVIDER_LOOKUP_CACHE_TTL_MINUTES` caches Google Places/SerpAPI provider lookup results by provider, specialty, rounded coordinates, and radius, and `LOCATION_SEARCH_CACHE_TTL_MINUTES` caches location search results. Provider lookup caching avoids repeated external map searches even when the user changes filters such as insurance or language.

## API Surface

- `GET /api/health`
- `GET /api/doctors`
- `GET /api/doctors/{id}`
- `POST /api/admin/doctors`
- `GET /api/specialties`
- `GET /api/locations/search?query=Boston%20Children%27s%20Hospital`
- `POST /api/recommendations`
- `GET /api/recommendations/history`
- `POST /api/feedback`
- `GET /api/feedback/summary`

Example recommendation request:

```json
{
  "symptoms": "chest pain and shortness of breath",
  "latitude": 41.8781,
  "longitude": -87.6298,
  "radiusMiles": 20,
  "insurance": "Aetna",
  "language": "English",
  "telehealthPreferred": true
}
```

With live keys enabled, the backend searches near the submitted coordinates for the specialties inferred from symptoms, saves returned provider listings with `externalProvider`, `externalId`, and `externalUri`, then ranks them together with stored doctors. Repeated provider lookups are served from Redis when the provider, specialty, coordinates, and radius match. External listings should be treated as provider/place data, not verified clinical credential records.

## Evidence Tools

Run the offline matching evaluation without external API calls:

```bash
node tools/accuracy-evaluation.mjs
```

The output compares a first-keyword baseline with enhanced dictionary extraction plus TF-IDF-style specialty scoring on a small labeled engineering dataset. Treat it as regression evidence only; use a larger clinically reviewed dataset before publishing accuracy-lift claims such as `~20%`.

Run the Redis provider lookup benchmark:

```bash
ALLOW_EXTERNAL_LOOKUP=true node tools/provider-cache-benchmark.mjs
```

This sends two recommendation requests with the same symptoms, coordinates, and radius but different insurance filters. That avoids the full recommendation-response cache while allowing provider lookup cache reuse. On a cold Redis cache this can consume up to two external provider searches because the sample symptom text maps to two specialties. Keep this under control when using SerpAPI trial quota.

User validation feedback is stored through `POST /api/feedback` and summarized through `GET /api/feedback/summary`. Use those records, not hard-coded text, to support claims such as `validated by 100+ users`.

## Tests

Backend:

```bash
cd backend
mvn test
```

Frontend:

```bash
cd frontend
npm install
npm test
```

Integration tests are designed for Testcontainers and are skipped automatically when Docker is not available.

## AWS Deployment Notes

ECS:

1. Build and push backend/frontend images to ECR.
2. Replace placeholders in `infra/ecs/task-definition.json` and `infra/ecs/service.json`.
3. Store database and API secrets in SSM Parameter Store.
4. Store `GOOGLE_PLACES_API_KEY` and optional `SERPAPI_API_KEY` in SSM Parameter Store.
5. Point the backend to Amazon RDS MySQL and ElastiCache Redis.

EKS:

1. Create the namespace with `infra/eks/namespace.yaml`.
2. Copy `infra/eks/secrets.template.yaml`, replace secret values, and apply it.
3. Replace image, domain, RDS, and ElastiCache placeholders in the EKS manifests.
4. Apply backend, frontend, and ingress manifests.

## Metrics Claims

The code supports measuring repeat lookup latency through Redis cache hits and recommendation history. Provider lookup cache counters are exposed through Actuator metrics as `doctor.provider.lookup.cache.hits`, `doctor.provider.lookup.cache.misses`, `doctor.provider.lookup.cache.writes`, and `doctor.provider.lookup.cache.errors`. Do not hard-code resume metrics such as "100+ users", "20% accuracy increase", or "35% latency reduction"; validate them with user testing, matched-care review, and latency measurements before publishing.

See `RESUME_EVIDENCE.md` for a claim-by-claim checklist of completed, deploy-ready, and still-needs-real-data items.
