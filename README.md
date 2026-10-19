# Papyrus by IBM Bob

**Papyrus by Bob** (`com.hape.payrus`) ist eine moderne, leichtgewichtige und sichere Notiz-App für Android, inspiriert von „Papyrus Ex“ von Kallisto. Sie ist komplett neu in modernem **Java (Android SDK 34 / AndroidX / Material Components)** geschrieben.

---

## 🧭 Herkunft & Credit

**Inspiriert von der App „Papyrus Ex“ von Kallisto** (Version 2.3.3, Paket `com.kallisto.papyrusex`). Dieses Projekt ist ein neues, unabhängiges Werk und steht in keiner Verbindung zum Entwickler des Originals.

- **Code:** komplett neu geschrieben in modernem Java (AndroidX, Material Components, Android 14). Es wurde kein Quellcode des Originals kopiert oder übernommen.
- **Idee und Bedienkonzept:** Notizen als einfache Textdateien in einem frei wählbaren Ordner. Das Konzept stammt vom Original, Ideen und Funktionsweisen sind urheberrechtlich nicht geschützt.
- **Name und Icon:** eigenständig („Papyrus by Bob“). Weder Name noch Grafiken des Originals werden verwendet.
- **Anlass:** Das Original wurde 2018 aus dem Google Play Store entfernt und deklarierte keine SDK-Versionen im Manifest, sodass aktuelle Android-Versionen es als „zu alt“ markieren.
- **Neu gegenüber dem Original:** Verschlüsselung mit AES-256-GCM und PBKDF2, Suchverlauf, Ordnerauswahl mit Schnellwahl für Telefon und SD-Karte, Adaptive Icon, deutsche Übersetzung und ein About-Dialog.
- **Nicht übernommen:** Mit dem Original verschlüsselte Notizen lassen sich nicht entschlüsseln. Es gibt keinen Fallback auf das alte Verschlüsselungsformat.

---

## 📱 Funktionsumfang & Features

- **Speicherverwaltung & Ordnernavigation:**
  - Freie Wahl des Speicherorts über einen interaktiven Pfadauswahldialog (`DirectoryPickerActivity`).
  - Speicherung direkt im Dateisystem als Klartext- bzw. verschlüsselte `.txt`-Dateien.
  - Vollständige Ordnernavigation inklusive Aufwärtsnavigation (`..`).
  - Rekursive Volltextsuche über alle Notizen im ausgewählten Verzeichnis.
- **Suchverlauf (Search History):**
  - Automatisches Speichern der letzten 10 Suchbegriffe (ohne Duplikate, neueste zuerst).
  - Komfortable Auswahl zuvor gesuchter Begriffe aus einem Dialog in der Hauptansicht.
  - Option zum vollständigen Löschen des Suchverlaufs.
- **Krypto-Verschlüsselung (AES-256-GCM):**
  - Starker Passwortschutz für sensible Notizinhalte via PBKDF2 und AES-256-GCM.
  - Dateinamen bleiben im Klartext für die Übersicht lesbar, während der Inhalt kryptografisch geschützt wird.
- **Notiz-Editor (`EditorActivity`):**
  - Dynamische Titelgenerierung (erste Zeile der Notiz dient als Titel, angezeigt mit automatischer Kürzung `...`).
  - In-Text-Suche mit Vorwärts-/Rückwärtsnavigation im Editor.
  - Schnellaktionen: Aktuelles Datum/Zeit einfügen, Verschlüsseln/Entschlüsseln, Text über das System teilen (Senden).
  - Linkify-Unterstützung (automatische Erkennung von URLs, E-Mails und Telefonnummern).
- **Einstellungen (`SettingsActivity`):**
  - Konfiguration des Notizenpfads.
  - Anpassbare Sortierung (Name, Änderungsdatum) und Platzierung von Ordnern (oben/unten/sortiert).
  - Visuelle Themes für den Editor (Standard, Dunkel, Amber/Sepia).
  - Anpassbare Schriftgröße und Monospace-Schriftart.
- **System-Integration:**
  - App-Shortcut (`PENewNoteShortcut`) zum schnellen Erstellen einer neuen Notiz direkt vom Android-Startbildschirm.
  - Empfänger für den `ACTION_SEND`-Intent zum schnellen Importieren von geteiltem Text aus anderen Apps.
- **Lokalisierung:**
  - Englisch (Standard), Deutsch und Russisch. Die App folgt der Systemsprache.
- **About-Dialog:**
  - Menü „Über…“ mit Version, Hinweisen und dem Verweis auf das Original.

---

## ⚙️ Voraussetzungen & Systemanforderungen

