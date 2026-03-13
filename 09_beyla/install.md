# Installation Beyla

## Manulle Installation

```sh
kubectl apply -f namespace.yaml
kubectl apply -f beyla-config.yaml
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
helm install beyla grafana/beyla -f values-beyla.yaml -n beyla
```