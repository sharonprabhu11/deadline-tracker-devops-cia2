# Progress

Ticked only when really done and verified.

## Definition of done
- [x] Git repo with Java source and `pom.xml`
- [x] Unit tests passing in the pipeline (build-test job green on 2026-11-05)
- [x] `Dockerfile` and `.dockerignore`
- [x] Pipeline file, with a green run on `main` (run for commit 36dd233)
- [x] Kubernetes manifests applied to a cloud cluster, pods Running (2 app pods + postgres-0 Running on AKS)
- [x] Ansible playbook and inventory example (playbook ran: failed=0)
- [x] App reachable in a browser, footer showing version, SHA and pod name  (user confirmed on phone; laptop network blocks the raw IP via FortiGuard. I verified `/api/info` through `kubectl get --raw`: SHA 24652b8, both pod names alternate)
- [x] Image visible in registry (Azure ACR: tags 36dd233, e8c644f, 0137ca1, 24652b8 listed by `az acr repository show-tags`; user still to screenshot)
- [x] Redeploy demo works  (heading change pushed as e8c644f; footer SHA went 36dd233 -> e8c644f, new ReplicaSet, 2 pods, no manual steps. Rollback proven: commit 0137ca1 failed readiness (503), pipeline ran `rollout undo` at 13:40:46, site stayed on e8c644f; reverted in 6b4a35f)
- [x] `docs/evidence-checklist.md` filled in (25 screenshots listed; the screenshots themselves are still to be taken by the user)
- [x] `docs/report-notes.md` filled in (notes, not the finished report; headings must be matched to the real assignment brief)
- [x] `docs/viva-notes.md` filled in

## Stages
- [x] Stage 1 steps 1-3: generated project, footer + /api/info + health probes, Dockerfile/compose (verified: `docker compose up`, /api/info returned SHA and container name)
- [x] Stage 1 steps 4-5: features and tests (24 tests pass via ./mvnw -B verify; add course/task verified against real Postgres in compose)
- [x] Stage 2: k8s, Ansible, workflow, azure-setup.md written; manifests dry-run OK, ansible syntax-check OK, workflow YAML parses (actionlint not available)
- [x] Stage 3: cloud run (all steps verified except browser screenshots, which the user captures)
- [x] Stage 4: evidence checklist, report notes, viva notes written

## Not done / needs the user
- Take the screenshots in `docs/evidence-checklist.md` (cannot be done from the terminal).
- Write the actual 5-8 page report from `docs/report-notes.md`; no assignment document was in `docs/`, so check the required headings.
- `actionlint` was never run on the workflow; its correctness is shown by real green runs only.
- No test of a pull-request run (PR path runs only secret-scan and build-test).
- Delete the Azure resource group after the screenshots and viva (`az group delete --name deadlinedash-rg`), then the Entra app `deadlinedash-github`. The cluster costs about 1 USD/day while it runs.
- Footer `buildTime` shows 1980 (reproducible Maven build timestamp); cosmetic and not shown on the page.
