# Moderation Service

A microservice responsible for content moderation within the Traversium platform. It provides moderation capabilities to other services via gRPC and is designed for secure, observable, and scalable operation. It uses Azure's Content Safety service to moderate incoming text and flag it as profane, hostile, or otherwise unsafe.

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Running the Service](#running-the-service)
- [API Documentation](#api-documentation)
- [Architecture](#architecture)
- [Integration](#integration)
- [Monitoring and Health](#monitoring-and-health)

## Features

### Content Moderation
- gRPC-based moderation endpoints
- Designed for integration with other Traversium services

### Security
- Keycloak-based service-to-service security for gRPC communication
- Keycloak-based JWT token validation (provided by OAuth2 Resource Server)

### Configuration 
- Centralized configuration via Config Server support

### Integration
- gRPC communication with other Traversium services, such as TripService and SocialService
- Prometheus metrics for monitoring
- Healthcheck endpoints
- Structured logging with ELK stack integration

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Keycloak (for service-to-service authentication)
- Docker (optional, for containerized deployment)

## Configuration

### Application Properties

The service is configured via `src/main/resources/application.properties`. Key configurations:

```properties
# Application
spring.application.name=ModerationService
server.port=8082

# Config Server (optional)
spring.config.import=optional:configserver:http://localhost:8888

# gRPC server (ModerationService)
spring.grpc.server.port=9090
spring.grpc.server.reflection-service-enabled=true
spring.grpc.server.metrics.enabled=true
```

### Azure Content Safety service configuration

Configuration properties for Azure Content Safety (version 2024-09-01):
```properties
azure.contentsafety.endpoint=<resource-endpoint>
azure.contentsafety.api-key=<api-key>
azure.contentsafety.api-version=2024-09-01
moderation.policy.block-severity=2
```

**Property Descriptions:**

- **`azure.contentsafety.endpoint`**: Endpoint of Azure API 
- **`azure.contentsafety.api-key`**: Secret API key for accessing the Azure Content Safety resource
- **`azure.contentsafety.api-version`**: API version of Content Safety (default: 2024-09-01)
- **`moderation.policy.block-severity`**: The value checked against returned `max_severity` of gRPC Response (default: 2). 
  - If `max-severity` is greater or equal to `block_severity`, then ModerationService blocks and returns `SEVERITY_THRESHOLD_EXCEEDED`. 

### gRPC Configuration

gRPC client configuration for inter-service communication:

**ModerationService Server:**
- **`spring.grpc.server.port`**: Port of the ModerationService gRPC server for communication with other Traversium services

### Keycloak OAuth2 Configuration

Service-to-service authentication configuration for secure gRPC communication:

```properties
## Keycloak Resource Server Configuration
spring.security.oauth2.resourceserver.jwt.issuer-uri=<issuer-uri>
spring.security.oauth2.resourceserver.jwt.audiences=moderation-service
security.moderation.required-role=<realm-role>
```

**Property Descriptions:**

- **`spring.security.oauth2.resourceserver.jwt.issuer-uri`**: Keycloak endpoint URL for obtaining access tokens
    - Example: `http://localhost:8202/auth/realms/traversium/protocol/openid-connect/token`
- **`spring.security.oauth2.resourceserver.jwt.audiences`**: incoming Keycloak JWT token property (`aud`) that must include ModerationService as its audience
- **`security.moderation.required-role`**: Keycloak Realm role that the incoming Keycloak clients must have to pass authorization

### Resilience4j Configuration

Circuit breaker and retry mechanisms for gRPC calls (ModerationService):
```properties
resilience4j.circuitbreaker.instances.moderation-service.sliding-window-size=10
resilience4j.circuitbreaker.instances.moderation-service.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.moderation-service.wait-duration-in-open-state=30s
resilience4j.retry.instances.azureModeration.wait-duration=1s
resilience4j.retry.instances.azureModeration.max-attempts=2
```

- **`sliding-window-size`**: Number of calls to track for circuit breaker evaluation (default: 10)
- **`failure-rate-threshold`**: Percentage of failures that triggers circuit breaker to open (default: 50%)
- **`wait-duration-in-open-state`**: Wait time between retry attempts in an open state (default: 30 seconds)
- **`wait-duration`**: Wait time between retry attempts (default: 1 second)
- **`max-attempts`**: Maximum retry attempts for failed gRPC calls (default: 2)

## Running the Service

### Local Development

```bash
# Run with Maven
mvn spring-boot:run

# Or build and run JAR (e.g. version 1.1.0-SNAPSHOT)
mvn clean package
java -jar target/ModerationService-1.1.0-SNAPSHOT.jar
```

### Using Docker

```bash
# Build Docker image
docker build -t traversium-moderation-service .

# Run container
docker run -p 8082:8082 traversium-moderation-service
```

### Verify Service is Running

```bash
# Health check
curl http://localhost:8082/actuator/health

# Liveness probe
curl http://localhost:8082/actuator/health/liveness

# Readiness probe
curl http://localhost:8082/actuator/health/readiness
```

## API Documentation

### gRPC API

Service: `TextModerationService`
Proto file: `TextModeration.proto`
Transport: gRPC over HTTP/2
Authentication: OAuth2 (Keycloak), JWT via gRPC metadata

### gRPC Operations

#### ModerateText
```protobuf
rpc ModerateText (ModerateTextRequest) returns (ModerateTextResponse)
```
Moderates a given text input using Azure Content Safety and applies internal moderation policies to determine whether the text is allowed.

- **Request: `ModerateTextRequest`**
    
    | Field  | Type   | Description                               |
    |--------|--------|-------------------------------------------|
    | `text` | string | Text content to be analyzed and moderated |

- **Response: `ModerateTextResponse`**
    
    | Field  | Type                      | Description                               |
    |--------|---------------------------|-------------------------------------------|
    | `allowed` | bool                      | Whether the text is allowed after moderation |
    | `max_severity` | int32                     | Highest severity detected across all categories |
    | `categories` | repeated `CategoryResult` | Per-category moderation results |
    | `blocklist_hits` | repeated `BlocklistHit`   | Matched blocklist entries, if any |
    | `decision_reason` | string                    | Reason for blocking (e.g. `SEVERITY_THRESHOLD_EXCEEDED`), null if allowed |

- **`CategoryResult`**
    
    | Field  | Type   | Description                                                 |
    |--------|--------|-------------------------------------------------------------|
    | `category` | string | Moderation category (e.g. Hate, Sexual, Violence)           |
    | `severity` | int32 | Severity level (0–7, using Azure EightSeverityLevels model) |

    > Note: `EightSeverityLevels` set by default in ModerationService

- **`BlocklistHit`**

  | Field  | Type   | Description                                    |
      |--------|--------|------------------------------------------------|
  | `blocklist_name` | string | Name of the matched blocklist            |
  | `matched_text` | string | Text fragment that triggered the blocklist |


### Moderation Decision Logic

A text is blocked if either of the following is true:
1) The maximum detected severity is greater than or equal to the configured threshold: `moderation.policy.block-severity`
2) One or more blocklist matches are detected

If Azure Content Safety is unavailable, the service returns a safe fallback response allowing the text.

### gRPC Security

All gRPC calls must include a valid OAuth2 access token issued by Keycloak.
Required gRPC metadata:
```
Authorization: Bearer <access-token>
```
The token must:

- Be issued by the configured Keycloak realm
- Include moderation-service in the aud claim
- Contain the required realm role configured via: `security.moderation.required-role`

## Architecture

### gRPC-First Design
- Exposes moderation functionality exclusively via gRPC
- Designed for internal platform consumption
- Integrates external Azure Content Safety API via REST

### Security

- Service uses OAuth2 client credentials flow via Keycloak
- Secured service-to-service communication

### Resilience Patterns

- **Circuit Breaker**: Prevents cascading failures when calling Azure Content Safety API
- **Retry**: Automatic retry for transient failures
- **Fallback**: Graceful degradation when dependencies are unavailable or if external API is unavailable

## Integration

### gRPC Clients

Consumed by Traversium services for moderation operations (e.g. TripService, UserService...)

## Monitoring and Health

### Health Checks

- **Liveness**: `/actuator/health/liveness` - Indicates if the application is running
- **Readiness**: `/actuator/health/readiness` - Indicates if the application is ready to serve traffic

### Metrics

Prometheus metrics exposed at:
```
http://localhost:8082/actuator/prometheus
```

Key metrics:
- JVM metrics (memory, threads, GC)
- HTTP request metrics
- Circuit breaker state
- Custom business metrics

### Logging

Logs are structured in JSON format (Logstash encoder) for ELK Stack integration:
- Application logs: Log4j2
- Request/response logging
- Error tracking
