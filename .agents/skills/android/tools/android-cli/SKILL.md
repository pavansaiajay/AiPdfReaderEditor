---
name: android-cli
description: How to use the official Google Android CLI for searching documentation, looking up library versions, inspecting UI layouts, capturing screenshots, and interacting with Android devices.
---

# Android CLI Specialist Guide

The `android` CLI (`dl.google.com/android/cli`) is installed and available in the environment. Agents should use it as the primary authority for querying up-to-date Android APIs, documentation, and device interaction.

## 1. Documentation & API Search
Use the `android docs` command to search authoritative, high-quality Android documentation from the official Knowledge Base:
```powershell
# Search Android documentation (enclose query in quotes)
android docs search "Jetpack Compose state hoisting"
android docs search "Room 3 bundled sqlite migration"
android docs search "Navigation Compose type safe routes"

# Fetch the full documentation article
android docs fetch "kb://..."
```
**When to use**:
- Finding official examples and migration guides.
- Checking best practices for new Android APIs.
- Resolving API deprecations or compatibility questions.

## 2. Library & Artifact Version Lookup
Look up the latest available versions of Maven artifacts, Gradle plugins, or Android SDKs:
```powershell
android studio version-lookup androidx.compose.material3:material3
android studio version-lookup com.google.dagger:hilt-android
android studio version-lookup androidx.room3:room3-runtime
```

## 3. UI Layout Inspection & Screen Capture
Inspect running application UI layouts directly in JSON format (faster and more deterministic than raw screenshots):
```powershell
# Get layout tree in JSON format
android layout --pretty

# Get layout diff of changes since last invocation
android layout --diff

# Capture screen to file
android screen capture -o=screenshot.png
```

## 4. Emulator & Device Management
```powershell
android emulator list
android emulator start <avd_name>
android run --activity=<ActivityName>
```
