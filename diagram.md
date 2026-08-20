# DevVault Backend — Diagrams

## 1. How the backend works (architecture)

```
                        ┌─────────────────────────────────────────────────┐
                        │              FRONTEND (Next.js :3000)           │
                        │   Artifacts · Dashboard · Admin · Publish UI    │
                        └───────────────────────┬─────────────────────────┘
                                                │  /api/**  (JWT cookie)
                                                ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                     DEVVAULT BACKEND (Spring Boot :8080)                  │
│                                                                           │
│  ┌───────────────┐   ┌──────────────────┐   ┌──────────────────────────┐  │
│  │ SecurityConfig │──▶│ JwtCookieFilter  │──▶│  AuthController          │  │
│  │  (auth rules)  │   │ (validates JWT)  │   │  login / me / logout    │  │
│  └───────────────┘   └──────────────────┘   └──────────────────────────┘  │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │  REST CONTROLLERS (/api/...)                                        │  │
│  │   ArtifactController   UserController   AnalyticsController         │  │
│  │   AdminController      AuditController                              │  │
│  └───────────────┬─────────────────────────────────────────────────────┘  │
│                  ▼                                                        │
│   ┌──────────────────────────┐    ┌──────────────────────────────┐        │
│   │      SERVICES            │    │  MAVEN REPOSITORY ENDPOINT   │        │
│   │  ArtifactService         │    │  /repository/maven/**        │        │
│   │  UserService             │◀───│  ┌────────────────────────┐  │        │
│   │  AnalyticsService        │    │  │ GET  resolveFile ──────┼──┼─┐      │
│   │  AuditService            │    │  │ GET  metadata          │  │ │      │
│   └───────────┬──────────────┘    │  │ PUT  deploy (mvn)      │  │ │      │
│               │                   │  └───────────┬────────────┘  │ │      │
│               │                   │              ▼               │ │      │
│               │                   │   RemoteMavenProxyService ───┼─┼─┼─┐  │
│               │                   │   (fetch from Maven Central) │ │ │ │  │
│               │                   └──────────────────────────────┘ │ │ │  │
│               ▼                                                    ▼ ▼ ▼  │
│   ┌─────────────────────────┐   ┌──────────────────────────────────────┐  │
│   │  JPA REPOSITORIES       │   │  ArtifactStorageService             │  │
│   │  ArtifactRepository     │   │  (files on disk)                    │  │
│   │  VersionRepository      │   │  storage/<group>/<artifact>/        │  │
│   │  FileRepository         │   │          <version>/<file>           │  │
│   │  UserRepository         │   │                                     │  │
│   │  DownloadEventRepo      │   └──────────────────────────────────────┘  │
│   └───────────┬─────────────┘                                              │
└───────────────┼───────────────────────────────────────────────────────────┘
                ▼
   ┌──────────────────────────────┐
   │   DATABASE (MySQL/Supabase)  │
   │   artifacts · versions ·     │
   │   files · users · downloads  │
   │   audit_logs · events        │
   └──────────────────────────────┘
```

## 2. How the backend talks to Maven (mvn client flow)

```
                         YOUR MACHINE
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   ~/.m2/settings.xml  ── credentials (id=devvault, email+password)  │
│                                                                     │
│   pom.xml             ── <repository> + <dependency>                │
│                          <distributionManagement> for deploy        │
│                                                                     │
│   mvn clean package   ──▶  reads pom.xml, asks Maven for deps       │
│   mvn clean deploy    ──▶  uploads jar/pom to repository            │
│                                                                     │
│   ~/.m2/repository    ◀──  cached downloads (local storage)         │
└──────────────────────┬──────────────────────────────────────────────┘
                       │  HTTP
                       │  (GET resolve / PUT deploy / checksums / metadata)
                       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│              DEVVAULT BACKEND  (localhost:8080)                          │
│                                                                          │
│  MavenRepositoryController  (/repository/maven/**)                       │
│       │                                                                  │
│       ├─▶ MavenPathParser ──▶ parse URL path into:                      │
│       │      groupId / artifactId / version / fileName / fileType       │
│       │                                                                  │
│       ├─▶ GET  (pull) ──▶ resolveFile()                                 │
│       │      │                                                            │
│       │      ├─ in DB? ──▶ stream file bytes + record download ⬆count   │
│       │      │                                                            │
│       │      └─ NOT in DB? ──▶ RemoteMavenProxyService                  │
│       │             ├─ parent/BOM pom (*-parent/*-bom)? ──▶ serve only   │
│       │             └─ real dep ──▶ fetch from Maven Central             │
│       │                     └─▶ store file + register PROXY artifact     │
│       │                                                                  │
│       ├─▶ PUT  (mvn deploy) ──▶ deploy()                                │
│       │      │  Basic Auth (settings.xml credentials)                    │
│       │      ├─ checksum/metadata files? ──▶ ignore (200)                │
│       │      └─ real jar/pom ──▶ create INTERNAL artifact + version     │
│       │                duplicate version? ──▶ 409 (immutable)            │
│       │                └─▶ store bytes + sha256 + audit log              │
│       │                                                                  │
│       └─▶ GET metadata ──▶ generates maven-metadata.xml on the fly       │
│            (versions list Maven asks for during resolution)              │
│                                                                          │
│   ArtifactStorageService:  storage/<group>/<artifact>/<version>/<file>   │
│   JPA Repositories ──▶  DB (MySQL/Supabase)                              │
└──────────────────────────────────────────────────────────────────────────┘
```

## 3. Maven's exact requests during a build

```
mvn clean package (pull):
  GET  /repository/maven/org/springframework/data/spring-data-bom/2021.2.8/spring-data-bom-2021.2.8.pom
       ──▶ 200 + stores locally
  GET  .../spring-data-bom-2021.2.8.pom.sha1
       ──▶ generated checksum
  GET  .../maven-metadata.xml
       ──▶ versions list
  (if dependency not found → proxy fetches from repo1.maven.org, caches it)

mvn clean deploy (push):
  PUT  /repository/maven/com/myorg/lib/1.0.0/lib-1.0.0.jar    + Basic Auth  ──▶ 200 (artifact+version created)
  PUT  /repository/maven/com/myorg/lib/1.0.0/lib-1.0.0.pom                  ──▶ 200
  PUT  .../lib-1.0.0.jar.sha1 ... (ignored, DevVault generates its own)
```

> **Key point:** Maven never talks to the `/api/**` endpoints or the frontend — it only speaks
> to the `/repository/maven/**` endpoint via standard GET/PUT, and DevVault makes it look
> like a normal Maven repository.