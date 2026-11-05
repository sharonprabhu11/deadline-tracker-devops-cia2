# Azure setup checklist (do these by hand, in order)

Claude never runs these. Replace the values in the first block once and keep the same terminal open.
Tools needed on your laptop: Azure CLI (`az`), `kubectl`, `ansible`, `git`.

> Cost note: AKS Free tier has no control-plane charge. You pay for the one node VM, the Basic ACR,
> a 1 GiB disk and a public IP. Check current prices in the Azure pricing calculator before step 3.

## 1. Cost Management budget alert (FIRST)
Portal -> **Cost Management + Billing** -> **Budgets** -> **Add**: scope = your subscription,
amount = a small figure (e.g. 500 INR / 5 USD per month), alert at 50% and 90% to your email.
Do not continue until this exists.

## 2. Sign in and set variables
```bash
az login
az account show --query "{sub:id, tenant:tenantId}" -o table

export SUB=$(az account show --query id -o tsv)
export TENANT=$(az account show --query tenantId -o tsv)
export LOCATION=indiasouthcentral      # allowed by the student policy; Standard_B2s is unrestricted here
export RG=deadlinedash-rg
export ACR=deadlinedash$RANDOM          # must be globally unique, lowercase letters/digits only
export AKS=deadlinedash-aks
export GH_REPO=<your-github-username>/<your-repo-name>   # e.g. sharon/deadline-tracker-devops-cia2
echo $ACR   # note this name down
```

## 3. Resource group, ACR and AKS
VM size: `Standard_B2s` (2 vCPU, 4 GiB) was checked with `az vm list-skus` and has no restrictions in
`indiasouthcentral`. 4 GiB is the practical minimum for 2 app pods + Postgres + system pods.
Check quota first (needs at least 2 free vCPUs):
```bash
az vm list-usage --location $LOCATION -o table | grep -iE "Total Regional|Standard BS"
az provider show -n Microsoft.ContainerService --query registrationState -o tsv   # must say Registered
```
```bash
az group create --name $RG --location $LOCATION

az acr create --resource-group $RG --name $ACR --sku Basic

az aks create --resource-group $RG --name $AKS \
  --tier free --node-count 1 --node-vm-size Standard_B2s \
  --attach-acr $ACR --generate-ssh-keys
```
`--attach-acr` lets the cluster pull from ACR, so no image pull secret is needed.
This takes about 5-10 minutes.

## 4. Entra app registration for GitHub OIDC (no client secret)
```bash
export APP_ID=$(az ad app create --display-name deadlinedash-github --query appId -o tsv)
az ad sp create --id $APP_ID

# Trust workflows from the main branch of YOUR repo only
az ad app federated-credential create --id $APP_ID --parameters '{
  "name": "github-main",
  "issuer": "https://token.actions.githubusercontent.com",
  "subject": "repo:'"$GH_REPO"':ref:refs/heads/main",
  "audiences": ["api://AzureADTokenExchange"]
}'
```

## 5. Give that app only the roles the pipeline needs
```bash
ACR_ID=$(az acr show --name $ACR --query id -o tsv)
AKS_ID=$(az aks show --resource-group $RG --name $AKS --query id -o tsv)

az role assignment create --assignee $APP_ID --role AcrPush --scope $ACR_ID
az role assignment create --assignee $APP_ID --role "Azure Kubernetes Service Cluster Admin Role" --scope $AKS_ID
```
(The pipeline runs `az aks get-credentials --admin`, which needs the Cluster Admin role.)

## 6. GitHub secrets and variables
Repo -> **Settings -> Secrets and variables -> Actions**.

Secrets (tab *Secrets*):

| Name | Value |
| --- | --- |
| `AZURE_CLIENT_ID` | `echo $APP_ID` |
| `AZURE_TENANT_ID` | `echo $TENANT` |
| `AZURE_SUBSCRIPTION_ID` | `echo $SUB` |

Variables (tab *Variables*):

| Name | Value |
| --- | --- |
| `ACR_NAME` | `echo $ACR` (name only, no `.azurecr.io`) |
| `AKS_RESOURCE_GROUP` | `deadlinedash-rg` |
| `AKS_CLUSTER` | `deadlinedash-aks` |

No password or key is stored anywhere.

## 7. Create the database Secret with Ansible Vault
```bash
az aks get-credentials --resource-group $RG --name $AKS --admin --overwrite-existing
kubectl get nodes                       # should show 1 node Ready

cd ansible
cp inventory.ini.example inventory.ini
cp group_vars/all/vault.yml.example group_vars/all/vault.yml
# edit vault.yml: choose a database user and a strong password, then encrypt it
ansible-vault encrypt group_vars/all/vault.yml
ansible-playbook -i inventory.ini playbook.yml --ask-vault-pass
```
This creates the namespace, the Secret, the ConfigMap, Postgres and the Service, and waits for
Postgres. (`inventory.ini` and `vault.yml` are git-ignored.)

## 8. Trigger the first deploy
```bash
git push origin main
```
Watch **Actions** in GitHub. The first run creates the Deployment with the real image. Then:
```bash
kubectl -n deadlinedash get pods
kubectl -n deadlinedash get service deadlinedash   # EXTERNAL-IP may take a minute; open http://<EXTERNAL-IP>/
```
Tell Claude when this is finished, and Stage 3 starts.

## 9. LAST: delete everything after the demo
```bash
az group delete --name $RG --yes --no-wait
az ad app delete --id $APP_ID
```
Then confirm in the portal that the resource group is gone, so billing stops.
