# GateKeeper — API Rate Limiter & Gateway

[![CI](https://github.com/raigaurav26/GateKeeper/actions/workflows/ci.yml/badge.svg)](https://github.com/raigaurav26/GateKeeper/actions/workflows/ci.yml)

A production-grade API rate limiting gateway built with **Spring Boot 3, Redis, JWT, and MySQL**. Manages API keys, enforces rate limits using the Token Bucket algorithm, logs every request, and provides usage analytics.

## Architecture

```
┌──────────────┐      ┌───────────────────────────────────────────┐
│  Client App  │─────▶│              GateKeeper                   │
│              │      │                                           │
│  X-API-Key   │      │  ┌─────────┐  ┌──────────┐  ┌─────────┐ │
│  in header   │      │  │  Rate   │  │  API Key │  │ Request │ │
│              │      │  │  Limit  │  │  Validate│  │   Log   │ │
└──────────────┘      │  │  Filter │  │          │  │         │ │
                      │  └────┬────┘  └────┬─────┘  └────┬────┘ │
                      │       │            │              │      │
                      │  ┌────▼────┐  ┌────▼─────┐  ┌────▼────┐ │
                      │  │  Redis  │  │  MySQL   │  │  MySQL  │ │
                      │  │ Counter │  │  Keys DB │  │ Logs DB │ │
                      │  └─────────┘  └──────────┘  └─────────┘ │
                      └───────────────────────────────────────────┘
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Framework | Spring Boot 3.2 |
| Rate Limiting | Redis + Lua (Token Bucket Algorithm) |
| Database | MySQL 8 |
| Authentication | Spring Security + JWT |
| Language | Java 17 |
| Build | Maven |

## Features

- **API Key Management** — Generate, revoke, and manage keys with tier-based limits
- **Token Bucket Rate Limiting** — atomic Redis Lua script, smooth refill, configurable limits per key
- **Rate Limit Headers** — `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`
- **Request Logging** — Every API call logged with method, path, status, response time, IP
- **Usage Analytics** — Top endpoints, hourly volume, success rates, per-key stats
- **JWT Admin Auth** — Secure management endpoints with token-based authentication
- **429 Too Many Requests** — Proper HTTP response with `Retry-After` header

## Rate Limit Tiers

| Tier | Requests/Hour | Use Case |
|------|--------------|----------|
| FREE | 100 | Development & testing |
| PRO | 1,000 | Small applications |
| ENTERPRISE | 10,000 | Production workloads |

## How to Run

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8
- Redis

### Setup

```bash
# 1. Create database
mysql -u root -proot -e "CREATE DATABASE gatekeeper;"

# 2. Start Redis
redis-server

# 3. Set the JWT signing secret (at least 32 characters)
export JWT_SECRET='replace-with-a-long-random-string-123'

# 4. Run the application
cd GateKeeper
mvn spring-boot:run
```

If `JWT_SECRET` is not set, a random key is generated at startup, so admin tokens stop working after a restart.

App runs on `http://localhost:9090`

## API Reference

### Auth
```
POST /api/auth/login
Body: {"username": "admin", "password": "admin123"}
Returns: JWT token
```

### API Key Management (requires JWT)
```
POST   /api/keys              — Generate new API key
GET    /api/keys              — List all keys
GET    /api/keys/{id}         — Get key details
PUT    /api/keys/{id}         — Update key tier
DELETE /api/keys/{id}         — Revoke key
```

### Analytics (requires JWT)
```
GET /api/analytics/summary?hours=24       — Overall stats
GET /api/analytics/top-endpoints?hours=24 — Most used endpoints
GET /api/analytics/requests?hours=24      — Hourly request volume
GET /api/analytics/key/{keyId}?hours=24   — Per-key usage
```

### Gateway (requires X-API-Key header)
```
ANY /gateway/{service}/** — Forward request through rate limiter
GET /gateway/health       — Health check
```

## Usage Example

```bash
# 1. Login as admin
curl -X POST http://localhost:9090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 2. Create an API key (use JWT from step 1)
curl -X POST http://localhost:9090/api/keys \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"my-app","owner":"gaurav","tier":"FREE"}'

# 3. Use the API key to make requests
curl http://localhost:9090/gateway/users/list \
  -H "X-API-Key: gk_<your_key>"

# Response headers will include:
# X-RateLimit-Limit: 100
# X-RateLimit-Remaining: 99
# X-RateLimit-Reset: 3580

# 4. Exceed rate limit → get 429
# After 100 requests in an hour:
# HTTP 429 Too Many Requests
# {"error":"Rate limit exceeded. Upgrade your plan or wait."}

# 5. Check analytics
curl http://localhost:9090/api/analytics/summary \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

## Token Bucket Algorithm

Each API key gets a bucket in Redis that holds up to its tier limit in tokens and refills continuously over one hour (a FREE key gets one token back every 36 seconds, not 100 all at once on the hour).

1. A request runs a Lua script on the key's bucket (`scripts/token_bucket.lua`)
2. The script tops the bucket up for the time since the last request, capped at the limit
3. If at least one token is left → take it and allow the request
4. Otherwise → reject with 429 and a `Retry-After` of the seconds until the next token
5. Idle buckets expire on their own after an hour, which is when they would be full again

The read, refill and update happen inside one script, so Redis runs them atomically: concurrent requests on the same key can never spend more tokens than the bucket holds. The bucket level is kept in integer units (1/3,600,000 of a token per ms), so refill has no floating point drift.

```
Request → EVAL token_bucket.lua → token available? → take 1, allow
                                        │
                                        └─ empty → 429 + Retry-After
```

## Running Tests

```bash
docker run -d -p 6379:6379 redis:7-alpine   # rate limiter tests run against real Redis
mvn verify
```

## Author

**Gaurav Rai** — [GitHub](https://github.com/raigaurav26)
