# IntelliStock Full Stack Application (Production Release)

IntelliStock is an automated, AI-driven stock research and fundamental analysis dashboard. 

## Architectural Layout
- **React Frontend (Nginx)**: Port `80` (production) or `5173` (development). Integrates Recharts area chart close price visualizations, watchlist favorites list, and responsive glassmorphic design.
- **Spring Boot Backend**: Port `8080`. Connects to Redis caching, H2 database storage, Gemini AI prompt summarizers, and multiple financial APIs with connection timeouts and fallbacks.
- **Redis Cache**: Port `6379`. Stores stock reports in JSON format with a 5-minute Time-to-Live (TTL). Falls back to memory-caching if offline.
- **Gemini AI**: Generates 3-4 sentence summaries under `gemini-1.5-flash`. Falls back to local rule-based analysis if unconfigured.

---

## Environment Variables Configuration
Create a `.env` file in the project root or configure these keys in your system environment:

| Environment Variable | Description | Requirement |
|:---|:---|:---|
| `GEMINI_API_KEY` | Google Gemini API Key for narrative summaries | Optional (Falls back to local rules) |
| `FINNHUB_API_KEY` | Finnhub API Key for quote prices | Optional (Falls back to Twelve/AV/H2) |
| `TWELVE_DATA_API_KEY` | Twelve Data API Key for quotes & histories | Optional (Falls back to AV/H2) |
| `ALPHA_VANTAGE_API_KEY` | Alpha Vantage API Key for SMA indicators | Optional (Falls back to H2) |
| `FMP_API_KEY` | Financial Modeling Prep API Key for metrics | Optional (Falls back to H2) |
| `NEWS_API_KEY` | NewsAPI Key for keyword sentiment analysis | Optional (Falls back to Neutral) |
| `CORS_ALLOWED_ORIGINS` | Configures allowed origins on backend controllers | Default: `http://localhost:5173` |

---

## Setup & Deployment Instructions

### Option 1: Docker Compose (Recommended for Production)
Ensure Docker and docker-compose are installed on your host, then run:

```bash
# 1. Set environment variables
export GEMINI_API_KEY="your_api_key_here"

# 2. Build and launch all services (Frontend, Backend, Redis)
docker-compose up --build -d
```
Open your browser at `http://localhost/` to access the application.

### Option 2: Local Development Setup

#### 1. Backend Server Setup
Navigate to the `intellistock-backend` directory:
```powershell
# Copy template and edit keys
copy .env.example .env

# Compile and run
.\mvnw spring-boot:run
```
*Port mapping:* Backend will listen on `http://localhost:8080/`.

#### 2. Frontend React Setup
Navigate to the `intellistock-frontend` directory:
```bash
# Copy template and install dependencies
copy .env.example .env
npm install

# Run Vite dev server
npm run dev
```
*Port mapping:* Frontend will listen on `http://localhost:5173/`.

---

## Running Test Suites

### Backend Unit & Integration Tests (MockMvc)
Runs 15 test cases verifying controller routes, time series arrays, watchlist database CRUD, and indicator logic:
```bash
cd intellistock-backend
./mvnw test
```

### Frontend Vitest Component Tests
Runs Vitest specifications checking text input sync, loader state spinners, and sidebar lists:
```bash
cd intellistock-frontend
npm run test
```

---

## Release Readiness Checklist
- [x] All 15 backend unit and integration tests compile and pass.
- [x] Frontend Vitest component tests pass.
- [x] Caching layer is resilient to Redis connection failures.
- [x] Historical stock price endpoint provides chronological series.
- [x] Startup validator warns about missing environmental API keys.
- [x] CORS allowed origins are dynamically configurable.
- [x] Dockerfiles and docker-compose configurations are validated.
