# 05 Grafana Loki

## Dateien erstellen

In das Verzeichnis 05_loki wechseln und folgende Dateien erstellen

**loki-config.yaml**

```
apiVersion: v1
kind: Secret
metadata:
  name: loki-config
  labels:
    app.kubernetes.io/component: loki
    kustomize.toolkit.fluxcd.io/substitute: disabled
  annotations:
    kustomize.toolkit.fluxcd.io/substitute: disabled
stringData:
  config.yaml: |-
    auth_enabled: false

    # -- Check https://grafana.com/docs/loki/latest/configuration/#server for more info on the server configuration.
    server:
      http_listen_port: 3100
      grpc_listen_port: 9095
      http_server_read_timeout: 300s
      http_server_write_timeout: 300s

    memberlist:
      abort_if_cluster_join_fails: false
      bind_port: 7946
      max_join_backoff: 1m
      max_join_retries: 10
      min_join_backoff: 1s
      join_members:
      - loki-memberlist.monitoring.svc.cluster.local:7946

    schema_config:
      configs:
      - from: "2023-10-12"
        store: tsdb
        object_store: s3
        schema: v12
        index:
          prefix: index_
          period: 24h

    # -- Limits config
    limits_config:
      enforce_metric_name: false
      ingestion_rate_mb: 50
      ingestion_burst_size_mb: 75
      max_global_streams_per_user: 15000
      reject_old_samples: true
      reject_old_samples_max_age: 168h
      max_cache_freshness_per_query: 10m
      split_queries_by_interval: 15m
      per_stream_rate_limit: 5MB
      per_stream_rate_limit_burst: 20MB
      max_query_lookback: 720h
      max_query_length: 72h
      retention_period: 744h
      query_timeout: 5m

    # -- Additional storage config
    storage_config:
      hedging:
        at: "250ms"
        max_per_second: 20
        up_to: 3
      tsdb_shipper:
        active_index_directory: /var/loki/data/tsdb-index
        cache_location: /var/loki/data/tsdb-cache
        cache_ttl: 24h         # Can be increased for faster performance over longer query periods, uses more disk space
        shared_store: s3

      # --  Optional compactor configuration
    compactor:
      working_directory: /var/loki/data/retention
      shared_store: s3
      compaction_interval: 5m
      retention_enabled: true
      retention_delete_delay: 2h
      retention_delete_worker_count: 150

    ingester:
      autoforget_unhealthy: true
      chunk_encoding: snappy
      wal:
        flush_on_shutdown: true

    # -- Check https://grafana.com/docs/loki/latest/configuration/#common_config for more info on how to provide a common configuration
    common:
      path_prefix: /var/loki
      replication_factor: 1
      compactor_address: loki-backend

      storage:
        s3:
          endpoint: ${S3_ENDPOINT}
          region: eu-frankfurt-1
          bucketnames: ${S3_BUCKET}
          access_key_id: ${S3_ACCESSKEY}
          secret_access_key: ${S3_SECRETKEY}
          insecure: true
          sse_encryption: false
          http_config:
            idle_conn_timeout: 90s
            response_header_timeout: 0s
            insecure_skip_verify: false
          s3forcepathstyle: true

      ring:
        kvstore:
          store: memberlist

    querier:
      max_concurrent: 16
      engine:
        timeout: 5m0s

    query_scheduler:
      max_outstanding_requests_per_tenant: 32768

    query_range:
      parallelise_shardable_queries: true
      align_queries_with_step: true
      max_retries: 5
      cache_results: true
      results_cache:
        cache:
          embedded_cache:
            enabled: true

    ruler:
      storage:
        s3:
          bucketnames: ${S3_BUCKET}

    analytics:
      reporting_enabled: false
```

**loki-values.yaml**

