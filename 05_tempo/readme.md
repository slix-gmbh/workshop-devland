# Installation Loki

## Manuell

```sh
kubectl apply -f namespace.yaml
kubectl apply -f tempo-s3-credentials.yaml -n tempo
helm repo add grafana-community https://grafana-community.github.io/helm-charts
helm repo update
helm install --values values-tempo.yaml tempo grafana-community/tempo-distributed -n tempo
```

## Flux

Das Repository einbinden in 00_common/flux-system/observabilityworkshop