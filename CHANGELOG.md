# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-10-19

### Added
- **Complete Re-implementation**: Written from scratch in modern Java (Android SDK 34 / AndroidX / Material Components) under package name `com.hape.payrus`.
- **Crypto Manager (AES-256-GCM)**: Strong security layer using PBKDF2 for key derivation and AES-256-GCM for file content encryption, replacing legacy DES/AES-CBC.
- **Interactive Directory Picker**: Interactively browse and select directory paths for notes storage via `DirectoryPickerActivity` with document file abstraction support.
- **Search History**: Embedded search history dialog preserving up to 10 latest search terms across app sessions.
- **Recursive Note Search**: Live full-text filtering and recursive file system searching.
- **Enhanced Editor**: Rich text editor featuring text search (forward/backward), insert current timestamp, system-wide sharing, on-the-fly decryption/encryption, and automatic title generation from the first line.
- **Themes & Styling**: Editor color themes (Default, Dark, Amber), Monospace font toggling, and font size configurations.
- **System Integration**: System shortcut `PENewNoteShortcut` to trigger a new note and an `ACTION_SEND` intent receiver to import text from third-party apps.
- **Localization**: Native support for English, German, and Russian locales.
- **About Dialog**: New "About…" entry in the main menu showing the app version, usage notes and a credit to "Papyrus Ex" by Kallisto, which inspired this project.
- **Branding**: App renamed to "Papyrus by Bob" with its own icon; no name or graphics of the original are used.
- **Adaptive Icon**: Launcher icon as adaptive icon (Android 8+) with PNG fallback for older versions.
- **Storage Access**: "All files access" flow and quick-select buttons for phone and SD card in the directory picker.
- **Unit Testing**: 100% green JUnit and Robolectric unit tests for `CryptoManager`, `StorageManager`, and search history logic.
