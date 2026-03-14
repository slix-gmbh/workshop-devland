# Installation Loki

## Manuell

```sh
kubectl apply -f namespace.yaml
kubectl apply -f loki-config.yaml
kubectl apply -f loki-s3-credentials.yaml
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
helm install --values values-loki.yaml loki grafana/loki -n loki
```

## Flux

Das Repository einbinden in 00_common/flux-system/observabilityworkshop