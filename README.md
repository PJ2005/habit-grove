# Habit Grove

A personal habit tracker that grows a tree. Positive habits make the tree grow and stay healthy. Negative habits strip its leaves, and a tree left neglected for a week dies.

Built as a DevOps assessment project: a Java 25 / Spring Boot backend, tested with Maven, packaged with Docker, deployed to Kubernetes (minikube) with PostgreSQL, and delivered through a Jenkins pipeline.

## Features

- Create habits with a type (positive or negative), a weight (1 to 5), and a daily target
- Log habits for today or a past date
- Daily score (0 to 100) and rolling score (exponentially smoothed) for each day
- Tree health from the rolling score: THRIVING, STABLE, WILTING, DYING, or DEAD
- Growth points that never decrease, so a large but neglected tree looks big and bare
- Death after 7 consecutive days with a rolling score below 10
- One living tree per user. The history of dead trees is kept as a memorial list
- Friends: send and accept requests, then share a tree with chosen friends
- Friends see health and history. Habit names are hidden unless the owner opts in

## Scoring rules

| Rule | Value |
|---|---|
| Points per log | habit weight x quantity |
| Negative points | counted double in the daily score |
| Positive cap per habit per day | dailyTarget x weight |
| Daily score | 100 x (positive - 2 x negative) / 10, clamped to 0 to 100 |
| Rolling score | 0.3 x today + 0.7 x yesterday's rolling |
| Health | THRIVING 70+, STABLE 40 to 69, WILTING 20 to 39, DYING 10 to 19, DEAD below 10 for 7 days |

The rules live in `com.example.grove.domain.ScoringRules` and are unit-tested without Spring.

## Architecture

```
Browser / any client
        |
        v   HTTP (JSON)
Spring Boot API  (com.example.grove.web)
        |
        v
Services         (service: LogService, SnapshotService, SharingService)
        |
        v
Domain rules     (domain: ScoringRules, TreeHealth, GrowthStage)  - no Spring
        |
        v
JPA repositories -> PostgreSQL (Kubernetes) / H2 in memory (tests and local runs)
```

Each log saves a `HabitLog`, then `SnapshotService` rebuilds that day's totals and every later day's rolling score. Health and growth are derived from these snapshots, not stored as a counter, so backdated logs are handled correctly.

## Project layout

```
habit-grove/
├── pom.xml
├── Dockerfile              multi-stage: Maven build + tests, then JRE runtime
├── .dockerignore
├── Jenkinsfile             pipeline definition
├── k8s/
│   ├── deployment.yaml     app Deployment (IMAGE_PLACEHOLDER filled by Jenkins)
│   ├── service.yaml        NodePort 30080
│   └── postgres/           secret, PVC, Deployment, Service for the database
├── scripts/
│   └── demo.sh             creates sample data through the API
└── src/
    ├── main/java/com/example/grove/
    │   ├── domain/         scoring rules (pure Java)
    │   ├── model/          JPA entities
    │   ├── repository/     Spring Data repositories
    │   ├── service/        business logic
    │   └── web/            REST controllers
    ├── main/resources/application.properties
    └── test/java/com/example/grove/
        ├── domain/         unit tests for the rules
        ├── service/        integration tests against the database
        └── api/            HTTP tests against a running server
```

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/users` | Create a user (`name`, `email`, `targetProfile`) |
| POST | `/users/{userId}/habits` | Create a habit |
| GET | `/users/{userId}/habits` | List a user's habits |
| POST | `/users/{userId}/tree` | Create the living tree (409 if one exists) |
| GET | `/users/{userId}/tree` | Current tree with today's and rolling scores |
| GET | `/users/{userId}/tree/history` | Daily snapshots from first log onward |
| GET | `/users/{userId}/tree/all` | Living and dead trees (memorial list) |
| POST | `/users/{userId}/logs` | Log a habit (`habitId`, `quantity`, optional `occurredOn`) |
| POST | `/users/{userId}/friends/{friendId}` | Send a friend request |
| POST | `/users/{userId}/friend-requests/{requesterId}/accept` | Accept a request |
| PUT | `/users/{ownerId}/tree/shares/{viewerId}` | Share tree with a friend (`showHabitNames`) |
| GET | `/users/{viewerId}/friends/{ownerId}/tree` | Friend's read-only view |
| GET | `/actuator/health` | Health check (used by Kubernetes probes) |

Errors use standard HTTP codes: 404 for missing users, habits, or trees; 403 for logging another user's habit or viewing without access; 409 for duplicates.

## Run locally

Requirements: Java 25, Maven 3.9+.

```bash
mvn clean test          # runs all tests
mvn spring-boot:run     # starts on http://localhost:8080
```

Locally the app uses an in-memory H2 database, so data resets on restart. Try the API with:

```bash
./scripts/demo.sh
```

## Run with Docker

```bash
docker build -t habit-grove:local .
docker run --rm -p 8080:8080 habit-grove:local
```

The image runs the Maven tests during the build, so a failing test stops it from being built.

## Deploy to Kubernetes (minikube)

```bash
minikube start

