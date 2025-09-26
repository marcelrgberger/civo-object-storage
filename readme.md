# Civo Object Storage (Jakarta EE)

Leichtgewichtiger Jakarta-EE-Wrapper für Civo Object Storage (S3-kompatibel via MinIO Java SDK). Bietet einfache
Methoden zum Hochladen, Abrufen und Löschen von Objekten inkl. Content-Type und benutzerdefinierten Metadaten.

## Features

- Upload von Bytes mit Content-Type und optionalen User-Metadaten
- Abrufen von Objektinhalt, Content-Type und User-Metadaten
- Löschen von Objekten
- ApplicationScoped Bean, MicroProfile Config-Integration

## Voraussetzungen

- Java 21
- Jakarta EE / MicroProfile Config
- Zugangsdaten für Civo Object Storage (Access Key, Secret Key, Bucket)
- Endpoint (z. B. FRA1: https://objectstore.fra1.civo.com)

## Konfiguration (application.yaml)

application.yaml:

```yaml
storage:
  civo:
    endpoint: https://objectstore.fra1.civo.com
    access-key: YOUR_ACCESS_KEY
    secret-key: YOUR_SECRET_KEY
    bucket: YOUR_BUCKET
```

Hinweise:

- Schlüssel entsprechen den MicroProfile-Property-Namen (storage.civo.*) in YAML-Hierarchie.
- Stellen Sie sicher, dass Ihr Runtime-Stack YAML als MicroProfile Config-Quelle lädt (z. B. via SmallRye Config
  YAML-Extension oder eigener ConfigSource).

## Verwendung

- Upload: Bytes unter Schlüssel speichern, Content-Type setzen, optional User-Metadaten
- Get: Objektinhalt, Content-Type und Metadaten als Rückgabe
- Delete: Objekt über Schlüssel entfernen

Beispielaufrufe (pseudocode):

```java 
var userMeta = Map.of("x-my-flag", "true"); 
storage.

putBytes("path/file.txt","hello".getBytes(UTF_8),"text/plain",userMeta);
var obj = storage.getObject("path/file.txt");
var bytes = obj.data();
var type = obj.contentType();
var meta = obj.userMetadata();
storage.

deleteObject("path/file.txt");

```

## Fehlerbehandlung

Alle Remote-/IO-Fehler werden als CivoObjectStorageException gekapselt.

## Sicherheit

- Geheimnisse ausschließlich über Config/Secrets-Management bereitstellen
- TLS-geschützten Endpoint verwenden
- Geeignete Bucket-Policies setzen

## Lizenz

MIT