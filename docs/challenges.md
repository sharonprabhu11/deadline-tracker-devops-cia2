# Challenges (real errors, for the report)

Raw log, newest last. Used later in `docs/report-notes.md`.

1. **Port 8080 already in use locally.** `docker compose up` failed: `failed to bind host port 0.0.0.0:8080/tcp: address already in use`. Cause: another process on the laptop. Fix: host port made configurable (`${APP_PORT:-8080}:8080`).
2. **Locale error running Ansible.** `ERROR: Ansible requires the locale encoding to be UTF-8; Detected ISO8859-1.` Cause: shell locale. Fix: `export LC_ALL=C.UTF-8`.
3. **Ansible reserved variable name.** Warning `Found variable using reserved name 'namespace'`. Fix: renamed to `k8s_namespace`.
4. **Azure for Students region and VM-size limits.** Allowed regions were restricted by policy (5 regions). Fix: checked `az vm list-skus`, chose `indiasouthcentral` and `Standard_B2s`.
5. **gitleaks-action failed on the first push.** Log: `fatal: ambiguous argument '<root>^..<head>': unknown revision`, then `Unexpected exit code [1]`. Cause: the action scans `<first commit>^..<last commit>` and a root commit has no parent. Fix: run the pinned gitleaks binary over the full history instead.
6. **Azure OIDC login rejected.** `AADSTS700213: No matching federated identity record found for presented assertion subject 'repo:sharonprabhu11@161618358/deadline-tracker-devops-cia2@1446489116:ref:refs/heads/main'`. Cause: GitHub sent an immutable-ID subject claim, but the federated credential held the plain `repo:owner/name:ref:...` subject. Fix: updated the credential's subject to the exact string in the log.
7. **Public IP blocked from the laptop.** `curl http://172.198.4.144/` returned HTTP 403 `Web Filter Violation FortiGuard ... Category Unrated`. Cause: the local/university network filters plain-HTTP requests to unrated raw IPs; the app was healthy (pods Ready, GitHub's verify step reached it, `kubectl get --raw .../proxy/api/info` worked). Fix: view the site from another network (phone hotspot) or give the IP a DNS name.

## Rollback demo (not an error, evidence)
- Commit `0137ca1` added a health indicator that reports DOWN, wired into the readiness group. The new pod stayed `0/1` (`Readiness probe failed: HTTP probe failed with statuscode: 503`) while the two old pods served traffic (`maxUnavailable: 0`).
- The pipeline's Verify step waited for the rollout, failed, and `kubectl rollout undo` restored image `e8c644f` (13:35:42 deploy, 13:40:46 undo).
- Lesson 1: Spring Boot refuses to start if a health group names a contributor that does not exist, so the demo indicator had to always exist and decide UP/DOWN from a property.
- Lesson 2: the test config keeps the switch off so `build-test` stays green and the deploy job really runs. Deploys only run on `main`, so a throwaway branch could not exercise the rollback.

## Port-change demo (finding)
- Changing the Service port in `k8s/service.yaml` would have done nothing in Azure, because the pipeline only ran `kubectl set image` and never applied manifests. Fix: a pipeline step now runs `kubectl apply -f k8s/service.yaml -f k8s/configmap.yaml`, and the verify step reads the Service's real port instead of assuming 80.
- Why not change the container port: probes, `containerPort` and `targetPort` would all have to change at once; during the rolling update the Service would send traffic to old pods on the wrong port.
