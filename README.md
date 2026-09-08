# Cyber Threat Intelligence Dashboard

A production-grade, full-stack Cyber Threat Intelligence (CTI) platform built with **Spring Boot 3.3 (Java 21)**, **PostgreSQL**, and **React (Vite)**. The dashboard provides security operations teams and threat researchers with real-time indicators of compromise (IOCs), normalized threat records, 24-hour activity trends, source distribution analysis, multi-criteria filtering, and deep-dive threat investigation linking back to original intelligence feeds.

---

## Visual & Functional Highlights

- **Visual Fidelity**: Recreates a modern SOC interface matching the reference threat intelligence dashboard with dark header and navigation, responsive cards, and clean visual hierarchy.
- **Real Database-Backed Telemetry**: Operates on live PostgreSQL data with 70+ seeded realistic threat records covering Malicious IPs, Domains, Phishing URLs, Malware Hashes, and Fraud reports.
- **Dynamic Aggregates**: Dashboard summary counts, 24-hour time series activity, source donut charts, and severity breakdowns are calculated directly in PostgreSQL.
- **Granular Investigation Flow**:
  $$\text{Dashboard} \longrightarrow \text{Latest Threats Table} \longrightarrow \text{Threat Details Modal} \longrightarrow \text{Original Source URL}$$
- **Duplicate Prevention**: Composite uniqueness constraint on `(indicator, indicator_type, source)` with upsert normalization logic.
- **Extensible Provider Design**: Clean `ThreatIntelligenceProvider` interface enabling zero-downtime integration with live external threat feeds.

---

## Architecture Overview

```text
                  ┌──────────────────────────┐
                  │ External Threat Feeds    │
                  │ (PhishTank, URLhaus,     │
                  │  OpenPhish, I4C / NCRP)  │
                  └────────────┬─────────────┘
                               │
                               ▼
                  ┌──────────────────────────┐
                  │ Spring Boot 3.3 Backend  │
                  │ (Java 21 / Maven)        │
                  │                          │
                  │ Controller Layer         │
                  │      ↓                   │
                  │ Service Layer            │
                  │      ↓                   │
                  │ Repository Layer (JPA)   │
                  └────────────┬─────────────┘
                               │
                               ▼
                  ┌──────────────────────────┐
                  │ PostgreSQL Database      │
                  │ `cyber_threat_dashboard` │
                  │                          │
                  │ threats table with       │
                  │ composite uniqueness     │
                  └────────────┬─────────────┘
                               │ REST API (JSON)
                               ▼
                  ┌──────────────────────────┐
                  │ React 18 + Vite Frontend │
                  │                          │
                  │ • SOC-Style Dashboard    │
                  │ • Recharts Visualizations│
                  │ • Paginated /threats     │
                  │ • Threat Details Modal   │
                  └────────────┬─────────────┘
                               │ [ View Original Source ]
                               ▼
                  ┌──────────────────────────┐
                  │ External Threat Portal   │
                  └──────────────────────────┘
```

---

## Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.3.3, Spring Web, Spring Data JPA, Hibernate, PostgreSQL Driver, Bean Validation, Spring Boot Actuator, Lombok |
| **Frontend** | React 18, Vite 5, React Router 6, Recharts 2, Lucide React, Axios, Vanilla CSS Design System |
| **Database** | PostgreSQL 16/18, pgAdmin-compatible, indexed relational schema |
| **Build Tools** | Apache Maven 3.8+, Node.js 18+, npm 9+ |

---

## Project Structure

```text
Desktop/
└── cyber-threat-dashboard/
    ├── backend/
    │   ├── pom.xml
    │   └── src/
    │       ├── main/
    │       │   ├── java/com/cyberthreat/dashboard/
    │       │   │   ├── CyberThreatDashboardApplication.java
    │       │   │   ├── config/          # CorsConfig, WebMvc
    │       │   │   ├── controller/      # DashboardController, ThreatController
    │       │   │   ├── dto/
    │       │   │   │   ├── request/     # ThreatCreateRequest, ThreatFilterRequest
    │       │   │   │   └── response/    # Summary, Activity, Sources, ThreatResponse, Page
    │       │   │   ├── entity/          # ThreatEntity
    │       │   │   ├── enums/           # IndicatorType, ThreatType, Severity, ThreatStatus
    │       │   │   ├── exception/       # GlobalExceptionHandler, ResourceNotFoundException
    │       │   │   ├── mapper/          # ThreatMapper
    │       │   │   ├── repository/      # ThreatRepository, ThreatSpecification
    │       │   │   └── service/
    │       │   │       ├── ThreatService, DashboardService, ThreatIntelligenceProvider
    │       │   │       └── impl/        # Implementations & DataSeederService
    │       │   └── resources/
    │       │       └── application.properties
    │       └── test/
    │           └── java/com/cyberthreat/dashboard/
    │
    ├── frontend/
    │   ├── package.json
    │   ├── vite.config.js
    │   ├── index.html
    │   └── src/
    │       ├── assets/
    │       ├── components/
    │       │   ├── common/              # Navbar, Sidebar, StatCard, Badges, Spinner
    │       │   ├── dashboard/           # ActivityChart, SourceDonutChart, QuickFilters, RecentlyViewed, AboutCard
    │       │   └── threats/             # ThreatTable, ThreatDetailsModal
    │       ├── layouts/                 # MainLayout
    │       ├── pages/                   # DashboardPage, AllThreatsPage, Critical, High, ThreatDetail, Analytics, Sources, About
    │       ├── services/                # api.js, threatService.js, dashboardService.js
    │       ├── styles/                  # index.css (complete SOC design system)
    │       ├── App.jsx
    │       └── main.jsx
    │
    ├── README.md
    └── .gitignore
```

