# Papyrus Ex — APK Patch Notes

## Was wurde gemacht

Die originale `Papyrus Ex_v2.3.3.apk` hat keine SDK-Versionsangaben im Manifest gehabt.
Moderne Android-Versionen zeigen dadurch eine "App zu alt"-Warnung. Die App selbst läuft problemlos.

### Änderungen am `AndroidManifest.xml`

| Attribut | Wert |
|---|---|
| `android:compileSdkVersion` | `34` |
| `android:compileSdkVersionCodename` | `14` |
| `android:minSdkVersion` | `21` |
| `android:targetSdkVersion` | `34` |

`compileSdkVersion` und `compileSdkVersionCodename` wurden als Attribute am `<manifest>`-Tag gesetzt.
`minSdkVersion` und `targetSdkVersion` wurden als `<uses-sdk>`-Element eingefügt.

---

## Build & Sign Workflow

### Voraussetzungen

- `apktool` (getestet mit 3.0.3)
- `uber-apk-signer.jar` (im Projektverzeichnis)
- `patch-key.jks` (Keystore im Projektverzeichnis)
- Java: `/opt/homebrew/opt/openjdk/bin`

### 1. APK dekodieren

```bash
apktool d "Papyrus Ex_v2.3.3.apk" -o papyrus_decoded --force
```

### 2. Manifest editieren

Datei: `papyrus_decoded/AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.kallisto.papyrusex"
    android:compileSdkVersion="34"
    android:compileSdkVersionCodename="14">

    <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="34"/>
    ...
</manifest>
```

### 3. APK neu bauen

```bash
apktool b -f papyrus_decoded -o "Papyrus Ex_v2.3.3_patched_unsigned.apk"
```

> **Immer `-f` verwenden.** Ohne `-f` übernimmt apktool Manifest-Änderungen aus `apktool.yml` nicht zuverlässig.

### 4. APK signieren (v1+v2+v3) & zipalign

```bash
JAVA_BIN=/opt/homebrew/opt/openjdk/bin
$JAVA_BIN/java -jar uber-apk-signer.jar \
  --apks "Papyrus Ex_v2.3.3_patched_unsigned.apk" \
  --ks patch-key.jks \
  --ksAlias patchkey \
  --ksPass patchpass \
  --ksKeyPass patchpass \
  --allowResign \
  --out .
```

Output: `Papyrus Ex_v2.3.3_patched_unsigned-aligned-signed.apk`

### 5. Verifizieren (optional)

```bash
$JAVA_BIN/java -jar uber-apk-signer.jar \
  --apks "Papyrus Ex_v2.3.3_patched_unsigned-aligned-signed.apk" \
  --onlyVerify
```

---

## Ergebnis

| Datei | Beschreibung |
|---|---|
| `Papyrus Ex_v2.3.3.apk` | Original, unverändert |
| `Papyrus Ex_v2.3.3_patched_unsigned.apk` | Gepatchtes Manifest, unsigniert |
| `Papyrus Ex_v2.3.3_patched_unsigned-aligned-signed.apk` | **Finales APK** — zipaligned, signiert v1+v2+v3 |

## Keystore-Info

| Eigenschaft | Wert |
|---|---|
| Datei | `patch-key.jks` |
| Alias | `patchkey` |
| Algorithmus | RSA 2048 |
| Gültig bis | 2054-02-20 |
| SHA256 | `f11069694c38b04043be1b0cf00e3b08a66faf81b4f3ad42f8def960933f737b` |
