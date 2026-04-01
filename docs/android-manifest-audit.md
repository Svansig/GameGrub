# Android Manifest Audit

Last updated: 2026-04-01
Scope: `app/src/main/AndroidManifest.xml`

## Why this document exists

This is a living reference for manifest-level decisions that are easy to lose in commit history:
- platform compatibility updates (Android 12+ / 14+ / 15)
- Play policy-sensitive permissions
- security-relevant component exposure
- rationale for keeping high-risk declarations

## Current manifest baseline

### App-level attributes

- `android:allowBackup="false"` remains explicit (backup disabled).
- `android:fullBackupContent="@xml/backup_rules"` added.
- `android:dataExtractionRules="@xml/data_extraction_rules"` added for Android 12+ transfer/cloud backup controls.
- `android:extractNativeLibs="true"` remains set (matches existing native packaging behavior).
- `android:usesCleartextTraffic="true"` remains set (legacy/network compatibility behavior; security tradeoff documented below).

### Permissions retained

- `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `INTERNET`, `VIBRATE`
- `RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS`
- `POST_NOTIFICATIONS`
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`
- `MANAGE_EXTERNAL_STORAGE` (policy-sensitive; still required by current custom folder scanning flow)
- `REQUEST_INSTALL_PACKAGES` (policy-sensitive; tied to paused legacy update installer flow)

### Permissions removed as obsolete for this app

Removed due to `minSdk = 33` and no effect on supported devices:
- `READ_EXTERNAL_STORAGE`
- `WRITE_EXTERNAL_STORAGE`
- `READ_LOGS`

## Component exposure review

### Activities

- `MainActivity` exported intentionally (launcher/deep links/action handling).
- OAuth activities (`GOGOAuthActivity`, `EpicOAuthActivity`, `AmazonOAuthActivity`) are non-exported.
- Icon aliases (`MainActivityAliasDefault`, `MainActivityAliasAlt`) are exported intentionally for launcher integration.

### Services

- Platform services (`SteamService`, `GOGService`, `EpicService`, `AmazonService`) are non-exported.
- Each declares `android:foregroundServiceType="dataSync"`, which matches Android 14+ requirements when starting foreground work.

### Provider

- `FileProvider` is non-exported and grants URI permissions only as needed.

## Remaining warnings and status

These are expected policy/lint warnings and not manifest syntax errors:

1. `MANAGE_EXTERNAL_STORAGE`
   - Status: intentional for current folder scanning UX outside app sandbox.
   - Risk: high Play policy scrutiny and approval requirements.

2. `REQUEST_INSTALL_PACKAGES`
   - Status: currently still declared while update install implementation is paused.
   - Risk: Play policy restriction; should be removed if self-update remains disabled.

3. `FOREGROUND_SERVICE`
   - Status: expected warning reminding to justify and type each foreground service on Android 14+.
   - Mitigation: type declarations are present on service components (`dataSync`).

## Open decisions (product/policy)

1. Storage model hardening
   - Option A: keep `MANAGE_EXTERNAL_STORAGE` and maintain broad manual-folder UX.
   - Option B: move to SAF/document picker model and remove all-files access.

2. In-app APK install capability
   - Option A: if updater remains paused, remove `REQUEST_INSTALL_PACKAGES` and related install surface.
   - Option B: keep it and enforce strict user-initiated install flow with policy evidence.

3. Cleartext traffic
   - Option A: keep temporary compatibility behavior.
   - Option B: move to per-domain network security config and tighten defaults.

## Verification checklist

When touching the manifest, run and capture results in PR/ticket notes:

- `./gradlew lint`
- `./gradlew testDebugUnitTest`
- manifest merge report check (`:app:processDebugMainManifest`)
- manual smoke on Android 14/15 device/emulator:
  - startup + notification permission prompt
  - platform service foreground operations
  - manual custom folder scan flow

## Documentation impact

This document is the canonical record for manifest policy posture and compatibility decisions.

