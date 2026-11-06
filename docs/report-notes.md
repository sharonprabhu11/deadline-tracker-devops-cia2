# Report notes (target: 5 to 8 pages)

Structure follows CLAUDE.md and the task list: problem, tools and why, pipeline stages, cloud resources,
challenges, limitations. No assignment document was found in `docs/`, so check the real assignment brief for any
required headings (cover page, declaration, references) and move these notes into them. Page budget in brackets.

---

## 1. Problem statement and project overview [about 0.5 page]

- Students juggle assignments, CIAs and lab records with different due dates and different mark weightages.
  A plain deadline list does not say what to do first.
- DeadlineDash is a small Spring Boot web app: add courses and tasks (title, due date, weight 1 to 100) and the
  page lists them by an **urgency score**, so the top row answers "what should I do first?".
- Score = `round(weight * 100 / (max(daysLeft, 0) + 1))`. Bands: OVERDUE, URGENT (>= 500), SOON (>= 150), LATER, DONE.
- The app is deliberately simple. The graded part is the **CI/CD pipeline**: every push to `main` is tested,
  built into a container image, scanned, stored in a registry, and rolled out to a Kubernetes cluster on Azure with
  no manual steps, and a bad release is rolled back automatically.
- Footer on every page shows version, short commit SHA and pod name, so the deployment is visible in the browser.

## 2. Tools chosen and why [about 1.5 pages]

| Area | Tool | Why this one |
| --- | --- | --- |
| Language/framework | Java 21, Spring Boot 4.1.2 (versions taken from start.spring.io) | LTS Java; Spring Boot gives web, validation, JPA and health endpoints with little code |
| UI | Thymeleaf server-side templates | One simple page, no separate frontend build |
| Build | Maven with wrapper (`./mvnw`) | Same build locally, in Docker and in CI, no tool install needed |
| Database | PostgreSQL (cluster, compose); H2 for tests | Real DB in deployment; tests run with no external service |
| Schema | Flyway (`V1__init.sql`) | Schema changes are versioned and run on startup |
| Tests | JUnit 5, Mockito, MockMvc | 24 tests, none need Postgres or the real date (fixed `Clock`) |
| Source control / CI | Git, GitHub, GitHub Actions | Pipeline lives in the repo (`.github/workflows/ci-cd.yml`) |
| Container | Docker multi-stage build | Small runtime image, no Maven or source in it, runs as non-root |
| Registry | Azure Container Registry (Basic) | Private registry next to the cluster; AKS pulls from it with its managed identity, so no pull secret |
| Orchestration | Azure Kubernetes Service, Free tier, 1 node | Real managed Kubernetes, no control-plane charge |
| Config management | Ansible (+ Ansible Vault) | Repeatable apply of manifests and creation of the DB Secret from an encrypted file |
| Security scanning | Trivy (image), gitleaks (secrets) | Catch known vulnerabilities and committed secrets before deploy |
| Cloud login | GitHub OIDC to Azure (Entra federated credential) | No long-lived password or key stored anywhere |

Decisions and alternatives are in `docs/decisions.md` (for example: k3s on EC2 was the first plan, replaced by AKS
when the project moved to Azure; Postgres in the cluster instead of a paid managed database).

## 3. Pipeline stages [about 1.5 pages]

Trigger: push to `main` (full pipeline) and pull requests (first two jobs only). File: `.github/workflows/ci-cd.yml`.

1. **secret-scan** (gitleaks 8.24.3, full history). Fails if a password or token is found.
2. **build-test** (Checkout, Java 21, `./mvnw package`, `./mvnw verify` = 24 tests).
3. **deploy** (only on push to `main`, needs both jobs above):
   1. Build Docker image tagged with the short commit SHA (`--build-arg GIT_SHA`).
   2. Trivy scan (fails on fixable CRITICAL findings).
   3. `azure/login` using OIDC (no secrets other than IDs).
   4. Push to ACR: `deadlinedash25500.azurecr.io/deadlinedash:<sha>`.
   5. `az aks get-credentials --admin`.
   6. `kubectl set image` on the Deployment (creates it on the very first run).
   7. **Verify**: `kubectl rollout status` (5 minute limit), then poll `/api/info` until it reports the new SHA.
   8. **Rollback**: if verify fails, `kubectl rollout undo` and wait for the old version to be Ready.

Evidence of it working (see `docs/evidence-checklist.md`):
- Green runs for commits `36dd233` (first deploy), `e8c644f` (heading change) and `24652b8` (revert).
- Redeploy demo: footer SHA changed `36dd233` -> `e8c644f` with no manual step; rolling update with 2 replicas and
  `maxUnavailable: 0` kept the site up.
