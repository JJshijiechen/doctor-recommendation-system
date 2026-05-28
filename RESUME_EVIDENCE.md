# Resume Evidence Checklist

This file maps each resume claim to concrete project evidence. It is intentionally conservative: do not present unverifiable claims as completed work.

## Claim Status

| Resume claim | Status | Evidence |
| --- | --- | --- |
| Developed layered Spring Boot services | Complete | `backend/src/main/java/com/doctorrecommendation/controller`, `service`, `repository`, `domain` |
| Used JPA/Hibernate ORM | Complete | JPA entities in `domain`, repositories in `repository`, `spring-boot-starter-data-jpa` in `backend/pom.xml` |
| Built REST APIs for symptom matching | Complete | `RecommendationController`, `RecommendationService`, `POST /api/recommendations` |
| Built REST APIs for doctor profile management | Complete | `DoctorController`, `DoctorService`, `GET /api/doctors`, `GET /api/doctors/{id}`, `POST /api/admin/doctors` |
| Containerized with Docker | Complete | `backend/Dockerfile`, `frontend/Dockerfile`, `docker-compose.yml` |
| Deployed to AWS ECS/EKS | Deploy-ready only | ECS examples in `infra/ecs`; EKS manifests in `infra/eks`; no real AWS deployment output is stored in this repo |
| Validated by 100+ users | Needs real data | `POST /api/feedback` and `GET /api/feedback/summary` now capture validation data; do not claim 100+ until real records exist |
| Built patient-friendly React frontend | Complete | `frontend/src/App.tsx`, `frontend/src/styles.css`, `frontend/src/App.test.tsx` |
| Added OAuth 2.0 authentication | Configured | Google OAuth frontend wrapper in `frontend/src/main.tsx`; backend JWT resource server in `SecurityConfig`; production requires `VITE_GOOGLE_CLIENT_ID` and `SECURITY_ENABLED=true` |
| Used OpenAI APIs for medical-term extraction | Complete with fallback | `MedicalTermExtractionService` calls OpenAI Responses API when `OPENAI_API_KEY` is set; `LocalMedicalTermExtractor` handles no-key fallback |
| Used TF-IDF ranking | Complete | `TfIdfRankingService` and `TfIdfRankingServiceTest` |
| Resulted in ~20% accuracy increase | Needs evaluated evidence | Run `node tools/accuracy-evaluation.mjs`; use a larger reviewed dataset before publishing this claim |
| Integrated Google Places API | Complete/configured | `DoctorDiscoveryService` calls Google Places when `GOOGLE_PLACES_API_KEY` is set |
| Supports location-based recommendations | Complete | Frontend city/geolocation inputs plus backend coordinate search and distance ranking |
| Maintained persistent MySQL database on Amazon RDS | Deploy-ready only | MySQL config exists in `application.yml`, `docker-compose.yml`, and RDS placeholders in `infra`; real RDS endpoint evidence is not in repo |
| Added Redis caching for repeated provider lookups | Complete and locally verified | `ProviderLookupCacheService`, provider cache metrics, `tools/provider-cache-benchmark.mjs` |
| Reduced latency in repeated provider lookups | Locally benchmarked | Use `ALLOW_EXTERNAL_LOOKUP=true node tools/provider-cache-benchmark.mjs`; keep benchmark output as evidence for the actual environment |

## Verification Commands

Backend tests:

```bash
cd backend
mvn test
```

Frontend tests and build:

```bash
cd frontend
npm test
npm run build
```

Offline accuracy evaluation:

```bash
node tools/accuracy-evaluation.mjs
```

Redis provider lookup benchmark:

```bash
ALLOW_EXTERNAL_LOOKUP=true node tools/provider-cache-benchmark.mjs
```

## Safe Resume Wording

Use this wording before real AWS deployment, user validation, and clinical-review evaluation are available:

> Built a full-stack Doctor Recommendation System with layered Spring Boot services, JPA/Hibernate ORM, RESTful APIs, React frontend, Google OAuth support, OpenAI-backed medical-term extraction with local fallback, TF-IDF ranking, Google Places/SerpAPI location-based provider discovery, MySQL persistence, Redis provider lookup caching, Docker Compose, and deploy-ready AWS ECS/EKS manifests.

After real validation is collected, replace deploy-ready language only with measured facts, such as actual ECS/EKS deployment URL, feedback count, benchmark output, and reviewed accuracy results.