```
loki:
  # Configures the readiness probe for all of the Loki pods
  readinessProbe:
    httpGet:
      path: /ready
      port: http-metrics
    initialDelaySeconds: 30
    timeoutSeconds: 1
  image:
    # -- The Docker registry
    registry: docker.io
    # -- Docker image repository
    repository: grafana/loki
    # -- Overrides the image tag whose default is the chart's appVersion
    # TODO: needed for 3rd target backend functionality
    # revert to null or latest once this behavior is relased
    tag: 2.9.2
    # -- Overrides the image tag with an image digest
    digest: null
    # -- Docker image pull policy
    pullPolicy: IfNotPresent
  # -- Common annotations for all deployments/StatefulSets
  annotations: {}
  # -- Common annotations for all pods
  podAnnotations: {}
  # -- Common labels for all pods
  podLabels: {}
  # -- The number of old ReplicaSets to retain to allow rollback
  revisionHistoryLimit: 10
  # -- The SecurityContext for Loki pods
  podSecurityContext:
    fsGroup: 10001
    runAsGroup: 10001
    runAsNonRoot: true
    runAsUser: 10001
  # -- The SecurityContext for Loki containers
  containerSecurityContext:
    readOnlyRootFilesystem: true
    capabilities:
      drop:
        - ALL
    allowPrivilegeEscalation: false
  # -- Should enableServiceLinks be enabled. Default to enable
  enableServiceLinks: true
  # -- Specify an existing secret containing loki configuration. If non-empty, overrides `loki.config`
  existingSecretForConfig: "loki-config"
  configStorageType: ConfigMap
  # -- Name of the Secret or ConfigMap that contains the configuration (used for naming even if config is internal).
  externalConfigSecretName: ""
  
serviceAccount:
  # -- Specifies whether a ServiceAccount should be created
  create: true
  # -- Set this toggle to false to opt out of automounting API credentials for the service account
  automountServiceAccountToken: true
# RBAC configuration
rbac:
  # -- If pspEnabled true, a PodSecurityPolicy is created for K8s that use psp.
  pspEnabled: false
  # -- For OpenShift set pspEnabled to 'false' and sccEnabled to 'true' to use the SecurityContextConstraints.
  sccEnabled: false
# -- Section for configuring optional Helm test
test:
  enabled: false
  
# Monitoring section determines which monitoring features to enable
monitoring:
  # Dashboards for monitoring Loki
  dashboards:
    # -- If enabled, create configmap with dashboards for monitoring Loki
    enabled: true

  # Recording rules for monitoring Loki, required for some dashboards
  rules:
    # -- If enabled, create PrometheusRule resource with Loki recording rules
    enabled: true
    # -- Include alerting rules
    alerting: true
    
  # ServiceMonitor configuration
  serviceMonitor:
    # -- If enabled, ServiceMonitor resources for Prometheus Operator are created
    enabled: true
  
  selfMonitoring:
    enabled: false
    grafanaAgent:
      installOperator: false
  
  # The Loki canary pushes logs to and queries from this loki installation to test
  # that it's working correctly
  lokiCanary:
    enabled: false
    # -- The name of the label to look for at loki when doing the checks.

# Configuration for the write pod(s)
write:
  # -- Number of replicas for the write
  replicas: 0

# Configuration for the read pod(s)
read:
  # -- Number of replicas for the read
  replicas: 0
  
# Configuration for the backend pod(s)
backend:
  # -- Number of replicas for the backend
  replicas: 0
  
# Configuration for the single binary node(s)
singleBinary:
  # -- Number of replicas for the single binary
  replicas: 1
  targetModule: "all"
  # -- Labels for single binary service
  extraArgs: 
  - -config.expand-env=true
  # -- Environment variables to add to the write pods
  extraEnv: 
  - name: S3_ACCESSKEY
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: S3_ACCESS_KEY_ID
  - name: S3_SECRETKEY
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: S3_SECRET_ACCESS_KEY
  - name: S3_ENDPOINT
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: ENDPOINT
  - name: S3_BUCKET
    value: lokilogs
  resources: {}
  # -- Grace period to allow the single binary to shutdown before it is killed
  terminationGracePeriodSeconds: 30
  persistence:
    # -- Enable StatefulSetAutoDeletePVC feature
    enableStatefulSetAutoDeletePVC: true
    # -- Enable persistent disk
    enabled: true
    # -- Size of persistent disk
    size: 5Gi
    storageClass: null
    # -- Selector for persistent disk
    selector: null

ingress:
  enabled: false

memberlist:
  service:
    publishNotReadyAddresses: true
# Configuration for the gateway
gateway:
  # -- Specifies whether the gateway should be enabled
  enabled: false
```

## Installation loki

Die ConfigMap anlegen

```
kubectl apply -f loki-config -n loki
```

Loki mit Helm installieren

```
helm -n loki upgrade --install loki grafana/loki -f loki-values.yaml
```

## Prüfen

Ausgabe der Helm-Installation

```
Release "loki" does not exist. Installing it now.
NAME: loki
LAST DEPLOYED: Sat Nov 11 16:12:55 2023
NAMESPACE: loki
STATUS: deployed
REVISION: 1
NOTES:
***********************************************************************
 Welcome to Grafana Loki
 Chart version: 5.36.3
 Loki version: 2.9.2
***********************************************************************

Installed components:
* loki
```

Prüfen ob die Pods laufen
```
kubectl get pods -n loki

NAME     READY   STATUS    RESTARTS   AGE
loki-0   1/1     Running   0          59s
```

## Zugriff API UI
Für Debug und Anzeige aktueller config

```
kubectl port-forward svc/loki-write 3100:3100 -n loki
```

Dann folgende URL aufrufen:

[buildinfo](http://localhost:3100/loki/api/v1/status/buildinfo)
[installierte services](http://localhost:3100/services)
[config](http://localhost:3100/config)
[metrics](http://localhost:3100/metrics)
[ring](http://localhost:3100/ring)
[Reload Config](http://localhost:3100/~/reload)
[Loglevel](http://localhost:3100/log_level)

Für Backend
[Compactor-Ring](http://localhost:3100/compactor/ring)
[Ruler](http://localhost:3100/ruler/ring)

## Datasource in Grafana prüfen

```
kubectl port-forward svc/grafana 8081:80 -n grafana
```

Unter Home/Connection/datasources die Loki-datasource auswählen und auf Save and Test klicken. 

![Loki1](assets/loki1.png)

Dann sollte Meldung OK Sein

![Loki2](assets/loki2.png)

Jetzt auf Link Explore View in der Meldung klicken

![Loki3](assets/loki3.png)

Hier können die logdaten abgefragt werden. Dann zuerst auf Label Browser klicken

![Loki4](assets/loki4.png)

Hier werden die Labels die schon in loki vorhanden sind angezeigt. Einfach etwas auswählen und auf Show logs klicken. Es werden dann die entsprechenden Logs angezeigt

![Loki5](assets/loki5.png)

