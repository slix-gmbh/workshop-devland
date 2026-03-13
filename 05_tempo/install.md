# Installation Tempo

## Manulle Installation

```sh
kubectls apply -f values-tempo.yaml
kubectl apply -f tempo-s3-credentials.yaml
helm repo add grafana-community https://grafana-community.github.io/helm-charts
helm repo update
helm install tempo grafana-community/tempo-distributed -f values-tempo.yaml -n tempo
```
