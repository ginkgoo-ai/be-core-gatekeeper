# Gatekeeper Microservice (Metadata-Driven Forms)

## Overview

The Gatekeeper microservice is a pivotal component of the SaaS platform, designed to deliver dynamic form and questionnaire generation capabilities driven by metadata. It empowers the system to flexibly collect information across a variety of workflows, including user onboarding, Know Your Customer (KYC) processes, compliance verifications, and other data-gathering scenarios. Architected using the Spring Cloud framework, Gatekeeper facilitates the dynamic adaptation of forms and questionnaires without necessitating new code deployments or service restarts.

## Features

### Completed ✅

* **Dynamic Form Rendering Engine:** Generates UIs for forms and questionnaires based on centrally managed metadata definitions.
* **Metadata Management APIs:** Provides comprehensive RESTful APIs for the Create, Read, Update, and Delete (CRUD) operations of form definitions, sections, fields, and associated validation rules.
* **Form Metadata Versioning:** Supports multiple versions of form metadata, allowing for iterative changes, A/B testing, and rollback capabilities.
* **Conditional Logic Engine:** Enables dynamic visibility and behavior of form sections and fields based on user input or predefined conditions derived from metadata.
* **Server-Side Input Validation:** Performs initial data validation against metadata-defined rules upon submission before relaying data to downstream services.
* **Extensible Metadata Schema:** Designed with a flexible and extensible schema to accommodate new form element types, validation rules, and UI behaviors as requirements evolve.

### In Progress 🚧

* **Advanced Inter-Field Dependencies:** Implementing more complex logic for dependencies between fields (e.g., calculated fields, cascading selections).
* **Integration with UI Component Libraries:** Developing standardized adapters for popular frontend UI component libraries to streamline form rendering.
* **Form Submission Analytics:** Adding capabilities to track form completion rates, field-level drop-offs, and time-to-completion.
* **Workflow Orchestration Hooks:** Designing integration points for external workflow engines to manage multi-step form processes.
* **Multi-Language Support in Metadata:** Enhancing metadata schema to support internationalization for form labels, messages, and options.

## Tech Stack

* **Java:** 17+ (or specify your target version, e.g., Java 21)
* **Spring Boot:** 3.x
* **Spring Cloud Suite:**
    * **Spring OAuth2:** For authentication and authorization. Acts as a resource server for token validation.
    * **Spring Cloud OpenFeign / WebClient:** For resilient and declarative inter-service communication (e.g., fetching dynamic options, calling downstream services).
* **Database:**
    * **Metadata Store:** PostgreSQL 14+ / MySQL / MongoDB (Choose based on preference for relational structure vs. document flexibility).
    * **Form Data Store (Optional):** NoSQL (e.g., MongoDB for flexible schema) or Relational DB with JSONB types.
* **Messaging Queue:** (Optional, e.g., Apache Kafka, RabbitMQ) For asynchronous processing of form submissions or event-driven integrations.
* **Build Tool:** Maven 3.8+ or Gradle
* **Containerization:** Docker (for packaging and deployment)

## Getting Started

```bash
# Clone the repository
git clone <your-gatekeeper-repository-url>
cd gatekeeper-service

# Build the project using Maven
mvn clean install

# Or using Gradle
# ./gradlew clean build

# Run the application
mvn spring-boot:run

# Or using Gradle
# ./gradlew bootRun

```



## Health Check

Service health and basic information can be monitored via Spring Boot Actuator endpoints:

Bash

```
GET /actuator/health

# Example Response:
{
    "status": "UP"
    // Additional details might be present depending on configuration
}

GET /actuator/info

# Example Response:
{
    // Application specific info like name, version etc.
}
```

## Configuration

Key configuration properties are managed in `application.yml` (or `application.properties`). Sensitive values and environment-specific settings should be externalized (e.g., via Spring Cloud Config, environment variables, or Kubernetes ConfigMaps/Secrets).

YAML

