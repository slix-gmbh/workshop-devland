# Installation Prometheus

## Manuell
```sh
kubectl apply -f namespace.yaml
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm install prometheus-stack prometheus-community/kube-prometheus-stack -n prometheus
```