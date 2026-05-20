# S3 Backend rustfs

Sehr leichtgewichtiges S3-Backend. Wird per helm installiert

## Manuell

```sh
kubectl apply -f namespace.yaml
kubectl apply -f rustfs-admin-credentials.yaml
helm repo add rustfs https://charts.rustfs.com
helm install my-rustfs rustfs/rustfs --version 0.0.80 -f values-rustfs.yaml
#Nach dem rustfs läuft
kubectl apply -f provisioning/provision-bucketjob.yaml
```

Informationen zum Helmchart:
https://artifacthub.io/packages/helm/rustfs/rustfs