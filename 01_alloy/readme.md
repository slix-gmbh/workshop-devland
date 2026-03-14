# Installation Alloy

# Manuelle Installation

```sh
kubectl apply -f namespace.yaml
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
helm install -n alloy alloy-ds grafana/alloy -f values-alloy-ds.yaml
```