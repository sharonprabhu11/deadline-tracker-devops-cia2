# Challenges (real errors, for the report)

Raw log, newest last. Used later in `docs/report-notes.md`.

1. **Port 8080 already in use locally.** `docker compose up` failed: `failed to bind host port 0.0.0.0:8080/tcp: address already in use`. Cause: another process on the laptop. Fix: host port made configurable (`${APP_PORT:-8080}:8080`).
2. **Locale error running Ansible.** `ERROR: Ansible requires the locale encoding to be UTF-8; Detected ISO8859-1.` Cause: shell locale. Fix: `export LC_ALL=C.UTF-8`.
3. **Ansible reserved variable name.** Warning `Found variable using reserved name 'namespace'`. Fix: renamed to `k8s_namespace`.
4. **Azure for Students region and VM-size limits.** Allowed regions were restricted by policy (5 regions). Fix: checked `az vm list-skus`, chose `indiasouthcentral` and `Standard_B2s`.
5. **gitleaks-action failed on the first push.** Log: `fatal: ambiguous argument '<root>^..<head>': unknown revision`, then `Unexpected exit code [1]`. Cause: the action scans `<first commit>^..<last commit>` and a root commit has no parent. Fix: run the pinned gitleaks binary over the full history instead.
6. **Azure OIDC login rejected.** `AADSTS700213: No matching federated identity record found for presented assertion subject 'repo:sharonprabhu11@161618358/deadline-tracker-devops-cia2@1446489116:ref:refs/heads/main'`. Cause: GitHub sent an immutable-ID subject claim, but the federated credential held the plain `repo:owner/name:ref:...` subject. Fix: updated the credential's subject to the exact string in the log.