- **Entwicklungsumgebung:** Android Studio (Iguana oder neuer empfohlen)
- **Java Development Kit (JDK):** Java 17 (z. B. OpenJDK 17)
- **Android SDK:** Version 34 (Android 14)
- **Mindestanforderung Gerät (minSdk):** API 24 (Android 7.0+)
- **Ziel-Plattform (targetSdk):** API 34 (Android 14)

---

## 🛠️ Build- & Test-Befehle (CLI)

Führe alle Gradle-Befehle unter Angabe von `JAVA_HOME` für JDK 17 aus (z. B. auf macOS):

### 1. Projekt kompilieren (Debug)
Erzeugt ein unbeschränktes, unsigniertes Debug-APK:
```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleDebug
```
Das fertige APK liegt unter: `app/build/outputs/apk/debug/app-debug.apk`

### 2. Unit- & Integrationstests ausführen
Führt alle Unit-Tests (inkl. Robolectric-Tests für `CryptoManager`, `StorageManager` und Suchverlauf) aus:
```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew testDebugUnitTest
```

### 3. Statische Codeanalyse (Lint)
Führt Android-Lint zur Überprüfung auf Codequalität und potenzielle Probleme aus:
```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew lint
```

### 4. Release-APK bauen und signieren
Erzeugt ein optimiertes Release-APK und signiert es automatisch mit dem konfigurierten Keystore:
```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleRelease
```
Das signierte APK liegt unter: `app/build/outputs/apk/release/app-release.apk` und wird zusätzlich als `Papyrus by Bob_v1.0.0.apk` im Projektstamm abgelegt.

---

## 🔑 Release-Signierung (Sicherheitshinweise)

Aus Sicherheitsgründen werden Zugangsdaten für den Release-Keystore (`patch-key.jks`) **niemals im Repository im Klartext hinterlegt**.

Der Build-Prozess in `app/build.gradle` sucht nach den benötigten Variablen in folgender Reihenfolge:
1. In der git-ignorierten Datei `keystore.properties` im Projektstamm.
2. In den globalen Gradle-Properties (z. B. `~/.gradle/gradle.properties`).
3. In den System-Umgebungsvariablen.

### Konfiguration über `keystore.properties` (Empfohlen)
Erstelle im Projektstamm eine Datei namens `keystore.properties` mit folgendem Inhalt (Beispielwerte für den mitgelieferten Keystore):
```properties
PAPYRUS_STORE_PASSWORD=dein_keystore_passwort
PAPYRUS_KEY_ALIAS=patchkey
PAPYRUS_KEY_PASSWORD=dein_schluessel_passwort
```
*(Die Datei `keystore.properties` sowie JKS-Keystore-Dateien sind in `.gitignore` eingetragen und werden nicht committet.)*

### Alternativ über Umgebungsvariablen
```bash
export PAPYRUS_STORE_PASSWORD="dein_keystore_passwort"
export PAPYRUS_KEY_ALIAS="patchkey"
export PAPYRUS_KEY_PASSWORD="dein_schluessel_passwort"
```

Wenn keine Zugangsdaten gefunden werden, bricht der Release-Build nicht ab, erzeugt jedoch eine unsignierte APK.

---

## 📂 Projektstruktur

- `app/src/main/java/com/hape/payrus/` - Java-Quellcode der Anwendung:
  - `PapyrusApplication.java` - Anwendungs-Einstiegspunkt.
  - `data/` - Datenhaltung und Logik:
    - `NoteItem.java` - Modell für Notizen und Ordner.
    - `StorageManager.java` - Dateizugriffe (CRUD), rekursive Volltextsuche und Sortierung.
    - `CryptoManager.java` - AES-256-GCM Ver-/Entschlüsselung.
  - `ui/` - Benutzeroberfläche:
    - `MainActivity.java` - Hauptliste, Navigation, Suchverlauf-Verarbeitung.
    - `EditorActivity.java` - Editor, In-Text-Suche, Krypto-Bedienung.
    - `SettingsActivity.java` - Preference-Screen.
    - `DirectoryPickerActivity.java` - Pfadauswahl im lokalen Dateisystem.
    - `adapter/` - Listen-Adapter für RecyclerViews.
  - `util/` - Hilfsklassen:
    - `PreferenceHelper.java` - Persistierung von Einstellungen und Suchverlauf in SharedPreferences.
- `app/src/test/java/com/hape/payrus/` - Automatisierte Unittests unter Verwendung von JUnit und Robolectric:
  - `data/CryptoManagerTest.java`
  - `data/StorageManagerTest.java`
  - `util/PreferenceHelperSearchHistoryTest.java`
- `app/src/main/res/` - Anwendungsressourcen (Layouts, Strings, Menüs, Icons).
