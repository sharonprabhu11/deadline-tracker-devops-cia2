# Evidence checklist (screenshots for the report)

Capture while the cluster is still up. Tick each box when the screenshot is saved. Use full-window shots
where the URL, date or commit SHA is visible. Hide nothing except secrets (there should be none on screen).

Current live values for reference: ACR `deadlinedash25500`, resource group `deadlinedash-rg`, cluster
`deadlinedash-aks`, region `indiasouthcentral`, namespace `deadlinedash`, public IP `172.198.4.144`.

| # | Screenshot | Where | What it proves |
| --- | --- | --- | --- |
| 1 | [ ] Repo home page | GitHub repo main page | Git repo with Java source, `pom.xml`, `Dockerfile`, `k8s/`, `ansible/`, `.github/` |
| 2 | [ ] Green pipeline run, all jobs | GitHub -> Actions -> a green run (e.g. commit `e8c644f` or `24652b8`), the run summary graph | Pipeline stages run in order and pass: secret-scan, build-test, deploy |
| 3 | [ ] Unit tests step expanded | Same run -> `build-test` -> "Run tests" | `Tests run: 24, Failures: 0`. Unit tests pass in the pipeline |
| 4 | [ ] Trivy and ACR push steps expanded | Same run -> `deploy` -> "Scan image with Trivy", "Push image to ACR" | Image scan and registry push ran |
| 5 | [ ] Verify step expanded | Same run -> `deploy` -> "Verify rollout and live commit SHA" | Pipeline checked that `/api/info` returned the new SHA |
| 6 | [ ] Image tags in ACR | Azure portal -> Container registries -> `deadlinedash25500` -> Repositories -> `deadlinedash` | Image stored in the registry and tagged by commit SHA (never `latest`) |
| 7 | [ ] Resource group contents | Portal -> Resource groups -> `deadlinedash-rg` | Cloud resources used: ACR, AKS, (node resource group), nothing unexpected |
| 8 | [ ] AKS cluster overview | Portal -> `deadlinedash-aks` -> Overview | Cluster, Free tier, region, Kubernetes version, node pool size |
| 9 | [ ] Nodes | Terminal: `kubectl get nodes -o wide` | 1 node Ready on `Standard_B2s` |
| 10 | [ ] Pods Running | Terminal: `kubectl -n deadlinedash get pods -o wide` | 2 app pods + postgres, all Running 1/1 |
| 11 | [ ] All objects | Terminal: `kubectl -n deadlinedash get all,pvc,configmap,secret` | Deployment, ReplicaSet, Services, StatefulSet, PVC, ConfigMap and Secret exist |
| 12 | [ ] Service and public IP | Terminal: `kubectl -n deadlinedash get svc` | LoadBalancer with EXTERNAL-IP |
| 13 | [ ] Browser, first refresh | Phone/other network: `http://172.198.4.144:8080/` (was port 80 before the port-change commit) | App reachable; footer shows version, commit SHA, pod name |
| 14 | [ ] Browser, second refresh | Same page after refreshing a few times | Pod name in footer is different: load balancing across 2 replicas |
| 15 | [ ] App with data | Browser: add a course and a task | Features work and data is stored in Postgres in the cluster |
| 16 | [ ] Redeploy: before | Browser footer showing the old SHA (`36dd233`) and old heading. Use the earlier screenshot if you took one, otherwise skip | Starting point of the demo |
| 17 | [ ] Redeploy: after | Browser showing heading "DeadlineDash: do this first" and SHA `e8c644f` or later | Change reached production with no manual steps |
| 18 | [ ] Rolling update | Terminal: `kubectl -n deadlinedash rollout history deploy/deadlinedash` and `kubectl -n deadlinedash get rs` | Old and new ReplicaSets; rollout revisions |
| 19 | [ ] Failed deploy run | GitHub -> Actions -> the red run for commit `0137ca1`, summary graph | Verify step failed as designed |
| 20 | [ ] Rollback step | Same run -> "Verify rollout..." log (timed out) and "Roll back on failed verification" log | Pipeline detected the failure and ran `kubectl rollout undo` |
| 21 | [ ] Readiness failures | Terminal: `kubectl -n deadlinedash get events --sort-by=.lastTimestamp \| grep -i readiness` (only works while events are fresh, about 1 hour) | Probe returned 503, so the bad pod never got traffic |
| 22 | [ ] Federated credential | Portal -> Entra ID -> App registrations -> `deadlinedash-github` -> Certificates & secrets -> Federated credentials | Passwordless OIDC login, no client secret exists |
| 23 | [ ] Secrets list | GitHub -> Settings -> Secrets and variables -> Actions | Only IDs are stored, no keys or passwords. Do not reveal values |
| 24 | [ ] Ansible run | Terminal: the `PLAY RECAP` from `ansible-playbook` (rerun it if the old terminal is gone; it is safe to repeat) | Ansible applied the manifests and created the Secret |
| 25 | [ ] Budget alert | Portal -> Cost Management -> Budgets | Cost control configured first |

Notes:
- Items 19 to 21 are time-sensitive. The failed run stays in GitHub for a long time, but Kubernetes events expire
  after about an hour. If events are gone, skip 21; the log in item 20 is enough.
- To redo the rollback demo for fresh screenshots, ask Claude: it needs a new bad commit on `main` and a revert.
- Final step after all screenshots: delete the resource group (last section of `docs/azure-setup.md`).
