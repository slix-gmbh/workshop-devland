# 05 Grafana Tempo

## Dateien erstellen

In das Verzeichnis 05_tempo wechseln und folgende Dateien erstellen:

**tempo-values.yaml**

```
global:
  image:
    # -- Overrides the Docker registry globally for all images, excluding enterprise.
    registry: docker.io
    pullSecrets: []
  clusterDomain: 'cluster.local'
  dnsService: 'kube-dns'
  dnsNamespace: 'kube-system'
fullnameOverride: ''


useExternalConfig: false

configStorageType: ConfigMap

externalConfigSecretName: '{{ include "tempo.resourceName" (dict "ctx" . "component" "config") }}'

externalRuntimeConfigName: '{{ include "tempo.resourceName" (dict "ctx" . "component" "runtime") }}'

externalConfigVersion: '0'

# -- If true, Tempo will report anonymous usage data about the shape of a deployment to Grafana Labs
reportingEnabled: false

tempo:
  image:
    tag: 2.6.1
    pullPolicy: IfNotPresent

  structuredConfig: {}
  memberlist:
    appProtocol: null

serviceAccount:
  create: true

rbac:
  create: true
  pspEnabled: false

# Configuration for the ingester
ingester:
  replicas: 1
  
  extraArgs: 
  - -config.expand-env=true
  
  extraEnv: 
  - name: ENDPOINT
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: ENDPOINT
  - name: SECRET_KEY
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: S3_SECRET_ACCESS_KEY
  - name: ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: S3_ACCESS_KEY_ID
  
  terminationGracePeriodSeconds: 300
  
  persistence:
    enabled: false
  config:
    # -- Number of copies of spans to store in the ingester ring
    replication_factor: 1
    # -- Amount of time a trace must be idle before flushing it to the wal.
    trace_idle_period: null
    # -- How often to sweep all tenants and move traces from live -> wal -> completed blocks.
    flush_check_period: null
    # -- Maximum size of a block before cutting it
    max_block_bytes: null
    # -- Maximum length of time before cutting a block
    max_block_duration: null
    # -- Duration to keep blocks in the ingester after they have been flushed
    complete_block_timeout: null
    # -- Flush all traces to backend when ingester is stopped
    flush_all_on_shutdown: false

# Configuration for the metrics-generator
metricsGenerator:
  enabled: true
  replicas: 1
  
  extraArgs: 
  - -config.expand-env=true
  
  extraEnv: 
  - name: ENDPOINT
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: ENDPOINT
  - name: SECRET_KEY
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: S3_SECRET_ACCESS_KEY
  - name: ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: S3_ACCESS_KEY_ID
  
  terminationGracePeriodSeconds: 300
  ports:
    - name: grpc
      port: 9095
      service: true
    - name: http-memberlist
      port: 7946
      service: false
    - name: http-metrics
      port: 3100
      service: true
  config:
    registry:
      collection_interval: 15s
      external_labels: {}
      stale_duration: 15m
    processor:
      # Span metrics create metrics based on span type, duration, name and service.
      span_metrics:
        # Configure extra dimensions to add as metric labels.
        dimensions:
          - http.method
          - http.target
          - http.status_code
          - service.version
        #  dimensions: []
        histogram_buckets: [0.002, 0.004, 0.008, 0.016, 0.032, 0.064, 0.128, 0.256, 0.512, 1.02, 2.05, 4.10]
      # Service graph metrics create node and edge metrics for determinng service interactions.
      service_graphs:
        # Configure extra dimensions to add as metric labels.
        dimensions:
          - http.method
          - http.target
          - http.status_code
          - service.version
      #  dimensions: []
        histogram_buckets: [0.1, 0.2, 0.4, 0.8, 1.6, 3.2, 6.4, 12.8]
        max_items: 10000
        wait: 10s
        workers: 10
    storage:
      path: /var/tempo/wal
      wal:
      remote_write_flush_deadline: 1m
      # -- A list of remote write endpoints.
      # -- https://prometheus.io/docs/prometheus/latest/configuration/configuration/#remote_write
      remote_write:
      - url: http://mimir-gateway.mimir.svc.cluster.local/api/v1/push
        send_exemplars: true
        #basic_auth:
        #  username: 1282013
        #  password: 
    traces_storage:
      path: /var/tempo/wal/traces
    metrics_ingestion_time_range_slack: 30s
  
# Configuration for the distributor
distributor:
  replicas: 1
  
  extraArgs: 
  - -config.expand-env=true
  extraEnv: 
  - name: ENDPOINT
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: ENDPOINT
  - name: SECRET_KEY
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: S3_SECRET_ACCESS_KEY
  - name: ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: S3_ACCESS_KEY_ID
  
  terminationGracePeriodSeconds: 30
  
  config:
    # -- Enable to log every received span to help debug ingestion or calculate span error distributions using the logs
    log_received_spans:
      enabled: false
      include_all_attributes: false
      filter_by_status_error: false
    # -- Disables write extension with inactive ingesters
    extend_writes: null
  
# Configuration for the compactor
compactor:
  # -- Number of replicas for the compactor
  replicas: 1
  
  extraArgs: 
  - -config.expand-env=true
  
  extraEnv: 
  - name: ENDPOINT
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: ENDPOINT
  - name: SECRET_KEY
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: S3_SECRET_ACCESS_KEY
  - name: ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: S3_ACCESS_KEY_ID
  config:
    compaction:
      # -- Duration to keep blocks
      block_retention: 48h
      # Duration to keep blocks that have been compacted elsewhere
      compacted_block_retention: 1h
      # -- Blocks in this time window will be compacted together
      compaction_window: 1h
      # -- Amount of data to buffer from input blocks
      v2_in_buffer_bytes: 5242880
      # -- Flush data to backend when buffer is this large
      v2_out_buffer_bytes: 20971520
      # -- Maximum number of traces in a compacted block. WARNING: Deprecated. Use max_block_bytes instead.
      max_compaction_objects: 6000000
      # -- Maximum size of a compacted block in bytes
      max_block_bytes: 107374182400
      # -- Number of tenants to process in parallel during retention
      retention_concurrency: 10
      # -- Number of traces to buffer in memory during compaction
      v2_prefetch_traces_count: 1000
      # -- The maximum amount of time to spend compacting a single tenant before moving to the next
      max_time_per_tenant: 5m
      # -- The time between compaction cycles
      compaction_cycle: 30s
  
# Configuration for the querier
querier:
  replicas: 1

  extraArgs: 
  - -config.expand-env=true
  
  extraEnv: 
  - name: ENDPOINT
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: ENDPOINT
  - name: SECRET_KEY
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: S3_SECRET_ACCESS_KEY
  - name: ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: S3_ACCESS_KEY_ID
  
  terminationGracePeriodSeconds: 30
  
  config:
    frontend_worker:
      frontend_address: tempo-query-frontend-discovery.tempo.svc.cluster.local:9095
      # -- grpc client configuration
      grpc_client_config:
        max_send_msg_size: 70000000
        max_recv_msg_size: 70000000
    trace_by_id:
      # -- Timeout for trace lookup requests
      query_timeout: 10s
    search:
      # -- Timeout for search requests
      query_timeout: 30s
      # -- If search_external_endpoints is set then the querier will primarily act as a proxy for whatever serverless backend you have configured. This setting allows the operator to have the querier prefer itself for a configurable number of subqueries.
      prefer_self: 10
      # -- If set to a non-zero value a second request will be issued at the provided duration. Recommended to be set to p99 of external search requests to reduce long tail latency.
      external_hedge_requests_at: 8s
      # -- The maximum number of requests to execute when hedging. Requires hedge_requests_at to be set.
      external_hedge_requests_up_to: 2
      # -- A list of external endpoints that the querier will use to offload backend search requests
      external_endpoints: []
      # -- The serverless backend to use. The default value of "" omits
      # -- credentials when querying the external backend.
      external_backend: ""
      # -- Google Cloud Run configuration. Will be used only if the value of
      # -- external_backend is "google_cloud_run".
      google_cloud_run: {}
    # -- This value controls the overall number of simultaneous subqueries that the querier will service at once. It does not distinguish between the types of queries.
    max_concurrent_queries: 20

# Configuration for the query-frontend
queryFrontend:
  query:
    # -- Required for grafana version <7.5 for compatibility with jaeger-ui. Doesn't work on ARM arch
    enabled: false
  replicas: 1
  
  config:
    # -- Number of times to retry a request sent to a querier
    max_retries: 2
    search:
      # -- The number of concurrent jobs to execute when searching the backend
      concurrent_jobs: 1000
      # -- The target number of bytes for each job to handle when performing a backend search
      target_bytes_per_job: 104857600
    # -- Trace by ID lookup configuration
    trace_by_id:
      # -- The number of shards to split a trace by id query into.
      query_shards: 50
      # -- If set to a non-zero value, a second request will be issued at the provided duration. Recommended to be set to p99 of search requests to reduce long-tail latency.
      hedge_requests_at: 2s
      # -- The maximum number of requests to execute when hedging. Requires hedge_requests_at to be set. Must be greater than 0.
      hedge_requests_up_to: 2
  
  ingress:
    enabled: false
  
  extraArgs: 
  - -config.expand-env=true
  
  extraEnv: 
  - name: ENDPOINT
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: ENDPOINT
  - name: SECRET_KEY
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: S3_SECRET_ACCESS_KEY
  - name: ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: minio-s3-credentials
        key: S3_ACCESS_KEY_ID
  
  terminationGracePeriodSeconds: 30

multitenancyEnabled: false

traces:
  jaeger:
    grpc:
      enabled: false
    thriftBinary:
      enabled: false
    thriftCompact:
      enabled: false
    thriftHttp:
      enabled: false
  zipkin:
    enabled: false
  otlp:
    http:
      enabled: true
    grpc:
      enabled: true
  opencensus:
    enabled: false
  # -- Enable Tempo to ingest traces from Kafka. Reference: https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/kafkareceiver
  kafka: {}

# -- Memberlist configuration. Please refer to https://grafana.com/docs/tempo/latest/configuration/#memberlist
memberlist:
  node_name: ""
  randomize_node_name: true
  stream_timeout: "10s"
  retransmit_factor: 2
  pull_push_interval: "30s"
  gossip_interval: "1s"
  gossip_nodes: 2
  gossip_to_dead_nodes_time: "30s"
  min_join_backoff: "1s"
  max_join_backoff: "1m"
  max_join_retries: 10
  abort_if_cluster_join_fails: false
  rejoin_interval: "0s"
  left_ingesters_timeout: "5m"
  leave_timeout: "5s"
  bind_addr: []
  bind_port: 7946
  packet_dial_timeout: "5s"
  packet_write_timeout: "5s"

server:
  logLevel: debug
  grpc_server_max_recv_msg_size: 70000000
  grpc_server_max_send_msg_size: 70000000
  http_server_read_timeout: 5m
  http_server_write_timeout: 5m

storage:
  trace:
    # Settings for the block storage backend and buckets.
    block:
      # -- The supported block versions are specified here https://grafana.com/docs/tempo/latest/configuration/parquet/
      version: null
    # -- The supported storage backends are gcs, s3 and azure, as specified in https://grafana.com/docs/tempo/latest/configuration/#storage
    backend: s3
    s3:
      access_key: ${ACCESS_KEY}
      secret_key: ${SECRET_KEY}
      bucket: tempotraces
      endpoint: ${ENDPOINT}
      insecure: true
    pool:
      # -- Total number of workers pulling jobs from the queue
      max_workers: 400
      # -- Length of job queue. imporatant for querier as it queues a job for every block it has to search
      queue_depth: 20000
    wal:
      path: /var/tempo/wal             # where to store the the wal locally
    local:
      path: /var/tempo/blocks
  
# Global overrides
global_overrides:
  per_tenant_override_config: /runtime-config/overrides.yaml
  metrics_generator_processors: ['service-graphs', 'span-metrics']

# Per tenants overrides
overrides: {}

memcached:
  enabled: true
  replicas: 1

memcachedExporter:
  enabled: false
  
metaMonitoring:
  serviceMonitor:
    enabled: true

prometheusRule:
  enabled: true
  groups:
  - "name": "tempo_rules"
    "rules":
    - "expr": "histogram_quantile(0.99, sum(rate(tempo_request_duration_seconds_bucket[1m])) by (le, cluster, namespace, job, route))"
      "record": "cluster_namespace_job_route:tempo_request_duration_seconds:99quantile"
    - "expr": "histogram_quantile(0.50, sum(rate(tempo_request_duration_seconds_bucket[1m])) by (le, cluster, namespace, job, route))"
      "record": "cluster_namespace_job_route:tempo_request_duration_seconds:50quantile"
    - "expr": "sum(rate(tempo_request_duration_seconds_sum[1m])) by (cluster, namespace, job, route) / sum(rate(tempo_request_duration_seconds_count[1m])) by (cluster, namespace, job, route)"
      "record": "cluster_namespace_job_route:tempo_request_duration_seconds:avg"
    - "expr": "sum(rate(tempo_request_duration_seconds_bucket[1m])) by (le, cluster, namespace, job, route)"
      "record": "cluster_namespace_job_route:tempo_request_duration_seconds_bucket:sum_rate"
    - "expr": "sum(rate(tempo_request_duration_seconds_sum[1m])) by (cluster, namespace, job, route)"
      "record": "cluster_namespace_job_route:tempo_request_duration_seconds_sum:sum_rate"
    - "expr": "sum(rate(tempo_request_duration_seconds_count[1m])) by (cluster, namespace, job, route)"
      "record": "cluster_namespace_job_route:tempo_request_duration_seconds_count:sum_rate"
  - "name": "tempo_alerts"
    "rules":
    - "alert": "TempoRequestLatency"
      "annotations":
        "message": |
          {{ $labels.job }} {{ $labels.route }} is experiencing {{ printf "%.2f" $value }}s 99th percentile latency.
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoRequestLatency"
      "expr": |
        cluster_namespace_job_route:tempo_request_duration_seconds:99quantile{route!~"metrics|/frontend.Frontend/Process|debug_pprof"} > 3
      "for": "15m"
      "labels":
        "severity": "critical"
    - "alert": "TempoCompactorUnhealthy"
      "annotations":
        "message": "There are {{ printf \"%f\" $value }} unhealthy compactor(s)."
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoCompactorUnhealthy"
      "expr": |
        max by (cluster, namespace) (tempo_ring_members{state="Unhealthy", name="compactor", namespace=~".*"}) > 0
      "for": "15m"
      "labels":
        "severity": "critical"
    - "alert": "TempoDistributorUnhealthy"
      "annotations":
        "message": "There are {{ printf \"%f\" $value }} unhealthy distributor(s)."
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoDistributorUnhealthy"
      "expr": |
        max by (cluster, namespace) (tempo_ring_members{state="Unhealthy", name="distributor", namespace=~".*"}) > 0
      "for": "15m"
      "labels":
        "severity": "warning"
    - "alert": "TempoCompactionsFailing"
      "annotations":
        "message": "Greater than 2 compactions have failed in the past hour."
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoCompactionsFailing"
      "expr": |
        sum by (cluster, namespace) (increase(tempodb_compaction_errors_total{}[1h])) > 2 and
        sum by (cluster, namespace) (increase(tempodb_compaction_errors_total{}[5m])) > 0
      "for": "5m"
      "labels":
        "severity": "critical"
    - "alert": "TempoIngesterFlushesUnhealthy"
      "annotations":
        "message": "Greater than 2 flush retries have occurred in the past hour."
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoIngesterFlushesFailing"
      "expr": |
        sum by (cluster, namespace) (increase(tempo_ingester_failed_flushes_total{}[1h])) > 2 and
        sum by (cluster, namespace) (increase(tempo_ingester_failed_flushes_total{}[5m])) > 0
      "for": "5m"
      "labels":
        "severity": "warning"
    - "alert": "TempoIngesterFlushesFailing"
      "annotations":
        "message": "Greater than 2 flush retries have failed in the past hour."
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoIngesterFlushesFailing"
      "expr": |
        sum by (cluster, namespace) (increase(tempo_ingester_flush_failed_retries_total{}[1h])) > 2 and
        sum by (cluster, namespace) (increase(tempo_ingester_flush_failed_retries_total{}[5m])) > 0
      "for": "5m"
      "labels":
        "severity": "critical"
    - "alert": "TempoPollsFailing"
      "annotations":
        "message": "Greater than 2 polls have failed in the past hour."
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoPollsFailing"
      "expr": |
        sum by (cluster, namespace) (increase(tempodb_blocklist_poll_errors_total{}[1h])) > 2 and
        sum by (cluster, namespace) (increase(tempodb_blocklist_poll_errors_total{}[5m])) > 0
      "labels":
        "severity": "critical"
    - "alert": "TempoTenantIndexFailures"
      "annotations":
        "message": "Greater than 2 tenant index failures in the past hour."
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoTenantIndexFailures"
      "expr": |
        sum by (cluster, namespace) (increase(tempodb_blocklist_tenant_index_errors_total{}[1h])) > 2 and
        sum by (cluster, namespace) (increase(tempodb_blocklist_tenant_index_errors_total{}[5m])) > 0
      "labels":
        "severity": "critical"
    - "alert": "TempoNoTenantIndexBuilders"
      "annotations":
        "message": "No tenant index builders for tenant {{ $labels.tenant }}. Tenant index will quickly become stale."
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoNoTenantIndexBuilders"
      "expr": |
        sum by (cluster, namespace, tenant) (tempodb_blocklist_tenant_index_builder{}) == 0 and
        max by (cluster, namespace) (tempodb_blocklist_length{}) > 0
      "for": "5m"
      "labels":
        "severity": "critical"
    - "alert": "TempoTenantIndexTooOld"
      "annotations":
        "message": "Tenant index age is 600 seconds old for tenant {{ $labels.tenant }}."
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoTenantIndexTooOld"
      "expr": |
        max by (cluster, namespace, tenant) (tempodb_blocklist_tenant_index_age_seconds{}) > 600
      "for": "5m"
      "labels":
        "severity": "critical"
    - "alert": "TempoBlockListRisingQuickly"
      "annotations":
        "message": "Tempo block list length is up 40 percent over the last 7 days.  Consider scaling compactors."
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoBlockListRisingQuickly"
      "expr": |
        avg(tempodb_blocklist_length{namespace=".*"}) / avg(tempodb_blocklist_length{namespace=".*", job=~"$namespace/$component"} offset 7d) > 1.4
      "for": "15m"
      "labels":
        "severity": "critical"
    - "alert": "TempoBadOverrides"
      "annotations":
        "message": "{{ $labels.job }} failed to reload overrides."
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoBadOverrides"
      "expr": |
        sum(tempo_runtime_config_last_reload_successful{namespace=~".*"} == 0) by (cluster, namespace, job)
      "for": "15m"
      "labels":
        "severity": "warning"
    - "alert": "TempoUserConfigurableOverridesReloadFailing"
      "annotations":
        "message": "Greater than 5 user-configurable overides reloads failed in the past hour."
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoTenantIndexFailures"
      "expr": |
        sum by (cluster, namespace) (increase(tempo_overrides_user_configurable_overrides_reload_failed_total{}[1h])) > 5 and
        sum by (cluster, namespace) (increase(tempo_overrides_user_configurable_overrides_reload_failed_total{}[5m])) > 0
      "labels":
        "severity": "critical"
    - "alert": "TempoProvisioningTooManyWrites"
      "annotations":
        "message": "Ingesters in {{ $labels.cluster }}/{{ $labels.namespace }} are receiving more data/second than desired, add more ingesters."
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoProvisioningTooManyWrites"
      "expr": |
        avg by (cluster, namespace) (rate(tempo_ingester_bytes_received_total{job=~".+/ingester"}[5m])) / 1024 / 1024 > 30
      "for": "15m"
      "labels":
        "severity": "warning"
    - "alert": "TempoCompactorsTooManyOutstandingBlocks"
      "annotations":
        "message": "There are too many outstanding compaction blocks in {{ $labels.cluster }}/{{ $labels.namespace }} for tenant {{ $labels.tenant }}, increase compactor's CPU or add more compactors."
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoCompactorsTooManyOutstandingBlocks"
      "expr": |
        sum by (cluster, namespace, tenant) (tempodb_compaction_outstanding_blocks{container="compactor", namespace=~".*"}) / ignoring(tenant) group_left count(tempo_build_info{container="compactor", namespace=~".*"}) by (cluster, namespace) > 100
      "for": "6h"
      "labels":
        "severity": "warning"
    - "alert": "TempoCompactorsTooManyOutstandingBlocks"
      "annotations":
        "message": "There are too many outstanding compaction blocks in {{ $labels.cluster }}/{{ $labels.namespace }} for tenant {{ $labels.tenant }}, increase compactor's CPU or add more compactors."
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoCompactorsTooManyOutstandingBlocks"
      "expr": |
        sum by (cluster, namespace, tenant) (tempodb_compaction_outstanding_blocks{container="compactor", namespace=~".*"}) / ignoring(tenant) group_left count(tempo_build_info{container="compactor", namespace=~".*"}) by (cluster, namespace) > 250
      "for": "24h"
      "labels":
        "severity": "critical"
    - "alert": "TempoIngesterReplayErrors"
      "annotations":
        "message": "Tempo ingester has encountered errors while replaying a block on startup in {{ $labels.cluster }}/{{ $labels.namespace }} for tenant {{ $labels.tenant }}"
        "runbook_url": "https://github.com/grafana/tempo/tree/main/operations/tempo-mixin/runbook.md#TempoIngesterReplayErrors"
      "expr": |
        sum by (cluster, namespace, tenant) (increase(tempo_ingester_replay_errors_total{namespace=~".*"}[5m])) > 0
      "for": "5m"
      "labels":
        "severity": "critical"
  
minio:
  enabled: false

gateway:
  enabled: true
  replicas: 1
  verboseLogging: true
  image:
    repository: nginxinc/nginx-unprivileged
    tag: 1.25-alpine
    pullPolicy: IfNotPresent
  
  terminationGracePeriodSeconds: 30
  
  ingress:
    enabled: false
```