```
server:
  port: 8080 # Default port for Gatekeeper

spring:
  application:
    name: gatekeeper-service
  # Datasource configuration for Metadata Store (Example for PostgreSQL)
  datasource:
    url: ${METADATA_DB_URL:jdbc:postgresql://localhost:5432/gatekeeper_metadata_db}
    username: ${METADATA_DB_USER:gatekeeper_user}
    password: ${METADATA_DB_PASSWORD:secretpassword}
    driver-class-name: org.postgresql.Driver
  jpa: # If using Spring Data JPA
    hibernate:
      ddl-auto: validate # Recommended: 'validate' or 'none' for production, 'update' for dev
    show-sql: false # Set to true for debugging SQL queries in dev
  # Spring Cloud Service Discovery Client (Example for Eureka)
  eureka:
    client:
      serviceUrl:
        defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
    instance:
      prefer-ip-address: true # Use IP address for registration

# Gatekeeper Specific Configuration
gatekeeper:
  form:
    metadata:
      cache:
        enabled: true
        ttl-seconds: 3600 # Cache TTL for frequently accessed form definitions
  # Potentially, API keys or endpoints for services Gatekeeper interacts with
  # external-services:
  #   kyc-service-url: ${KYC_SERVICE_ENDPOINT}

logging:
  level:
    com.yourcompany.gatekeeper: INFO # Your base package
    org.springframework.web: INFO
```

*Ensure environment variables (e.g., `METADATA_DB_URL`, `EUREKA_SERVER_URL`) are properly set in your deployment environment or local `bootstrap.yml` if using Spring Cloud Config.*

## API Endpoints (Key Examples)

### Metadata Management

- `POST /api/gatekeeper/v1/forms`: Create a new form definition.
- `GET /api/gatekeeper/v1/forms/{formDefinitionId}`: Retrieve a specific form definition.
- `PUT /api/gatekeeper/v1/forms/{formDefinitionId}`: Update an existing form definition.
- `GET /api/gatekeeper/v1/forms?name={formName}&status={status}`: List form definitions with optional filters.
- `POST /api/gatekeeper/v1/forms/{formDefinitionId}/versions`: Create a new version of a form.

### Form Rendering & Submission

- ```
  GET /api/gatekeeper/v1/render/forms/{formIdentifier}
  ```

  : Fetch the active form metadata for rendering (by name or ID).

  - Query Params: `?version={versionNumber}` (optional, to get a specific version)
  - Query Params: `?contextParam1=value1` (optional, for server-side conditional logic based on context)

- `POST /api/gatekeeper/v1/submit/forms/{formIdentifier}`: Submit collected form data.

## Requirements

- JDK 17+ (or your specified version)
- Maven 3.8+ or Gradle (corresponding version)
- PostgreSQL 14+ / MongoDB 5.x+ (or your chosen database and version)
- Access to a Service Discovery server (if enabled, e.g., Eureka)
- Access to a Configuration server (if using Spring Cloud Config)

# AGPLv3 & Commercial License


## License

This project is available under a **dual licensing model**:

- **Open Source License (AGPL-3.0)**:  
  This software is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)**.  
  See the **[LICENSE](./LICENSE)** file for details.

- **Commercial License**:  
  If you wish to use this software **without complying with AGPL-3.0**  
  or require a **proprietary license** with different terms,  
  please contact **[license@ginkgoo.ai](mailto:license@ginkgoo.ai)**.

## Contributing

We welcome contributions!
Please read our **[Contributing Guide](./CONTRIBUTING.md)** before submitting issues or pull requests.

## Code of Conduct

To foster a welcoming and inclusive environment, we adhere to our **[Code of Conduct](./CODE_OF_CONDUCT.md)**.
All contributors are expected to follow these guidelines.

## Contributor License Agreement (CLA)

Before making any contributions, you must agree to our **[CLA](./CLA.md)**.
This ensures that all contributions align with the project’s **dual licensing model**.

If you have any questions, contact **[license@ginkgoo.ai](mailto:license@ginkgoo.ai)**.

---

© 2025 Ginkgo Innovations. All rights reserved.