# CLAUDE.md: DeadlineDash

Project guide for Claude Code. Read this fully before making changes.

## Cloud provider: Azure (overrides every AWS reference in this file)

Use Azure instead of AWS:
- Registry: Azure Container Registry (Basic tier). Image: <ACR_LOGIN_SERVER>/deadlinedash:<commit-sha>.
- Cluster: AKS with the Free tier control plane and one small node pool (smallest sensible
  VM size; check current pricing). Attach ACR to the cluster so pods pull images without a pull secret.
- Pipeline auth: GitHub OIDC to Azure (Entra app registration with a federated credential for this repo,
  azure/login action). No client secrets stored.
- Pipeline commands: push to ACR after login; use `az aks get-credentials` for kubectl access.
- Service type: LoadBalancer.
- Ansible: no k3s role. roles/deploy applies manifests and creates the Secret from an Ansible Vault vars file.
- Write docs/azure-setup.md instead of docs/aws-setup.md. First item: a Cost Management budget alert.
  Last item: delete the resource group after the demo.
- Never run az commands that create, scale or delete billable resources yourself; give me the commands.

## What this project is

DeadlineDash is a small Spring Boot web app for students. You add courses and tasks (assignments, CIAs, lab records) with a due date and a weightage, and the page lists them sorted by an **urgency score**, so the top of the list always answers "what should I do first?".

It is built for a college assignment (CIA 2) whose real goal is a working **CI/CD pipeline**: Git, Maven, Docker, a container registry, Kubernetes on a cloud provider, and Ansible. The app is intentionally simple. The pipeline and deployment are what get graded and demonstrated.

The owner is a student who must **explain every file in a viva**. Keep code simple and readable, and prefer fewer moving parts over clever ones.

If the assignment document is in `docs/`, it is the source of truth. If anything here conflicts with it, stop and ask the owner.

## Non-negotiable rules

1. **Walking skeleton first.** Get a trivial page through the whole pipeline to the cluster before building features. Every later feature then ships through a pipeline that already works.
2. **Small steps.** Do one task at a time. After each, run the tests and tell the owner what changed and how to check it.
3. **Do not invent versions.** Generate the project from https://start.spring.io (or check current docs) and pin what it gives you. Use the Java LTS version that the chosen Spring Boot release supports. Look up current versions of GitHub Actions and Maven plugins instead of guessing.
4. **Never commit secrets.** No passwords, tokens, keys or kubeconfigs in the repo. Use placeholders in files, GitHub Actions secrets or variables in the pipeline, and Kubernetes Secrets in the cluster.
5. **Never run billable or destructive cloud commands yourself.** For anything that creates, resizes or deletes AWS resources (EC2, EKS, ECR, RDS, IAM), write the exact commands or Ansible tasks and let the owner run them. Remind the owner to set an AWS billing alert first.
6. **Ask before adding scope.** No new frameworks, services or dependencies beyond the stack below without asking.
7. **Keep it explainable.** If a choice is non-obvious, add a short comment or note in `docs/decisions.md` saying why.

## Stack

- Java and Spring Boot (Web, Thymeleaf, Data JPA, Validation, Actuator)
- Maven with the Maven Wrapper (`./mvnw`)
- PostgreSQL in Docker Compose and in the cluster, H2 for unit tests
- Flyway for the schema (one `V1__init.sql` migration)
- JUnit 5, Mockito, MockMvc
- Docker (multi-stage build), GitHub Actions, Amazon ECR
- Kubernetes (k3s on one EC2 instance, or EKS; the manifests must work on either)
- Ansible for host setup and applying manifests

## Features

### Pages and endpoints

| Route | Purpose |
| --- | --- |
| `GET /` | Task list sorted by urgency, add-task form, footer with version info |
| `POST /tasks` | Create a task (validated) |
| `POST /tasks/{id}/done` | Mark a task done |
| `POST /tasks/{id}/delete` | Delete a task |
| `POST /courses` | Create a course |
| `GET /api/info` | JSON: app version, short commit SHA, pod name, build time |
| `GET /actuator/health` | Health, with liveness and readiness probe groups enabled |

### Data model

