# Installation Alloy

# Manuelle Installation

```sh
cd 01_alloy
kubectl apply -k .
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
helm install -n alloy alloy-ds grafana/alloy -f values-alloy-ds.yaml -n alloy
```

```sh

```