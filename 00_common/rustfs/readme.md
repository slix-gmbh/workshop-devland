# S3 Backend rustfs

Sehr leichtgewichtiges S3-Backend. Wird per helm installiert

## Manuell

```sh
kubectl apply -f namespace.yaml
helm repo add rustfs https://charts.rustfs.com
helm install my-rustfs rustfs/rustfs --version 0.0.80
```

Informationen zum Helmchart:
https://artifacthub.io/packages/helm/rustfs/rustfs

## Flux

Dazu einfach das helmrelease mit den konfigurierten Values ausrollen