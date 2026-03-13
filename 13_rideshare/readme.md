# Installation Demo

## Buildkit einrichten

Wenn multiarch-Build nicht möglich ist dann folgendes ausführen:

```sh
docker buildx create --name multiarch --driver docker-container --use
docker buildx inspect --bootstrap
```

## Build Image

Image mit Pyroscope-Agent:

```sh
cd rideshare
docker buildx build --platform linux/arm64,linux/amd64 -t fra.ocir.io/frsjez6mlvwx/slixpublic/ride-share-app:1.0 --push -f Dockerfile .
```

Image für Autoinstrumentierung:

```sh
cd rideshare-auto
docker buildx build --platform linux/arm64,linux/amd64 -t fra.ocir.io/frsjez6mlvwx/slixpublic/ride-share-auto:1.3.1 --push -f Dockerfile-auto .
```

Image für Loadgenerator:

1.0.1 -> Build mit einem singlepod
1.0.2 -> Build mit 3 Pods
1.1.0 -> Build mit Webinterface

```sh
cd loadgenerator
docker buildx build --platform linux/arm64,linux/amd64 -t fra.ocir.io/frsjez6mlvwx/slixpublic/ride-share-load:1.1.2 --push -f Dockerfile .
```