# Viva notes

Plain-language explanation of every important line, file by file, then 10 likely questions.
Open the real file next to these notes. Line numbers drift, so each point quotes the line.

---

## 1. Dockerfile

A Dockerfile is a recipe for building an image. This one has **two stages** (multi-stage build): one to build the app,
one to run it. Only the second stage ends up in the final image.

| Line | What it does |
| --- | --- |
| `FROM eclipse-temurin:21-jdk-noble AS build` | Start stage 1 from an image that has the full Java 21 JDK (needed to compile). Name it `build`. |
| `WORKDIR /workspace` | Work inside this folder. |
| `COPY mvnw pom.xml ./` and `COPY .mvn .mvn` | Copy only the build files first. |
| `RUN ./mvnw -B -q dependency:go-offline` | Download all libraries now. Because only `pom.xml` is copied before this, Docker caches this layer and does not re-download unless `pom.xml` changes. Faster builds. |
| `COPY src src` | Now copy the source code. |
| `RUN ./mvnw -B -q package -DskipTests` | Compile and make the JAR. Tests are skipped here because the pipeline already ran them. |
| `FROM eclipse-temurin:21-jre-noble` | Start stage 2 from a smaller image with only the Java runtime (JRE, no compiler). Same Java version as the build. |
| `RUN useradd --system --uid 1001 appuser` | Create an unprivileged user. |
| `COPY --from=build /workspace/target/*.jar app.jar` | Take only the finished JAR from stage 1. Maven, source code and the JDK stay behind, so the image is small. |
| `ARG GIT_SHA=dev` | A build-time input. The pipeline passes the commit SHA with `--build-arg`. |
| `ENV APP_GIT_SHA=$GIT_SHA` | Turn that input into an environment variable stored in the image. The app reads `APP_GIT_SHA` to show the SHA in the footer. |
| `USER appuser` | Run the app as that user, not root. If the app were hacked, the attacker would not be root in the container. |
| `EXPOSE 8080` | Documentation that the app listens on 8080. |
| `ENTRYPOINT ["java","-jar","app.jar"]` | The command that runs when the container starts. |

`.dockerignore` keeps `target/`, `.git/`, `docs/`, `k8s/`, IDE files out of the build context so builds are faster and
the image does not carry junk. `docker-compose.yml` runs the app plus Postgres locally; the app waits for the database
health check (`pg_isready`) before starting.

---

## 2. Pipeline file: `.github/workflows/ci-cd.yml`

GitHub Actions reads this file. A **workflow** has **jobs**; each job runs on a fresh virtual machine and has **steps**.

**Top of file**
- `on: push: branches: [main]` and `pull_request`: run on every push to `main`, and on pull requests.
- `permissions: id-token: write, contents: read`: lets the job request an OIDC token (proof of "I am this repo's workflow") and
  read the code. Nothing else is allowed. This is what makes password-less login to Azure possible.
- `env: APP, NAMESPACE`: shared names used by many steps.

**Job `secret-scan`**
- `actions/checkout@v7.0.1` with `fetch-depth: 0`: download the whole git history, not only the latest commit.
- Download the gitleaks binary and run `gitleaks detect --source . --exit-code 1`: fail the job if any secret appears
  anywhere in history. (We first used the packaged action; it failed on the first push, so we run the binary directly.)

**Job `build-test`** (runs on pushes and pull requests)
- `actions/setup-java` Java 21 Temurin with Maven cache: install Java.
- `./mvnw -B package -DskipTests`: compile and package.
- `./mvnw -B verify`: run all 24 tests. If any fails, the job fails and nothing is deployed.

**Job `deploy`**
- `needs: [secret-scan, build-test]`: wait for both to pass.
- `if: github.event_name == 'push' && github.ref == 'refs/heads/main'`: only deploy from `main`, never from pull requests.
- **Compute image name**: `SHA_SHORT=${GITHUB_SHA::7}` takes the first 7 characters of the commit hash;
  `IMAGE=<acr>.azurecr.io/deadlinedash:<sha>`. Written to `$GITHUB_ENV` so later steps can use them.
- **Build Docker image**: `docker build --build-arg GIT_SHA=$SHA_SHORT -t $IMAGE .` (this is how the SHA gets into the footer).
- **Trivy scan**: scans the image for known vulnerabilities. `severity: CRITICAL`, `ignore-unfixed: true`, `exit-code: 1`:
  fail only on critical problems that have a fix available.
- **azure/login** with client id, tenant id and subscription id: logs in using the OIDC token. Azure checks it against
  the federated credential (issuer, audience, and the exact subject `repo:...:ref:refs/heads/main`).