## Installation

```
helm -n tempo upgrade --install tempo grafana/tempo-distributed -f tempo-values.yaml
```

## Prüfen der Installation

Ausgabe nach helm:

```
Release "tempo" does not exist. Installing it now.
NAME: tempo
LAST DEPLOYED: Wed Nov  6 12:08:18 2024
NAMESPACE: tempo
STATUS: deployed
REVISION: 1
TEST SUITE: None
NOTES:
***********************************************************************
 Welcome to Grafana Tempo
 Chart version: 1.20.1
 Tempo version: 2.6.0
***********************************************************************

Installed components:
* ingester
* distributor
* querier
* query-frontend
* compactor
* memcached
* gateway
```

Prüfen ob die Pods laufen

```
kubectl get pods -n tempo

NAME                                      READY   STATUS    RESTARTS   AGE
tempo-compactor-797d6b77f7-gw6ts          1/1     Running   0          4m1s
tempo-distributor-69669fbb9d-ft8hq        1/1     Running   0          4m1s
tempo-gateway-6b68c9db66-grv5v            1/1     Running   0          11m
tempo-ingester-0                          1/1     Running   0          3m59s
tempo-memcached-0                         1/1     Running   0          11m
tempo-metrics-generator-65cbbb696-676zj   1/1     Running   0          4m1s
tempo-querier-86bb8599d9-7grvn            1/1     Running   0          4m1s
tempo-query-frontend-57d985dbbf-6nxx9     1/1     Running   0          4m1s
```

## Monitoring

Für die Dashboards folgende Datei erstellen:

**kustomization.yaml**

```
namespace: tempo

commonLabels:
    app.kubernetes.io/part-of: tempo
    app.kubernetes.io/component: monitoring

generatorOptions:
  disableNameSuffixHash: true

configMapGenerator:
  - name: tempo-dashboards1
    files:
      - dashboards/tempo-tenants.json
      - dashboards/tempo-rollout-progress.json
      - dashboards/tempo-resources.json
      - dashboards/tempo-reads.json
      - dashboards/tempo-writes.json
    options:
      labels:
        grafana_dashboard: "1"
        app.kubernetes.io/part-of: tempo
        app.kubernetes.io/component: monitoring
  - name: tempo-dashboards2
    files:
      - dashboards/tempo-operational.json
    options:
      labels:
        grafana_dashboard: "1"
        app.kubernetes.io/part-of: tempo
        app.kubernetes.io/component: monitoring
```

Dann die Dashboards im System anlegen:

```
kubectl apply -k .
```

