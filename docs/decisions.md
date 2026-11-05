# Decisions

Why each non-obvious choice was made (useful in the viva).

| # | Decision | Why |
| --- | --- | --- |
| 1 | Spring Boot 4.1.2, Java 21 | Versions come from start.spring.io (looked up on 2026-11-05: 4.1.2 is the default stable release). Java 21 is an LTS release that Boot 4.1 supports and the local JDK matches. |
| 2 | AKS, Free-tier control plane, one small node pool | CLAUDE.md now says Azure overrides AWS (supersedes the earlier default of k3s on EC2). The Free tier has no control-plane charge; only the node VM is billed. Manifests are plain Kubernetes. |
| 3 | Azure Container Registry (Basic), attached to AKS | Attaching ACR gives the cluster pull rights through its managed identity, so no image pull secret is needed or expires. |
| 4 | Postgres as a StatefulSet in the cluster | Avoids RDS cost. A StatefulSet gives the database a stable name and its own volume claim. Limitation: one replica, no backups. |
| 5 | LoadBalancer Service | AKS provisions an Azure public IP for it, so the app is reachable on port 80 with no extra component. |
| 6 | GitHub Actions with OIDC to Azure | An Entra app registration with a federated credential for this repo lets the workflow log in with `azure/login` without storing any client secret. |
| 7 | H2 for tests, same Flyway migration | Tests run without Postgres. `V1__init.sql` uses only SQL that both H2 (PostgreSQL mode) and Postgres accept, so the real migration is also tested. |
| 8 | `spring.jpa.hibernate.ddl-auto=validate` | Flyway owns the schema; Hibernate only checks that entities match it. |
| 9 | `docker-compose.yml` host port is `${APP_PORT:-8080}` | Port 8080 may already be used on a developer machine. |

| 10 | No k3s Ansible role | AKS is managed; Ansible only applies manifests and creates the Secret from a Vault file. |
| 11 | Region `indiasouthcentral`, node `Standard_B2s` | The Azure for Students policy allows only 5 regions. `Standard_B2s` showed no restrictions in `indiasouthcentral` (checked with `az vm list-skus`). |
| 12 | gitleaks job in the pipeline | Fails the run if a secret is committed. First tried gitleaks-action v3.0.0, but on the first push it scanned `<root commit>^..HEAD`, an invalid range, and failed. Now the pinned gitleaks 8.24.3 binary scans the full history directly. The deploy job waits for it. |
