# Installation

## Operator installieren

```sh
helm install flux-operator oci://ghcr.io/controlplaneio-fluxcd/charts/flux-operator \
  --namespace flux-system \
  --create-namespace
```

## FluxInstance bereitstellen

Um Flux zu installieren und über den Operator zu verwalten ist ein FluxInstance-Artefakt zu installieren und vorher ein Secret mit dem Token des gitRepositorys anzulegen

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: github-token
  namespace: flux-system
stringData:
  username: thorsten@slix.de
  password: <token>
```

Ausrollen der Artefakte

```sh
kubectl apply -f git-repo-token.yaml
kubectl apply -f fluxInstance.yaml -n flux-system

```

## Binary installieren

Die Flux Binary der gewünschten Version installieren

Direkt aus github die Artefakte runterladen

```sh
brew install fluxcd/tap/flux
```

oder Version:

```sh
brew install fluxcd/tap/flux@2.3
```

Und dann die Binary für den operator installieren:

```sh
brew install controlplaneio-fluxcd/tap/flux-operator
```

## Direkt in GitRepo mit Anlage

Bootstrap generic git
Voraussetzungen: Adminrechte im Cluster, mindestens push-Rechte im git
Repository erstellen
Username/Password

```sh
flux bootstrap git \
  --url=https://bitbucket.org/slixrepo/fluxworkshop \
  --username=x-token-auth \
  --password=ATCTT3xFfGN0M3Vvr_Uj2Wtg5JtnDHWJre5-oMardYaF_<...> \
  --token-auth=true \
  --path=clusters/my-cluster
```

Mit Accesstoken:

```sh
flux bootstrap git \
  --url=https://bitbucket.org/slixrepo/fluxworkshop \
  --password=ATCTT3xFfGN0M3Vvr_Uj2Wtg5JtnDHWJre5-oMardYaF_<...> \
  --with-bearer-token \
  --path=clusters/my-cluster
```

https://bitbucket.org/slixrepo/fluxworkshop

## Manuell ohne Operator

flux install --components-extra="image-reflector-controller,image-automation-controller" --export > flux-system/app/gotk-components.yaml

Dann die kustomization unter flux-system/app ausführen

## Löschen von flux

Auf dem Cluster flux-system kustomization löschen. Wenn prune nicht bewusst auf false, werden alle ressourcen entfernt