---

## Database Configuration

The backend is configured to connect to PostgreSQL. You can customize the connection using environment variables:

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `cyber_threat_dashboard` | Database name |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `root` | Database password |

To create the database manually in PostgreSQL / pgAdmin if needed:
```sql
CREATE DATABASE cyber_threat_dashboard;
```

---

## Getting Started & Running Locally

### 1. Start the Backend

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```
The backend will launch on **http://localhost:8080**.
On initial startup, `DataSeederService` will automatically verify the database and seed **70+ realistic threat records** into PostgreSQL.

To test the backend health:
```bash
curl http://localhost:8080/actuator/health
```

### 2. Start the Frontend

In a separate terminal:
```bash
cd frontend
npm install
npm run dev
```
The frontend dev server will launch on **http://localhost:5173**. Open your browser to explore the dashboard.

---

## REST API Endpoints

### Dashboard Telemetry
- `GET /api/dashboard/summary` — Returns total threats, severity breakdown, category counts, and daily increments.
- `GET /api/dashboard/activity` — Returns 24-hour time series activity distribution (Phishing, Malware, Fraud).
- `GET /api/dashboard/by-source` — Returns distribution breakdown by intelligence source (PhishTank, URLhaus, OpenPhish, I4C / NCRP, ThreatFox, AlienVault).
- `GET /api/dashboard/by-severity` — Returns distribution grouped by severity (CRITICAL, HIGH, MEDIUM, LOW).
- `GET /api/dashboard/by-type` — Returns distribution grouped by threat classification.

### Threat Management & Search
- `GET /api/threats/latest?limit=10` — Returns latest threat records.
- `GET /api/threats?page=0&size=15&sortBy=lastSeen&direction=desc` — Paginated and filtered threats.
  - Supports filters: `keyword`, `severity`, `threatType`, `indicatorType`, `status`, `country`, `source`.
- `GET /api/threats/search?keyword=malware` — Keyword search across indicators, descriptions, and tags.
- `GET /api/threats/critical` — Returns only CRITICAL severity threats.
- `GET /api/threats/high` — Returns only HIGH severity threats.
- `GET /api/threats/{id}` — Full threat record details.
- `POST /api/threats` — Ingest a new threat indicator (JSON body).
- `POST /api/threats/ingest/mock` — Trigger batch ingestion poll from provider feeds.
- `DELETE /api/threats/{id}` — Delete a threat record.

---

## Connecting External Threat Feeds

The backend uses a modular provider pattern:
1. Implement the `ThreatIntelligenceProvider` interface:
   ```java
   public interface ThreatIntelligenceProvider {
       String getProviderName();
       List<ThreatCreateRequest> fetchThreats();
   }
   ```
2. Annotate your implementation with `@Component`.
3. Ingesting feeds via `threatService.ingestFromProvider(provider)` automatically ensures idempotency: existing indicators are updated with latest timestamps and confidence scores rather than duplicated.

---

## Analytics & Telemetry (Google Analytics 4)

The frontend integrates production-grade Google Analytics 4 (GA4) with SPA route tracking, user interaction telemetry, and strict PII safeguards.

- **Testing & Verification Guide**: See [`docs/GA4_TESTING_GUIDE.md`](file:///home/akashsoni/Desktop/cyber-threat-dashboard/docs/GA4_TESTING_GUIDE.md) for full testing instructions using Browser Console, Network Tab, GA4 Realtime, and GA4 DebugView.
- **Environment Configuration**: Configure `VITE_GA4_MEASUREMENT_ID=G-XXXXXXXXXX` in `frontend/.env`.

