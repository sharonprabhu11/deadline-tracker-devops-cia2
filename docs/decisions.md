# Decisions

Why each non-obvious choice was made (useful in the viva).

| # | Decision | Why |
| --- | --- | --- |
| 1 | Spring Boot 4.1.2, Java 21 | Versions come from start.spring.io (looked up on 2026-11-05: 4.1.2 is the default stable release). Java 21 is an LTS release that Boot 4.1 supports and the local JDK matches. |
| 2 | k3s on one EC2 instance | A full EKS cluster costs money per hour for the control plane. k3s is a real, certified Kubernetes in one small binary and runs on a single cheap instance. The manifests are plain Kubernetes, so they also work on EKS. |
| 3 | Amazon ECR | Image registry inside AWS, visible in the console for the evidence screenshot, and GitHub Actions can push to it with OIDC. |
| 4 | Postgres as a StatefulSet in the cluster | Avoids RDS cost. A StatefulSet gives the database a stable name and its own volume claim. Limitation: one replica, no backups. |
| 5 | NodePort Service (30080) | k3s has no cloud load balancer by default. NodePort exposes the app on the EC2 public IP, port 30080, with no extra component. |
| 6 | GitHub Actions with OIDC to AWS | The workflow gets short-lived credentials by assuming an IAM role, so no AWS keys are stored in GitHub. |
| 7 | H2 for tests, same Flyway migration | Tests run without Postgres. `V1__init.sql` uses only SQL that both H2 (PostgreSQL mode) and Postgres accept, so the real migration is also tested. |
| 8 | `spring.jpa.hibernate.ddl-auto=validate` | Flyway owns the schema; Hibernate only checks that entities match it. |
| 9 | `docker-compose.yml` host port is `${APP_PORT:-8080}` | Port 8080 may already be used on a developer machine. |
