# Progress

Ticked only when really done and verified.

## Definition of done
- [x] Git repo with Java source and `pom.xml`
- [x] Unit tests passing in the pipeline (build-test job green on 2026-11-05)
- [x] `Dockerfile` and `.dockerignore`
- [x] Pipeline file, with a green run on `main` (run for commit 36dd233)
- [x] Kubernetes manifests applied to a cloud cluster, pods Running (2 app pods + postgres-0 Running on AKS)
- [x] Ansible playbook and inventory example (playbook ran: failed=0)
- [ ] App reachable in a browser, footer showing version, SHA and pod name  (API verified via kubectl proxy: SHA 36dd233, pod name alternates; browser at http://172.198.4.144/ is blocked by a FortiGuard filter on the user's network, needs a different network)
- [x] Image visible in ECR (Azure: ACR tag 36dd233; user still to screenshot)
- [ ] Redeploy demo works
- [ ] `docs/evidence-checklist.md` filled in
- [ ] `docs/report-notes.md` filled in
- [ ] `docs/viva-notes.md` filled in

## Stages
- [x] Stage 1 steps 1-3: generated project, footer + /api/info + health probes, Dockerfile/compose (verified: `docker compose up`, /api/info returned SHA and container name)
- [x] Stage 1 steps 4-5: features and tests (24 tests pass via ./mvnw -B verify; add course/task verified against real Postgres in compose)
- [x] Stage 2: k8s, Ansible, workflow, azure-setup.md written; manifests dry-run OK, ansible syntax-check OK, workflow YAML parses (actionlint not available; nothing run in cloud yet)
- [ ] Stage 3: cloud run
- [ ] Stage 4: report and viva docs