# database
kubectl apply -f k8s/postgres/secret.yaml
kubectl apply -f k8s/postgres/pvc.yaml
kubectl apply -f k8s/postgres/service.yaml
kubectl apply -f k8s/postgres/deployment.yaml
kubectl rollout status deployment/grove-db

# app
docker build -t habit-grove:manual .
minikube image load habit-grove:manual
sed "s|IMAGE_PLACEHOLDER|habit-grove:manual|" k8s/deployment.yaml | kubectl apply -f -
kubectl apply -f k8s/service.yaml
kubectl rollout status deployment/habit-grove

# check
curl -s "http://$(minikube ip):30080/actuator/health"
BASE="http://$(minikube ip):30080" ./scripts/demo.sh
```

Configuration comes from environment variables in `k8s/deployment.yaml`, which override `application.properties`:

| Variable | Value in cluster |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://grove-db:5432/grove` |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | from the app config (see Security notes) |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update`, so tables and data survive restarts |

## CI/CD pipeline (Jenkins)

The `Jenkinsfile` runs these stages:

1. **Build & Test**: `docker build` runs `mvn package` (all tests) inside a Java 25 container. A failing test stops the pipeline here.
2. **Load image into minikube**: the built image is loaded into the cluster.
3. **Deploy database**: applies the PostgreSQL secret, PVC, service, and deployment, then waits for rollout.
4. **Deploy to Kubernetes**: applies the app deployment with the new image tag and the service, then waits for rollout.
5. **Smoke test**: calls `/actuator/health` through the NodePort.

Each build tags the image `habit-grove:<BUILD_NUMBER>`.

Jenkins runs as a system service. It needs the `docker` group membership and a copy of the minikube kubeconfig in its home directory (`/var/lib/jenkins/.kube` and `/var/lib/jenkins/.minikube`).

## Verifying data persistence

```bash
ID=<user id from demo output>
curl -s "http://$(minikube ip):30080/users/$ID/tree"
kubectl rollout restart deployment/habit-grove
kubectl rollout status deployment/habit-grove
curl -s "http://$(minikube ip):30080/users/$ID/tree"   # same result
```

## Testing

| Test class | Type | What it covers |
|---|---|---|
| `ScoringRulesTest`, `DeathStreakTest` | Unit | Daily score, rolling score, health levels, 7-day death rule |
| `LogServiceIntegrationTest`, `GrowthOverTimeTest` | Spring + database | Snapshots, daily cap, negative habits, growth and rolling over days |
| `ApiFlowTest` | HTTP | End-to-end endpoints, 404/403/409 responses, friend sharing |

## Known limits

- **Single replica.** Scaling out needs the same data for every pod, which the current design handles through PostgreSQL. The Deployment still runs one pod for simplicity.
- **No authentication.** Users are identified by ID in the URL. A real app would add login and check the caller's identity on every request.
- **Ownership checks are partial.** Logging checks that the habit belongs to the user in the URL. Other endpoints trust the ID in the path.
- **Secrets in plain YAML.** The database password is in `k8s/postgres/secret.yaml` and `k8s/deployment.yaml`. Production would use a sealed secret or an external secret store.
- **Dead trees are not revivable.** Once a tree dies, the user starts a new one.
- **No frontend in this repo.** The API is designed for a separate client.
- **Jenkins uses copied minikube credentials.** Fine for a local setup, not a production pattern.

## Security notes

Do not commit real credentials. The sample password in `k8s/postgres/secret.yaml` is for local development only.