- **Push image to ACR**: `az acr login`, then `docker push`.
- **Get AKS credentials**: `az aks get-credentials --admin` writes a kubeconfig on the runner so `kubectl` can talk to the cluster.
- **Deploy**: if the Deployment does not exist yet (first run), create it from `k8s/deployment.yaml` with `sed`
  replacing the placeholder image; then always `kubectl set image deployment/deadlinedash deadlinedash=$IMAGE`. Changing the image
  triggers a rolling update.
- **Verify**: `kubectl rollout status --timeout=300s` waits until new pods are Ready. Then it reads the Service's public IP and polls
  `/api/info` up to 30 times until `commitSha` equals the new SHA. This proves the new code is really serving traffic.
- **Roll back**: `if: failure() && steps.deploy.outcome == 'success' && steps.verify.outcome == 'failure'`: only when the deploy
  went through but verify failed. Runs `kubectl rollout undo` and waits for the old version to be Ready.

**Why `secrets` vs `vars`:** `AZURE_CLIENT_ID/TENANT_ID/SUBSCRIPTION_ID` are secrets (hidden in logs); `ACR_NAME`,
`AKS_RESOURCE_GROUP`, `AKS_CLUSTER` are plain variables. None is a password.

---

## 3. Kubernetes manifests (`k8s/`)

**`namespace.yaml`**: creates the namespace `deadlinedash`, a separate folder-like space inside the cluster so our objects are grouped.

**`configmap.yaml`**: key/value non-secret settings: `DB_HOST: postgres`, `DB_PORT: "5432"`, `DB_NAME: deadlinedash`.
The app reads them as environment variables. `postgres` works as a host name because the Postgres Service has that name
(cluster DNS).

**`secret.example.yaml`**: a **template only** with `CHANGE_ME` values (`DB_USER`, `DB_PASSWORD`). The real Secret has the same name
`deadlinedash-db` but is created by Ansible from the encrypted vault file, so the password is never in git. `stringData`
lets us write plain text; Kubernetes stores it base64-encoded (encoded, not encrypted).

**`postgres.yaml`**: two objects.
- *Service* `postgres` with `clusterIP: None` (headless): the name `postgres` resolves straight to the pod.
- *StatefulSet* `postgres`, `replicas: 1`: like a Deployment but gives the pod a stable name (`postgres-0`) and its own disk.
  `POSTGRES_DB/USER/PASSWORD` come from the ConfigMap and the Secret. `PGDATA` is a sub-folder because a fresh Azure disk
  contains a `lost+found` folder that makes Postgres refuse to start. `readinessProbe` runs `pg_isready`.
  `volumeClaimTemplates` asks for a 1 GiB disk (PVC) from the cluster's default storage class, so data survives pod restarts.
  `resources` set requests/limits so the pod cannot use the whole node.

**`deployment.yaml`**: runs the app.
- `replicas: 2`: two identical pods (that is why the pod name in the footer changes).
- `strategy: RollingUpdate`, `maxUnavailable: 0`, `maxSurge: 1`: during an update, start one extra new pod first and only remove
  an old pod once a new one is Ready. Zero downtime.
- `selector.matchLabels` / `template.metadata.labels: app: deadlinedash`: how the Deployment and the Service find the pods.
- `image: ACR_LOGIN_SERVER/deadlinedash:IMAGE_TAG`: placeholder; the pipeline puts in the real name and SHA. Never `latest`.
- `envFrom` ConfigMap + Secret: all their keys become environment variables (`DB_HOST`, `DB_USER`, ...).
- `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70`: tells the JVM to use up to 70% of the container memory limit.
- `startupProbe` on `/actuator/health/liveness`: gives the JVM and Flyway up to 60 s (`periodSeconds: 3` x `failureThreshold: 20`)
  to start before other probes run.
- `readinessProbe` on `/actuator/health/readiness`: Kubernetes sends traffic only to pods that pass. A failing pod is taken out of the
  Service. This is what the rollback demo broke on purpose.
- `livenessProbe` on `/actuator/health/liveness`: if it fails repeatedly, Kubernetes **restarts** the container.
- `resources` requests (reserved) and limits (maximum) for CPU and memory.

**`service.yaml`**: `type: LoadBalancer`: AKS creates an Azure public IP and load balancer. `port: 8080` is what users hit (it was 80; changed in git and applied by the pipeline),
`targetPort: 8080` is the container port. `selector: app: deadlinedash` sends traffic to ready pods, round-robin.

---

## 4. Ansible

Ansible runs tasks described in YAML from my laptop (`hosts: local`, `ansible_connection=local`), using `kubectl`.

**`ansible/inventory.ini.example`**: list of machines to manage; here only `localhost`. The real `inventory.ini` is git-ignored.

**`ansible/playbook.yml`**: names the play "Deploy DeadlineDash to AKS", `gather_facts: false` (no need to inspect the machine),
and runs the role `deploy`.

