# AI Agents

This repository defines a set of AI agents used by GitHub Copilot Chat and Copilot Workspace.  
These agents enforce consistent architecture, coding standards, testing practices, and DevOps workflows.

All agents follow these global rules unless overridden:

- Java 21  
- Spring Boot 3/4  
- Maven (no Lombok)  
- Package-by-feature  
- SOLID, DRY, KISS, YAGNI  
- Multi-tenant architecture (column, schema, database; switchable)  
- JWT + OAuth2 on all endpoints  
- Stripe via extensible provider abstraction  
- Test Pyramid + TDD principles  
- Semantic versioning (bump PATCH)


## Agents


### **architecture-agent**
Location: `.github/copilot-instructions/architecture-agent.md`  
Purpose: System architecture, domain modeling, multi-tenancy, patterns, module boundaries.

### **backend-feature-agent**
Location: `.github/copilot-instructions/backend-feature-agent.md`  
Purpose: Backend code generation — controllers, services, repositories, adapters.

### **testing-agent**
Location: `.github/copilot-instructions/testing-agent.md`  
Purpose: Unit/integration test generation using TDD and Given/When/Then.

### **devops-agent**
Location: `.github/copilot-instructions/devops-agent.md`  
Purpose: Dockerfile, Compose, CI/CD, JVM tuning, infra recommendations.

### **refactoring-agent**
Location: `.github/copilot-instructions/refactoring-agent.md`  
Purpose: Safe refactoring, DIFF-based improvements, cleanup, optimization.


Usage example:  
Use agent by name