- `Course`: `id`, `name`, `code`
- `Task`: `id`, `course` (many-to-one), `title`, `dueDate`, `weight` (integer 1 to 100, the marks weightage), `done` (boolean), `createdAt`

Validation: title not blank, due date required, weight between 1 and 100.

### The unique feature: urgency score

Put this in one small class (for example `UrgencyCalculator`) that takes a task and a `java.time.Clock`, so it is easy to unit test. Do not call `LocalDate.now()` directly.

```
daysLeft      = days from today to dueDate   (negative if overdue)
effectiveDays = max(daysLeft, 0)
score         = round(weight * 100 / (effectiveDays + 1))
```

Bands (keep thresholds as named constants so they can be tuned):

| Band | Rule |
| --- | --- |
| DONE | task is done (score 0, sorted last) |
| OVERDUE | daysLeft < 0 and not done (sorted first, then by score) |
| URGENT | score >= 500 |
| SOON | score >= 150 |
| LATER | otherwise |

Sort order: OVERDUE, then by score descending, then DONE last.

Required unit test cases (use a fixed clock):

| Weight | Due | Expected score | Expected band |
| --- | --- | --- | --- |
| 20 | today | 2000 | URGENT |
| 20 | in 3 days | 500 | URGENT |
| 30 | in 6 days | 429 | SOON |
| 10 | in 9 days | 100 | LATER |
| 20 | yesterday | 2000 | OVERDUE |
| 20 | any, done | 0 | DONE |

### Footer (this makes the DevOps visible in the browser)

Show version, short Git commit SHA and the pod name on every page.

- Version: from Spring Boot build info (`build-info` goal of the Spring Boot Maven plugin).
- Commit SHA: read env var `APP_GIT_SHA`, fall back to `dev`. The Dockerfile takes it from a build arg.
- Pod name: read env var `HOSTNAME` (in Kubernetes this is the pod name), fall back to `local`.

With two replicas, refreshing the page shows the pod name change. After a deploy, the SHA changes. This is the main thing the redeploy demo shows.

## Repository layout

```
deadlinedash/
├─ CLAUDE.md
├─ pom.xml
├─ mvnw, .mvn/
├─ Dockerfile
├─ .dockerignore
├─ docker-compose.yml          local app + Postgres
├─ src/
│  ├─ main/java/.../deadlinedash/
│  │  ├─ DeadlineDashApplication.java
│  │  ├─ course/               Course, CourseRepository, CourseService
│  │  ├─ task/                 Task, TaskRepository, TaskService, TaskForm
│  │  ├─ urgency/              UrgencyCalculator, UrgencyBand
│  │  ├─ web/                  PageController, InfoController
│  │  └─ config/               ClockConfig, BuildInfoConfig
│  ├─ main/resources/
│  │  ├─ application.yml
│  │  ├─ db/migration/V1__init.sql
│  │  └─ templates/            index.html, fragments/footer.html
│  └─ test/java/...            unit, service and MockMvc tests
├─ k8s/
│  ├─ namespace.yaml
│  ├─ configmap.yaml
│  ├─ secret.example.yaml      placeholder values only; the real Secret is never committed
│  ├─ postgres.yaml            StatefulSet + Service + PVC, or skip if using RDS
│  ├─ deployment.yaml
│  └─ service.yaml
├─ ansible/
│  ├─ inventory.ini.example
│  ├─ playbook.yml
│  └─ roles/{k3s,deploy}/
├─ .github/workflows/ci-cd.yml
└─ docs/
   ├─ decisions.md
   ├─ evidence-checklist.md    screenshots to capture for the report
   └─ report-notes.md
```

## Commands

```bash
./mvnw -B verify                                   # compile, package, run all tests
./mvnw spring-boot:run                             # run locally (needs Postgres or the dev profile)
docker compose up --build                          # app + Postgres locally on http://localhost:8080
docker build --build-arg GIT_SHA=$(git rev-parse --short HEAD) -t deadlinedash:local .
kubectl apply -f k8s/ && kubectl -n deadlinedash get pods
```

## Docker