**`roles/deploy/defaults/main.yml`**: default variables: `k8s_dir` (where manifests are), `k8s_namespace`, and empty
`acr_login_server` / `image_tag` (if set, Ansible also applies the Deployment itself).

**`roles/deploy/tasks/main.yml`** (tasks run in order):
1. *Create namespace*: `kubectl apply -f namespace.yaml`. `apply` is idempotent: running it again changes nothing.
2. *Create the database Secret from the Vault file*: builds a Secret manifest using `vault_db_user` and `vault_db_password` and pipes it to
   `kubectl apply -f -`. `no_log: true` hides the password from the output.
3. *Apply ConfigMap, Postgres and Service*: loop over the three files.
4. *Wait for Postgres*: `kubectl rollout status statefulset/postgres --timeout=300s`.
5. *Apply the Deployment* only `when` image variables are given (otherwise the pipeline creates it).
6. *Wait for the app rollout*, same condition.

**`group_vars/all/vault.yml`** holds the DB user and password and is encrypted with **Ansible Vault** (`ansible-vault encrypt`);
you need the vault passphrase (`--ask-vault-pass`) to run the playbook. Only `vault.yml.example` with placeholders is committed.

*Why Ansible plus the pipeline?* Ansible does one-time cluster setup (namespace, Secret, database). The pipeline does every
release. The Secret must not be in the pipeline or the repo.

---

## 5. Ten likely viva questions

1. **What is CI/CD and where is it in your project?**
   CI: every push is built and tested automatically (jobs `secret-scan` and `build-test`). CD: if those pass on `main`, the image is built, scanned,
   pushed and deployed to the cluster automatically (job `deploy`), with no manual steps.

2. **Why a multi-stage Dockerfile?**
   Stage 1 has Maven and the JDK to build; stage 2 has only the JRE and the JAR. The final image is smaller, faster to pull, and has fewer
   tools an attacker could use.

3. **Why tag images with the commit SHA and not `latest`?**
   The tag tells you exactly which code is running, makes rollbacks precise, and `latest` is ambiguous (Kubernetes may not even notice a change).
   The footer shows the same SHA, so I can prove which commit is live.

4. **How does the pipeline log in to Azure without a password?**
   GitHub OIDC. The job gets a short-lived signed token for my repo and branch. Azure has a federated credential that trusts exactly that
   issuer, audience and subject, and exchanges it for access. No secret to leak or rotate. We hit an error where the subject string had to match exactly.

5. **What is the difference between a Deployment and a StatefulSet?**
   A Deployment runs interchangeable pods (good for the stateless app). A StatefulSet gives each pod a stable name and its own disk (good for
   a database).

6. **Explain the three probes.**
   Startup: gives the app time to start. Readiness: decides if the pod gets traffic (fail = removed from the Service, not restarted). Liveness:
   decides if the container is stuck (fail = restarted). In the rollback demo, readiness failed, so the bad pod never got traffic.

7. **What does `maxUnavailable: 0, maxSurge: 1` mean?**
   During an update Kubernetes starts one extra pod first and only removes an old one after a new one is Ready, so capacity never drops. That is why
   the site stayed up during both the redeploy and the failed rollout.

8. **How does the rollback work, and what does it not cover?**
   If verify fails, `kubectl rollout undo` switches the Deployment back to the previous ReplicaSet (previous image). It does not undo database
   schema changes, so migrations must stay backward compatible.

9. **Where is the database password stored, and why is it safe?**
   In Ansible Vault (encrypted file, git-ignored) and then in a Kubernetes Secret created by Ansible. It is not in git, not in the image and not in
   the pipeline. gitleaks scans history to make sure nothing slips in. Limitation: Kubernetes Secrets are only base64-encoded unless encryption at rest
   is enabled.

10. **Why Azure, AKS Free tier and Postgres in the cluster? What would you change for production?**
    Cost: the Free tier has no control-plane charge and I have student credit. For production: managed database with backups, more than one node,
    Ingress with a domain and HTTPS, authentication, staging environment, monitoring/alerts and a manual approval before production.

**Bonus quick answers**
- *What does the urgency score do?* Multiplies weight by 100 and divides by days left plus one, so heavy and close tasks rank highest. Overdue first, done last.
- *Why a fixed `Clock` in tests?* So tests do not depend on today's date and always give the same result.
- *Why does the pod name change on refresh?* Two replicas behind one Service; the load balancer spreads requests.
- *Why did the page not open from my laptop?* The network's web filter blocked plain HTTP to a raw IP; the app was healthy.
- *How would you scale?* `kubectl scale deployment/deadlinedash --replicas=3` (needs more node capacity).
- *How do you clean up?* `az group delete` removes all Azure resources, so billing stops.
