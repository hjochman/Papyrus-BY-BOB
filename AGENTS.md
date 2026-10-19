# AGENTS.md — Papyrus by Bob

Hinweise für Coding-Agenten und Entwickler. Ausführliche Feature-Beschreibung: siehe [`README.md`](README.md), Änderungshistorie: [`CHANGELOG.md`](CHANGELOG.md).

## Projektüberblick

- **App:** „Papyrus by Bob“ — schlanke Notiz-App für Android (Notizen als `.txt`-Dateien in frei wählbarem Ordner, optional AES-256-GCM-verschlüsselt).
- **Inspiration:** „Papyrus Ex“ von Kallisto (`com.kallisto.papyrusex`). Komplette Neuimplementierung, kein Code, Name oder Grafik des Originals. Mit dem Original verschlüsselte Notizen werden **nicht** unterstützt (kein Fallback).
- **Package / applicationId:** `com.hape.payrus`
- **Version:** `1.0.0` (`versionCode 1`)
- **Sprache:** Java 17, AndroidX, Material Components
- **SDK:** `minSdk 24`, `targetSdk 34`, `compileSdk 34`
- **Lokalisierung:** Englisch (Standard), Deutsch (`values-de`), Russisch (`values-ru`)
- **Remote:** `https://github.com/hjochman/Papyrus-BY-BOB.git` (Branch `main`)

## Struktur

```
app/src/main/java/com/hape/payrus/
  PapyrusApplication.java        Einstiegspunkt
  data/    NoteItem, StorageManager (CRUD, rekursive Suche, Sortierung), CryptoManager (PBKDF2 + AES-256-GCM)
  ui/      MainActivity, EditorActivity, SettingsActivity, DirectoryPickerActivity, adapter/NoteAdapter
  util/    PreferenceHelper (Settings + Suchverlauf), StorageAccess
app/src/test/java/com/hape/payrus/   JUnit 4 + Robolectric
app/src/main/res/                    Layouts, Strings (en/de/ru), Menüs, Icons
```

Abhängigkeiten (siehe [`app/build.gradle`](app/build.gradle)): appcompat 1.6.1, material 1.11.0, constraintlayout 2.1.4, recyclerview 1.3.2, preference 1.2.1, documentfile 1.0.1; Tests: junit 4.13.2, robolectric 4.17.

## Voraussetzungen

- JDK 17 (macOS/Homebrew: `/opt/homebrew/opt/openjdk@17`)
- Android SDK 34 (Pfad in der git-ignorierten `local.properties`)
- Gradle Wrapper (`./gradlew`), kein separates Gradle nötig

## Build-Befehle

Alle Befehle im Projektstamm:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17

./gradlew assembleDebug        # Debug-APK  -> app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # Unit-Tests (JUnit + Robolectric)
./gradlew lint                 # Android Lint
./gradlew assembleRelease      # Release-APK -> app/build/outputs/apk/release/app-release.apk
```

Vor Abschluss einer Änderung mindestens `testDebugUnitTest` und `assembleDebug` ausführen (bei UI-/Ressourcenänderungen zusätzlich `lint`).

## Release-Signierung

- Keystore: `patch-key.jks` im Projektstamm (Alias `patchkey`). **Nicht** im Repository (`*.jks` ist in `.gitignore`).
- Zugangsdaten werden in dieser Reihenfolge gesucht (siehe `app/build.gradle`):
  1. `keystore.properties` im Projektstamm (git-ignoriert)
  2. Gradle-Properties (`~/.gradle/gradle.properties`)
  3. Umgebungsvariablen
- Benötigte Schlüssel: `PAPYRUS_STORE_PASSWORD`, `PAPYRUS_KEY_ALIAS` (Default `patchkey`), `PAPYRUS_KEY_PASSWORD` (Default = Store-Passwort).
- Fehlen die Zugangsdaten, wird die Release-APK **unsigniert** gebaut (Build bricht nicht ab).
- Release-Build: `minifyEnabled = false`; ProGuard-Regeln in `app/proguard-rules.pro`.

### Regeln

- **Niemals** Passwörter, Keystores oder `keystore.properties` committen oder in Dokumentation/Logs schreiben.
- Neue Secrets nur über die oben genannten Wege einlesen.

## Zusätzliche Dateien im Repo

| Datei | Zweck |
|---|---|
| `Papyrus by Bob_v1.0.0.apk` | Signierte Release-APK (Binärdatei im Repo) |
| `uber-apk-signer.jar` | Optionales Tool zum Signieren/Verifizieren von APKs |
| `Papyrus BY BOB.png`, `Papyrus BY BOB_preview.jpg` | Logo bzw. Vorschau für das README |

Optionale Verifikation einer APK:

```bash
$JAVA_HOME/bin/java -jar uber-apk-signer.jar --apks "Papyrus by Bob_v1.0.0.apk" --onlyVerify
```

## Konventionen

- Moderne Java-17-Syntax, AndroidX/Material; Stil des bestehenden Codes beibehalten.
- Neue Strings in **allen drei** Sprachen pflegen: `values/`, `values-de/`, `values-ru/`.
- Neue Logik in `data/` bzw. `util/` mit Unit-Tests unter `app/src/test/` abdecken.
- Kryptografie: ausschließlich über `CryptoManager` (PBKDF2 + AES-256-GCM); keine eigenen Verfahren, kein Klartext-Passwort-Logging.
- Änderungen im `CHANGELOG.md` (Keep a Changelog / SemVer) festhalten; bei Versionssprung `versionCode`/`versionName` in `app/build.gradle` anpassen.
- Build-Artefakte (`build/`, `.gradle/`), `local.properties`, `.idea/` bleiben aus Git (siehe `.gitignore`).

## Git-Hinweis

Schlägt `git push` mit `HTTP 400` / „RPC failed“ fehl (große Binärdateien), hilft:

```bash
git config http.postBuffer 524288000
git config http.version HTTP/1.1
```