- Rollback demo: commit `0137ca1` made readiness return HTTP 503. The new pod never became Ready, the old pods kept
  serving, verify timed out, the pipeline ran `rollout undo` (about 5 minutes), site stayed on `e8c644f` throughout.

## 4. Cloud resources used [about 1 page]

All in resource group `deadlinedash-rg`, region `indiasouthcentral`, Azure for Students subscription.

| Resource | Details | Cost note |
| --- | --- | --- |
| Resource group | Container for everything; deleting it removes everything | free |
| Container Registry `deadlinedash25500` | Basic tier, holds images by commit SHA | about 0.17 USD/day |
| AKS `deadlinedash-aks` | Free control-plane tier, 1 node `Standard_B2s` (2 vCPU, 4 GiB), 32 GB OS disk | about 1 USD/day (VM) |
| Load balancer + public IP | Created by the Kubernetes `Service` of type LoadBalancer | small daily charge |
| Disk (PVC) | 1 GiB managed disk for Postgres | small |
| Entra app registration `deadlinedash-github` | Federated credential for the repo's `main` branch; roles: AcrPush on ACR, AKS Cluster Admin Role on the cluster | free |
| Cost Management budget | Alert set before creating anything | free |

Kubernetes objects (namespace `deadlinedash`): Deployment with 2 replicas, Service (LoadBalancer, public port 8080 -> container port 8080; it was 80 until the port-change demo),
StatefulSet `postgres` with its PVC and headless Service, ConfigMap (DB host/name/port), Secret (DB user/password,
created by Ansible, never in git).

Security design to mention: no stored cloud keys (OIDC), least-privilege roles, Secret kept out of git (Ansible Vault),
non-root container, gitleaks and Trivy in the pipeline, no `latest` tag.

## 5. Challenges faced [about 1.5 pages]

Full raw log with exact error text: `docs/challenges.md`. Summary to expand:

1. **Azure for Students restrictions.** Only 5 regions allowed; VM list looked all "unavailable" because of zone
   restrictions and my own over-strict filter. Fixed by reading the full `az vm list-skus` table and checking quota;
   chose `indiasouthcentral` + `Standard_B2s`.
2. **gitleaks failed on the first push.** `fatal: ambiguous argument '<root>^..<head>'`. The action scans a commit
   range starting at the parent of the first commit, which does not exist for a repo's first push. Replaced it with the
   gitleaks binary scanning the whole history.
3. **Azure login rejected.** `AADSTS700213: No matching federated identity record found for presented assertion
   subject 'repo:sharonprabhu11@161618358/...@1446489116:ref:refs/heads/main'`. GitHub sent an immutable-ID subject;
   my federated credential had the plain `repo:owner/name` form. Fixed by copying the exact subject from the log.
4. **Site unreachable from the laptop.** HTTP 403 "FortiGuard Web Filter Violation". The network filter blocks plain
   HTTP to unrated raw IPs; the app was healthy (pods Ready, GitHub's verify step reached it, `kubectl get --raw
   .../proxy` worked). Verified from the phone network instead.
5. **Small local issues.** Port 8080 already in use (made compose port configurable); Ansible needs a UTF-8 locale;
   Ansible warned about the reserved variable name `namespace`; Spring refuses to start if a health group names a
   missing contributor (found while building the rollback demo).
6. **Building a safe failure for the rollback demo.** Deploys only run from `main`, so a throwaway branch could not
   reach the cluster. Solution: a switch in `application.yml` that makes readiness DOWN, kept off in tests so the
   pipeline still reaches the deploy stage; pushed, observed rollback, reverted.

## 6. Limitations and future work [about 0.5 page]

- One node and one Postgres replica: no high availability, and the database has no backups. A node failure means downtime.
- Postgres runs in the cluster for cost reasons; a managed database (Azure Database for PostgreSQL) would be better.
- Plain HTTP on a raw IP. No domain name, no TLS (would need Ingress and cert-manager).
- No authentication: anyone with the URL can add or delete tasks.
- The pipeline deploys straight to one environment; no staging, no manual approval, no database migration rollback
  (`rollout undo` reverts the app image, not the schema).
- Trivy only fails on fixable CRITICAL findings; stricter policy and image signing are possible.
- Ansible handles first-time setup; the day-to-day deploy is done by the pipeline, so two tools touch the cluster.
- Build timestamp in the footer data shows 1980 because Maven builds are reproducible by default.
- The cluster is deleted after the demo to save credit, so the live URL stops working after the viva.

## 7. Appendix ideas (not counted in the page limit)

- Screenshots from `docs/evidence-checklist.md`.
- Architecture diagram: GitHub -> Actions -> ACR -> AKS (Deployment x2, Postgres StatefulSet) -> LoadBalancer -> user.
- Repository layout and the urgency score table.