- Multi-stage: a build stage runs Maven, a runtime stage copies the JAR onto a slim JRE image of the same Java version.
- Run as a non-root user. Expose port 8080.
- `ARG GIT_SHA` then `ENV APP_GIT_SHA=$GIT_SHA`.
- `.dockerignore` excludes `target/`, `.git/`, IDE files and `docs/`.

## Kubernetes

- Namespace `deadlinedash`.
- Deployment with **2 replicas**, rolling update with `maxUnavailable: 0` and `maxSurge: 1`.
- Readiness probe on `/actuator/health/readiness`, liveness on `/actuator/health/liveness`.
- CPU and memory requests and limits on every container (the cluster is small).
- Database settings come from a ConfigMap (host, name) and a Secret (user, password).
- Service exposes the app. Use a type that fits the cluster (LoadBalancer on EKS, NodePort or Ingress on k3s) and document the choice in `docs/decisions.md`.
- Image reference is `<ECR_REGISTRY>/deadlinedash:<commit-sha>`. Never use `latest`.
- ECR login tokens expire after a short time. On k3s, document how the image pull secret is created and refreshed, or note the fallback of a public registry.

## Pipeline (`.github/workflows/ci-cd.yml`)

Runs on every push to `main`. Pull requests run only the first three stages.

1. Checkout
2. Maven package
3. Run tests
4. Build the Docker image, tagged with the commit SHA (pass `GIT_SHA` as a build arg)
5. Scan the image (Trivy) and push to ECR
6. Deploy: `kubectl set image` on the Deployment
7. Verify: `kubectl rollout status`, then check that `/api/info` returns the new SHA
8. On failure of the verify step, run `kubectl rollout undo`

AWS authentication uses GitHub OIDC with an assumed IAM role, so no long-lived AWS keys are stored as secrets. Cluster access uses either `aws eks update-kubeconfig` (EKS) or a kubeconfig stored as a secret (k3s). Pin every action to a version.

## Ansible

- `roles/k3s`: install k3s on the EC2 host and fetch the kubeconfig (skip if using EKS).
- `roles/deploy`: apply the manifests with `kubectl`, create the Secret from an Ansible Vault encrypted vars file, and wait for the rollout.
- Commit `inventory.ini.example` only. Real inventory and vault passwords are never committed.

## Testing expectations

- `UrgencyCalculator`: every row in the table above, plus an edge case for each band boundary.
- Service tests with Mockito for creating and completing tasks.
- MockMvc test that `GET /` returns 200 and contains the footer text, and that an invalid task form is rejected.
- Tests must not depend on the real current date or on a running Postgres (use the fixed `Clock` and H2).

## Definition of done (CIA 2 deliverables)

- [ ] Git repo with Java source and `pom.xml`
- [ ] Unit tests passing in the pipeline
- [ ] `Dockerfile` and `.dockerignore`
- [ ] Pipeline file, with a green run on `main`
- [ ] Kubernetes manifests applied to a cloud cluster, pods Running
- [ ] Ansible playbook and inventory example
- [ ] App reachable in a browser, footer showing version, SHA and pod name
- [ ] Image visible in ECR
- [ ] Redeploy demo works: change a heading or the score weighting, push, and the footer SHA changes with no manual steps
- [ ] `docs/evidence-checklist.md` filled in with the screenshots to capture (cluster, ECR image, pods, browser, pipeline run)
- [ ] `docs/report-notes.md` has notes for the 5 to 8 page report: problem, tools chosen, pipeline stages, cloud resources, challenges

## Demo script (keep it working at all times)

1. Show the running pods and the page footer (SHA A, one pod name).
2. Change a visible heading or the score weighting. Commit and push to `main`.
3. Show the pipeline stages running and passing.
4. Show the new image tag in ECR.
5. Show the rolling update finish.
6. Refresh the page: the footer shows SHA B, and the pod name changes between refreshes.

## How to work with the owner

- Start each session by checking `git status` and the last pipeline result.
- Explain what you did in two or three plain sentences. No long recaps.
- When something fails, show the actual error, say the likely cause, and fix one thing at a time.
- When a step needs the owner's hands (AWS console, billing alert, GitHub secrets, DNS), give a short numbered checklist and wait.