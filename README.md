# DevVault Backend

Private internal dependency registry backend for **i-exceed DevVault**.

A Spring Boot 3 REST API that lets teams publish, discover, and consume internal Java libraries through a Maven-compatible repository, with a web UI (`devvault-frontend`) on top. Consuming and publishing use standard `mvn` — no custom tooling required on developer machines.

---

## Tech Stack

| Layer      | Technology                                  |
| ---------- | ------------------------------------------- |
| Language   | Java 17                                     |
| Framework  | Spring Boot 3.2.5 (Spring Web, Data JPA, Security, Validation) |
| Build      | Maven 3.8+                                  |
| Database   | MySQL 8 (`devvault`, `ddl-auto=update`)     |
| Auth       | JWT stored in an httpOnly cookie (`devvault_token`) |
| Storage    | Local filesystem (`./storage`, Maven layout) |
| Docs/UI    | Next.js 14 frontend                             |

---

## Prerequisites

- JDK 17
- Maven 3.8+
- MySQL 8 running locally with a `devvault` database (created automatically via `createDatabaseIfNotExist`)
- Default credentials: `root` / `root`

---

## Getting Started

```bash
# 1. Start MySQL and make sure it is reachable on localhost:3306

# 2. Run the backend (from this folder)
mvn spring-boot:run
# or package and run
mvn clean package
java -jar target/devvault-backend-0.0.1-SNAPSHOT.jar
```

The API is served at `http://localhost:8080`.

> Seeded on first start: admin account `admin@devvault.local` / `Admin@123` and sample artifacts.

---

## Configuration

All settings live in `src/main/resources/application.properties` and can be overridden with environment variables.

| Property | Env var | Default | Description |
| -------- | ------- | ------- | ----------- |
| `spring.datasource.url` | `DB_URL` | `jdbc:mysql://localhost:3306/devvault?...` | MySQL connection |
| `spring.datasource.username` | — | `root` | DB user |
| `spring.datasource.password` | — | `root` | DB password |
| `server.port` | `SERVER_PORT` | `8080` | API port |
| `devvault.storage.location` | `STORAGE_LOCATION` | `./storage` | Where JARs/POMs are stored on disk |
| `devvault.jwt.secret` | `JWT_SECRET` | devvault-... | JWT signing key |
| `devvault.jwt.expiration-ms` | `JWT_EXPIRATION_MS` | `86400000` | Token lifetime (1 day) |
| `devvault.cors.allowed-origins` | `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Allowed web frontend origins |
| `devvault.remote.enabled` | `REMOTE_PROXY_ENABLED` | `true` | Enable the Maven Central proxy |
| `devvault.remote.url` | `REMOTE_PROXY_URL` | `https://repo1.maven.org/maven2` | Upstream repository for proxying |
| `devvault.remote.connect-timeout-ms` | `REMOTE_PROXY_CONNECT_TIMEOUT` | `5000` | Proxy connect timeout |
| `devvault.remote.read-timeout-ms` | `REMOTE_PROXY_READ_TIMEOUT` | `30000` | Proxy read timeout |

---

## Project Structure

```
src/main/java/com/company/devvault/
├── artifact/       # Artifact domain: entities, DTOs, repositories, service, controller
│   ├── controller/ # REST endpoints (/api/artifacts/**)
│   ├── dto/        # Request/response objects
│   ├── entity/     # JPA entities (Artifact, ArtifactVersion, ArtifactFile, ...)
│   ├── mapper/     # Entity <-> DTO mapping
│   ├── repository/ # Spring Data repositories
│   └── service/    # Business logic
├── analytics/      # Download events + analytics summaries
├── audit/          # Audit trail for sensitive actions
├── auth/           # Login, JWT issuing, cookie filter, security utils
├── config/         # Security config, data seeder, CORS
├── maven/          # Maven-compatible repository layer
│   ├── controller/ # /repository/maven/**
│   ├── parser/     # Maven path parsing (files, checksums, metadata)
│   └── service/    # MavenRepositoryService + RemoteMavenProxyService
├── storage/        # Local filesystem storage abstraction
├── user/           # User management (admin creates users)
└── common/         # Exceptions, checksums, Maven coordinates, security utils
```

---

## API Endpoints

### Authentication (`/api/auth`)
| Method | Path | Access | Description |
| ------ | ---- | ------ | ----------- |
| POST | `/api/auth/login` | Public | Login with email + password, sets JWT cookie |
| POST | `/api/auth/logout` | Public | Clears the JWT cookie |
| GET  | `/api/auth/me` | Authenticated | Current user profile |

### Artifacts (`/api/artifacts`)
| Method | Path | Access | Description |
| ------ | ---- | ------ | ----------- |
| GET | `/api/artifacts` | Public | Paginated artifact list (sort, category, owner filters) |
| GET | `/api/artifacts/recent` | Public | Recently published artifacts |
| GET | `/api/artifacts/{id}` | Public | Artifact detail |
| GET | `/api/artifacts/{id}/versions` | Public | Versions of an artifact |
| GET | `/api/artifacts/{id}/versions/{version}` | Public | Single version + files |
| GET | `/api/artifacts/{id}/versions/{version}/files/{fileId}/download` | Public | Download a file (tracks analytics) |
| POST | `/api/artifacts` | Public* | Create an artifact (`developerName`/`developerEmail` required when anonymous) |
| POST | `/api/artifacts/{id}/versions` | Public* | Publish a version (multipart: metadata + jar + optional pom/sources/javadoc) |
| PUT | `/api/artifacts/{id}` | Authenticated | Update artifact metadata |
| DELETE | `/api/artifacts/{id}` | ADMIN | Delete artifact + all versions/files (cascade) |
| POST | `/api/artifacts/{id}/versions/{version}/recommend` | Maintainer/ADMIN | Mark version recommended |
| POST | `/api/artifacts/{id}/versions/{version}/deprecate` | Maintainer/ADMIN | Deprecate a version |
| POST | `/api/artifacts/{id}/versions/{version}/undeprecate` | Maintainer/ADMIN | Restore a deprecated version |

