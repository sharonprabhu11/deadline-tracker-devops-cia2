# Progress

Ticked only when really done and verified.

## Definition of done
- [ ] Git repo with Java source and `pom.xml`
- [ ] Unit tests passing in the pipeline
- [ ] `Dockerfile` and `.dockerignore`
- [ ] Pipeline file, with a green run on `main`
- [ ] Kubernetes manifests applied to a cloud cluster, pods Running
- [ ] Ansible playbook and inventory example
- [ ] App reachable in a browser, footer showing version, SHA and pod name
- [ ] Image visible in ECR
- [ ] Redeploy demo works
- [ ] `docs/evidence-checklist.md` filled in
- [ ] `docs/report-notes.md` filled in
- [ ] `docs/viva-notes.md` filled in

## Stages
- [x] Stage 1 steps 1-3: generated project, footer + /api/info + health probes, Dockerfile/compose (verified: `docker compose up`, /api/info returned SHA and container name)
- [ ] Stage 1 steps 4-5: features and tests
- [ ] Stage 2: k8s, Ansible, workflow, aws-setup.md
- [ ] Stage 3: cloud run
- [ ] Stage 4: report and viva docs
