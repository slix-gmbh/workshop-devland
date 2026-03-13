# Beyla

Was ist Beyla und für was?
- Autoinstrumentierung für Metriken und Traces 
- Metriken und Traces der Anwendungen werden über ebpf geliefert
- keine spezielle Instrumentierung der Anwendug notwendig
- aber besonderen Rechte im Cluster notwendig...

## Einführung eBPF

Was ist eBPF und was kann es?
- kurze beschreibung
- kurzer Ausblick

## Bewertung
Für wen eignet sich Beyla?
Wo sind die Grenzen
Wann besser selbst instrumentieren?

# Installation von Beyla
```
helm repo add grafana https://grafana.github.io/helm-charts
helm upgrade --install beyla -n beyla --create-namespace  grafana/beyla -f values.yaml
```
