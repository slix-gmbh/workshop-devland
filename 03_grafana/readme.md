# 03 Grafana

Secrets und Configmaps erstellen

```sh
kubectl apply -f namespace.yaml
kubectl apply -f grafana-credentials.yaml -n grafana
kubectl apply -f grafana-datasource.yaml -n grafana
```

Dann per helm Grafana installieren

```yaml
helm -n grafana upgrade --install grafana grafana/grafana -f values-grafana.yaml
```

## Prüfen

Ausgabe von Helm:

```yaml
Release "grafana" does not exist. Installing it now.
NAME: grafana
LAST DEPLOYED: Wed Nov  6 11:09:01 2024
NAMESPACE: grafana
STATUS: deployed
REVISION: 1
NOTES:
1. Get your 'admin' user password by running:

   kubectl get secret --namespace grafana grafana-credentials -o jsonpath="{.data.admin-password}" | base64 --decode ; echo


2. The Grafana server can be accessed via port 80 on the following DNS name from within your cluster:

   grafana.grafana.svc.cluster.local

   Get the Grafana URL to visit by running these commands in the same shell:
     export POD_NAME=$(kubectl get pods --namespace grafana -l "app.kubernetes.io/name=grafana,app.kubernetes.io/instance=grafana" -o jsonpath="{.items[0].metadata.name}")
     kubectl --namespace grafana port-forward $POD_NAME 3000

3. Login with the password from step 1 and the username: admin
```

Prüfen ob die Pods laufen

```sh
kubectl get pods -n grafana

NAME                       READY   STATUS    RESTARTS   AGE
grafana-5dc4f97996-ds2qk   6/6     Running   0          36s
```

## Grafana öffnen

### port-forward

Ein port-forward einrichten

```sh
kubectl port-forward svc/grafana 8081:80 -n grafana
```

Dann im Browser [Grafana](http://localhost:8081) öffnen

### Login

Wird kein Passwort per Secret gesetzt
dann das automatisch generierte Adminpasswort auslesen:

```sh
kubectl get secret -n grafana grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
```

Ansonsten die Credentials aus dem Secret verwenden

Unter Home/Connections/Datasources die Datasources prüfen

Unter Dashboards die ersten Dashboards zu Mimir und k8s prüfen und anschauen
