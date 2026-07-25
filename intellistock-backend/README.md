# IntelliStock Backend (Phase 2 - Real API Integrations)

This is the backend repository for the IntelliStock stock analysis engine, featuring concurrent and resilient API integrations to five external stock and news services.

## Tech Stack
- **Language/Platform**: Java 17, Spring Boot 3.3.2, Maven
- **Database**: H2 (In-Memory Database)
- **Key Features**: In-memory Concurrent Caching, REST Client connection timeouts, resiliance fallbacks.

## Directory Structure
- `com.intellistock.controller`: REST controller exposing endpoint services.
- `com.intellistock.service`: Core service orchestrator applying metrics algorithms.
- `com.intellistock.service.client`: Modular API Clients connecting to external providers.
- `com.intellistock.repository`: Integrates database operations.
- `com.intellistock.model`: Holds the `Stock` JPA entity.
- `com.intellistock.dto`: Unified DTOs (`AnalyzeRequest`, `AnalyzeResponse`, and `AnalysisData`).
- `com.intellistock.config`: Houses CORS configurations, database seed, cache, and RestTemplate bean setups.
- `com.intellistock.exception`: Handles global exceptions.

## Core Endpoints
1. **Health Check**: `GET /api/health`
2. **Analyze Stock**: `POST /api/analyze`
   - Input: `{ "symbol": "INFY" }`
   - Returns a combined analysis payload. If any provider fails, it triggers the fallback logic and returns partial reports with zero API failures leaking to the client.

## Caching Layer
Repeated requests for the same stock symbol within **5 minutes** are served directly from our custom in-memory `AnalysisCache` utilizing a thread-safe `ConcurrentHashMap`. Check the console logs for `Cache hit for symbol: ...` or `Cache miss`.

---

## API Keys & Environment Variables

All API keys are configured using environment variable placeholders. Set these variables before starting the backend:

| Environment Variable | API Provider | Purpose |
| :--- | :--- | :--- |
| `FINNHUB_API_KEY` | Finnhub | Primary real-time stock quotes |
| `TWELVE_DATA_API_KEY` | Twelve Data | Backup real-time stock quotes |
| `ALPHA_VANTAGE_API_KEY` | Alpha Vantage | Tertiary quotes & Daily SMA indicator |
| `FMP_API_KEY` | Financial Modeling Prep | Company Profile & Key Ratios (P/E, ROE, Debt) |
| `NEWS_API_KEY` | NewsAPI | Recent news headlines |

### Fallback Behavior
If an API key is missing or calls fail due to limits/timeouts:
- Prices will fail over sequentially: `Finnhub` -> `Twelve Data` -> `Alpha Vantage` -> `Local Database` -> `Fallback Engine`.
- Metrics will fall back to local seed data or dynamic mock computations.
- The `confidenceScore` is reduced (starting from 100%, subtracting points for each failed real-time provider).

---

## How to Run Locally

### Prerequisites
- Java 17 installed and configured in your PATH environment variable.

### 1. Set Environment Variables

**Windows (PowerShell)**:
```powershell
$env:FINNHUB_API_KEY="your_finnhub_key"
$env:TWELVE_DATA_API_KEY="your_twelve_data_key"
$env:ALPHA_VANTAGE_API_KEY="your_alpha_vantage_key"
$env:FMP_API_KEY="your_fmp_key"
$env:NEWS_API_KEY="your_news_key"
```

**Windows (CMD)**:
```cmd
set FINNHUB_API_KEY=your_finnhub_key
set TWELVE_DATA_API_KEY=your_twelve_data_key
set ALPHA_VANTAGE_API_KEY=your_alpha_vantage_key
set FMP_API_KEY=your_fmp_key
set NEWS_API_KEY=your_news_key
```

**Linux / macOS**:
```bash
export FINNHUB_API_KEY="your_finnhub_key"
export TWELVE_DATA_API_KEY="your_twelve_data_key"
export ALPHA_VANTAGE_API_KEY="your_alpha_vantage_key"
export FMP_API_KEY="your_fmp_key"
export NEWS_API_KEY="your_news_key"
```

*Note: You can leave any or all keys empty to test the graceful fallback system!*

### 2. Launch Server
In the `intellistock-backend` directory, run:
```powershell
.\mvnw spring-boot:run
```
The server will run on `http://localhost:8080`.
