# Architectural Decisions

This document captures the key technical decisions for the Htaccess Tester plugin.

## 1. Language: Kotlin-only

**Decision**: The entire plugin is implemented in Kotlin.

**Rationale**:
- Kotlin is the recommended language for new JetBrains plugin development
- Full interoperability with IntelliJ Platform APIs
- Concise syntax and null safety reduce boilerplate and bugs
- Coroutines provide clean async handling for HTTP operations

## 2. Remote Evaluation Model

**Decision**: All `.htaccess` rule evaluation is performed by a remote service.

**Rationale**:
- Accurate Apache mod_rewrite emulation is complex
- The [htaccess.madewithlove.com](https://htaccess.madewithlove.com) service provides well-tested evaluation
- Reduces plugin complexity and maintenance burden
- Consistent behavior with the web-based tester users may already know

**Trade-offs**:
- Requires internet connectivity
- Subject to remote service availability
- User rules are sent to a third-party service

## 3. Data Sent to Remote Service

The plugin sends the following data for each test:

| Data | Description |
|------|-------------|
| `url` | The URL to test against the rules |
| `rules` | The `.htaccess` file content |
| `serverVariables` | Optional key-value pairs for server variables (e.g., `HTTP_HOST`, `REQUEST_URI`) |

No authentication tokens, IDE telemetry, or system information is sent.

## 4. Data Stored Locally

### Plugin Settings (Application-level)
Stored in `htaccess-tester.xml`:
- API base URL (default: `https://htaccess.madewithlove.com`)
- Request timeout
- First-run acknowledgment flag
- Warning preferences

### Saved Test Cases (Project-level)
Stored in `.idea/htaccess-tester.xml`:
- Named test cases with URL, rules, and server variables
- Recent URL history (optional)

## 5. HTTP Client Choice

**Decision**: Use OkHttp for HTTP communication.

**Rationale**:
- Widely used and well-documented
- Simple API for synchronous and async requests
- Built-in support for timeouts, retries, and interceptors
- Small footprint

**Alternative considered**: Ktor client - also viable but OkHttp is more established in the Java ecosystem.

## 6. Project Structure

The plugin follows a layered architecture:

```
├── ide/          # IntelliJ Platform integration (actions, tool windows)
├── domain/       # Pure Kotlin domain models and services
├── http/         # HTTP client, DTOs, and mapping
├── settings/     # Persistent state components
└── util/         # Shared utilities
```

**Rationale**:
- `domain/` has no IDE dependencies, enabling pure unit tests
- `http/` is isolated for mock testing
- `ide/` contains all platform-coupled code
- Clear separation of concerns

## 7. Build System

**Decision**: Use IntelliJ Platform Gradle Plugin 2.x.

**Rationale**:
- Official, actively maintained build tooling
- Supports latest IDE versions
- Integrates with `verifyPlugin` for marketplace readiness
- Kotlin DSL for build scripts (`build.gradle.kts`)