### Discovery (`/api/search`, `/api/categories`, `/api/analytics`)
| Method | Path | Access | Description |
| ------ | ---- | ------ | ----------- |
| GET | `/api/search?q=...` | Public | Full-text artifact search |
| GET | `/api/categories` | Public | Category list |
| GET | `/api/analytics/summary` | Public | Totals (artifacts, versions, downloads, users) |
| GET | `/api/analytics/top-artifacts` | Public | Most downloaded artifacts |
| GET | `/api/analytics/recent-downloads` | Public | Recent download events |

### Users (`/api/users`) and Admin (`/api/admin`)
| Method | Path | Access | Description |
| ------ | ---- | ------ | ----------- |
| POST | `/api/users` | ADMIN | Create a user (self-registration is disabled) |
| GET/PUT | `/api/users/me` | Authenticated | View/update own profile |
| GET/PUT/DELETE | `/api/admin/users/**` | ADMIN | Manage users |
| GET | `/api/admin/analytics/**` | ADMIN | Admin analytics |
| GET | `/api/admin/audit-logs` | ADMIN | Audit log viewer |

### Maven Repository (`/repository/maven`)
| Method | Path | Access | Description |
| ------ | ---- | ------ | ----------- |
| GET | `/repository/maven/**` | Public | Maven-compatible artifact files, checksums, and metadata |
| PUT | `/repository/maven/**` | Public (Basic-auth optional) | `mvn deploy` — publishes jars/poms to the registry |

---

## Maven-Compatible Repository

The backend exposes a standard Maven repository at `http://localhost:8080/repository/maven`.

```
GET /repository/maven/<group path>/<artifactId>/<version>/<artifactId>-<version>.jar
GET /repository/maven/<group path>/<artifactId>/<version>/<artifactId>-<version>.jar.sha1
GET /repository/maven/<group path>/<artifactId>/<version>/<artifactId>-<version>.jar.md5
GET /repository/maven/<group path>/<artifactId>/maven-metadata.xml
```

POM consumers declare the repository in their `pom.xml`:

```xml
<repositories>
  <repository>
    <id>devvault</id>
    <url>http://localhost:8080/repository/maven</url>
  </repository>
</repositories>
```

- Checksums (`sha1`, `sha256`, `md5`) are computed on the fly from the stored files.
- Download events are recorded for analytics and per-user audit logs.
- Basic-auth credentials (email/password or token) are optional and used only to attribute downloads.

### Publishing with `mvn deploy`

No custom CLI is required — developers publish with stock Maven. Add distribution management
to the project `pom.xml` and credentials to `~/.m2/settings.xml`, then run `mvn deploy`:

```xml
<!-- pom.xml -->
<distributionManagement>
  <repository>
    <id>devvault</id>
    <url>http://localhost:8080/repository/maven</url>
  </repository>
</distributionManagement>
```

```xml
<!-- ~/.m2/settings.xml -->
<settings>
  <servers>
    <server>
      <id>devvault</id>
      <username>your@email.com</username>
      <password>your-password</password>
    </server>
  </servers>
</settings>
```

What happens on each PUT:

- `maven-metadata.xml` and checksum uploads are acknowledged and ignored (DevVault generates its own).
- Jars/poms are stored under `./storage` and registered in MySQL as INTERNAL artifacts.
- Versions are immutable — re-deploying the same `groupId:artifactId:version` returns 409.
- With Basic auth, the artifact is attributed to that account; otherwise it is attributed to the
  auto-created `maven-deploy@devvault.local` system user (like the anonymous web publish flow).

---

## Maven Central Proxy

When a dependency is requested that does not exist in the local registry, the backend
fetches it from the upstream repository (Maven Central by default), stores it locally, and
serves it — so **any Maven dependency works out of the box**.

1. A request for a missing file hits `/repository/maven/...`.
2. `RemoteMavenProxyService` downloads it from `devvault.remote.url`.
3. The artifact, version, and file rows are persisted in MySQL and the bytes are written
   to the storage folder (`./storage/<group>/<artifact>/<version>/...`).
4. The artifact appears in the web UI with a **Proxied from Maven Central** badge.

Metadata (`maven-metadata.xml`) is passed through live from the upstream repository.
Set `devvault.remote.enabled=false` to disable proxying.

---

## Storage

Files are stored on the local filesystem under `devvault.storage.location` (default `./storage`)
in a Maven-compatible layout:

```
./storage/
└── com/
    └── company/
        └── demo/
            └── hello-app/
                └── 1.0.0/
                    ├── hello-app-1.0.0.jar
                    └── hello-app-1.0.0.pom
```

Metadata (name, version, size, SHA-256, file type, storage path) is tracked in MySQL
(`artifacts`, `artifact_versions`, `artifact_files`, `download_events`).

---

## Security Notes

- Passwords are hashed with BCrypt.
- JWT is issued at login and stored in the httpOnly `devvault_token` cookie.
- Self-registration is disabled; accounts are created by an administrator.
- Publishing without login is allowed only when a `developerName` + `developerEmail`
  are provided — an account is auto-created for that email and the publish is audit-logged.
- Deleting an artifact cascades to versions, files, tags, maintainers, and download events.

---

## Build & Run

```bash
mvn clean compile     # compile only
mvn clean package     # full build + tests
mvn spring-boot:run   # run locally

# Optional: run with overrides
$env:SERVER_PORT=9000
$env:REMOTE_PROXY_ENABLED=false
mvn spring-boot:run
```