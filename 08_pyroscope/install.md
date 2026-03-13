# Installation Pyroscope

## Manulle Installation

```sh
kubectls apply -f values-tempo.yaml
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
helm install pyroscope grafana/pyroscope -f values-pyroscope.yaml -n pyroscope
```