# NeuroPace - Distributed Learning System

NeuroPace is a distributed learning system built with **Spring Boot 3.2**, **Java 17**, and **Maven** multi-module structure.

---

## 🚀 Microservice Modules & Ports

- **`api-gateway`**: Routing and entry point. (Port `8080`)
- **`cognitive-engine`**: Learning analytics, student performance records, database schema execution, and data generation. (Port `8081`)
- **`curriculum-engine`**: Subject structure and prerequisites hierarchy. (Port `8082`)
- **`scheduler-engine`**: Course schedules and learning window planning. (Port `8083`)
- **`event-queue`**: Ingests and processes student learning events. (Port `8084`)

---

## 🛠️ Technology Stack

- **Java 17** (Record classes, text blocks)
- **Spring Boot 3.2** & **Spring Web**
- **Spring Data JPA** & **Hibernate**
- **MySQL 8** (Persistence)
- **Redis 7** (Caching & messaging)
- **Lombok**
- **JUnit 5**, **Mockito**, & **AssertJ** for testing
- **Testcontainers** (Integration testing with MySQL and Redis)

---

## 🗄️ Database Schema & Data Generation

The database schema (`neuropace_schema.sql`) contains tables for:
- `students`
- `subjects`
- `prerequisites` (Unique relations ensuring a DAG structure)
- `enrollments`
- `learning_events`
- `load_windows`
- `reroute_decisions`

On application startup, a `DataGenerator` (`CommandLineRunner`) seeds sample data into MySQL:
1. Creates **100 sample students**.
2. Creates **50 sample subjects** with difficulty levels (1-10).
3. Connects subjects with a **valid DAG of prerequisites** (mathematically guaranteeing no cycles).
4. Inserts **1000 sample learning events** matched to student enrollments.

---

## ⚙️ Quick Start

### 1. Start Infrastructure (MySQL & Redis)
Ensure Docker is running, then start the containers:
```bash
docker compose up -d
```

### 2. Build & Test the Project
Build the entire reactor project and run the tests (which automatically execute integration tests against Testcontainers/Docker):
```bash
mvn clean package
```

### 3. Run a Module
To run a specific module (e.g., `cognitive-engine`):
```bash
mvn spring-boot:run -pl cognitive-engine
```

### 4. Health Checks
Each service exposes a health check endpoint:
- `GET http://localhost:8080/actuator/health` (API Gateway)
- `GET http://localhost:8081/actuator/health` (Cognitive Engine)
- `GET http://localhost:8082/actuator/health` (Curriculum Engine)
- `GET http://localhost:8083/actuator/health` (Scheduler Engine)
- `GET http://localhost:8084/actuator/health` (Event Queue)
