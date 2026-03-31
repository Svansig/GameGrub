# SteamService Ownership Matrix

This document is the exhaustive ownership audit for `app/src/main/java/app/gamegrub/service/steam/SteamService.kt`.

It records:
- where each field and method currently lives,
- where it should live in the best-practice end-state,
- and whether to keep, move, or remove wrapper behavior.

## Ownership Rules

- `SteamService` owns Android lifecycle, foreground notification wiring, connectivity callback registration, Steam client connection wiring, and callback subscription wiring.
- Domains own business workflows and persistence coordination.
- Managers are implementation helpers behind domains.
- Companion object is temporary compatibility only and should trend toward removal.
- Service shell should not directly use DAOs in end-state.

## Pruning Corrections (strict end-state)

The first pass kept too many members in `SteamService`/companion. For strict thin-service goals, apply these corrections:

- Keep in `SteamService` only lifecycle/callback wiring and Steam handler registration state.
- Move all account/session/library state mirrors (`_loginResult`, persona/family state, token caches, PICS jobs/channels) to domains.
- Move all download orchestration state (`downloadJobs`, `licenses`, listener-owned maps) to install/download coordinator.
- Move all mutable global flags in companion (`isStopping`, `isConnected`, `isRunning`, `isLoggingOut`, `isWaitingForQRAuth`) into a scoped runtime state holder owned by service or account domain.
- Deprecate companion facades once call sites are migrated; default target is removal, not permanent facade.

Concretely, treat the following previous `Keep` calls as transitional only:

- `instance`, `requireInstance`, `getInstanceOrNull` -> remove service-locator usage from business paths.
- `MAX_RETRY_ATTEMPTS` -> move under connection policy owner.
- `keepAlive` / `autoStopWhenIdle` companion properties -> keep only as temporary wrappers, then remove.
- `isLoggedIn`, `userSteamId`, `isLoginInProgress`, `familyMembers` companion reads -> expose from `SteamAccountDomain` state model directly.
- `hasPartialDownload`, library/account/session wrapper methods -> collapse to direct domain APIs at call sites, then delete wrappers.

## Fields (class body)

| Member | Current | End-state owner | Decision |
|---|---|---|---|
| `logger` | `SteamService` | `SteamService` | Keep |
| `db` | `SteamService` | Domain transaction boundaries | Keep for transaction boundary (SRV-014) |
| `steamClientProvider` | `SteamService` DI | `SteamService` wiring layer | Keep |
| `licenseDao` | `SteamService` | `SteamLibraryDomain` | Done - removed (SRV-014) |
| `appDao` | `SteamService` | `SteamLibraryDomain` | Done - removed (SRV-014) |
| `libraryDomain` | `SteamService` DI | `SteamService` delegation boundary | Keep |
| `sessionDomain` | `SteamService` DI | `SteamService` delegation boundary | Keep |
| `notificationHelper` | `SteamService` | `SteamService` | Keep |
| `callbackManager` | `SteamService` | `SteamService` | Keep |
| `steamClient` | `SteamService` | `SteamService` | Keep |
| `callbackSubscriptions` | `SteamService` | `SteamService` | Keep |
| `picsGetProductInfoJob` | `SteamService` | `SteamPicsSyncDomain` | Done - removed (SRV-009) |
| `picsChangesCheckerJob` | `SteamService` | `SteamPicsSyncDomain` | Done - removed (SRV-009) |
| `friendCheckerJob` | `SteamService` | `SteamAccountDomain` | Done - removed (SRV-009) |
| `_unifiedFriends` | `SteamService` | `SteamService` callback wiring | Keep |
| `_steamUser` | `SteamService` | `SteamService` handler wiring | Keep |
| `_steamApps` | `SteamService` | `SteamService` handler wiring (domain-access via adapter) | Keep (encapsulate access) |
| `_steamFriends` | `SteamService` | `SteamService` handler wiring | Keep |
| `_steamCloud` | `SteamService` | `SteamService` handler wiring | Keep |
| `_steamUserStats` | `SteamService` | `SteamService` handler wiring | Keep |
| `_steamFamilyGroups` | `SteamService` | `SteamAccountDomain` | Move behavior ownership |
| `_loginResult` | `SteamService` | `SteamAccountDomain` | Move |
| `licenses` | `SteamService` | `SteamLibraryDomain`/download coordinator | Move |
| `retryAttempt` | `SteamService` | `SteamService` connection policy | Keep |
| `appPicsChannel` | `SteamService` | `SteamPicsSyncDomain` | Move |
| `packagePicsChannel` | `SteamService` | `SteamPicsSyncDomain` | Move |
| `scope` | `SteamService` | `SteamService` | Keep |
| `reconnectJob` | `SteamService` | `SteamService` | Keep |
| `onEndProcess` | `SteamService` | `SteamService` | Keep |
| `familyGroupMembers` | `SteamService` | `SteamAccountDomain` | Move |
| `appTokens` | `SteamService` | `SteamPicsSyncDomain` | Move |
| `connectivityManager` | `SteamService` | `SteamService` | Keep |
| `networkCallback` | `SteamService` | `SteamService` | Keep |
| `picsGetProductInfoJob` | `SteamService` | `SteamPicsSyncDomain` | Move |
| `picsChangesCheckerJob` | `SteamService` | `SteamPicsSyncDomain` | Move |
| `friendCheckerJob` | `SteamService` | `SteamAccountDomain` (if retained) | Move |
| `_localPersona` | `SteamService` | `SteamAccountDomain` | Move |
| `localPersona` | `SteamService` | `SteamAccountDomain` public flow | Move |
| `accountDomain` | `SteamService` DI | `SteamService` delegation boundary | Keep |
| `cloudStatsDomain` | `SteamService` DI | `SteamService` delegation boundary | Keep |
| `installDomain` | `SteamService` DI | `SteamService` delegation boundary | Keep |

## Fields (companion object)

| Member | Current | End-state owner | Decision |
|---|---|---|---|
| `MAX_PICS_BUFFER` | `SteamService.Companion` | `SteamPicsSyncDomain` config | Move |
| `MAX_RETRY_ATTEMPTS` | `SteamService.Companion` | `SteamService` connection policy | Keep |
| `INVALID_APP_ID` | `SteamService.Companion` | `SteamCatalogManager` constants | Move |
| `INVALID_PKG_ID` | `SteamService.Companion` | `SteamCatalogManager`/library constants | Move |
| `STEAM_CONTROLLER_CONFIG_FILENAME` | `SteamService.Companion` | `SteamInstallDomain` | Move |
| `catalogManager` | Companion facade | Internal domain helper | Keep facade, hide later |
| `installDomain` (companion getter) | Companion service locator | Remove service locator pattern | Remove after facade migration |
| `requestTimeout` | Companion mutable config | `SteamConnectionDomain`/provider config | Move |
| `responseTimeout` | Companion mutable config | `SteamConnectionDomain`/provider config | Move |
| `PROTOCOL_TYPES` | Companion | `SteamService` connection wiring | Keep local (non-public) |
| `instance` | Companion singleton ref | `SteamService` only | Keep short-term, reduce usage |
| `cachedAchievements` | Companion cache | `SteamCloudStatsDomain` | Move |
| `cachedAchievementsAppId` | Companion cache | `SteamCloudStatsDomain` | Move |
| `hasWifiOrEthernet` | Companion computed state | `NetworkMonitor` facade only | Keep facade or remove |
| `downloadJobs` | Companion global state | `SteamInstallDomain`/download coordinator | Move |
| `keepAliveFallback` | Companion fallback state | `SteamSessionDomain` | Move |
| `autoStopWhenIdleFallback` | Companion fallback state | `SteamSessionDomain` | Move |
| `keepAlive` | Companion facade | `SteamSessionDomain` | Keep facade |
| `isImporting` | Companion global flag | dedicated import coordinator | Move |
| `isStopping` | Companion runtime flag | `SteamService` | Keep |
| `isConnected` | Companion runtime flag | `SteamService` | Keep |
| `isRunning` | Companion runtime flag | `SteamService` | Keep |
| `isLoggingOut` | Companion runtime flag | `SteamAccountDomain` or service state holder | Move |
| `isLoggedIn` | Companion computed state | account/session domain read model | Keep facade |
| `isWaitingForQRAuth` | Companion runtime flag | `SteamAccountDomain` | Move |
| `userSteamId` | Companion computed state | account domain read model | Keep facade |
| `familyMembers` | Companion facade | `SteamAccountDomain` | Keep facade |
| `isLoginInProgress` | Companion computed state | `SteamAccountDomain` | Keep facade |
| `autoStopWhenIdle` | Companion facade | `SteamSessionDomain` | Keep facade |

## Methods (class body)

| Method | Current | End-state owner | Decision |
|---|---|---|---|
| `onCreate` | `SteamService` | `SteamService` | Keep; delegate business work |
| `onStartCommand` | `SteamService` | `SteamService` | Keep; delegate business work |
| `onDestroy` | `SteamService` | `SteamService` | Keep |
| `onBind` | `SteamService` | `SteamService` | Keep |
| `connectToSteam` | `SteamService` | `SteamService` | Keep |
| `stop` (instance) | `SteamService` | `SteamService` | Keep |
| `clearValues` | `SteamService` | `SteamService` | Keep |
| `reconnect` | `SteamService` | `SteamService` | Keep |
| `onConnected` | `SteamService` callback | `SteamService` + `SteamAccountDomain` | Keep callback; delegate decisions |
| `onDisconnected` | `SteamService` callback | `SteamService` | Keep |
| `onLoggedOn` | `SteamService` callback | `SteamAccountDomain` + `SteamPicsSyncDomain` | Move behavior out |
| `onLoggedOff` | `SteamService` callback | `SteamAccountDomain` | Move behavior out |
| `onPlayingSessionState` | `SteamService` callback | `SteamSessionDomain` | Keep callback delegation |
| `onPersonaStateReceived` | `SteamService` callback | `SteamAccountDomain` | Move behavior out |
| `onLicenseList` | `SteamService` callback | `SteamLibraryDomain` + `SteamPicsSyncDomain` | Move behavior out |
| `onChanged` (QR) | `SteamService` callback | `SteamAccountDomain` event mapping | Move behavior out |
| `continuousPICSChangesChecker` | `SteamService` | `SteamPicsSyncDomain` | Move |
| `picsChangesCheck` | `SteamService` | `SteamPicsSyncDomain` | Move |
| `continuousPICSGetProductInfo` | `SteamService` | `SteamPicsSyncDomain` | Move |
| `getEncryptedAppTicket` | `SteamService` wrapper | `SteamSessionDomain` facade | Keep thin wrapper |
| `getEncryptedAppTicketBase64` | `SteamService` wrapper | `SteamSessionDomain` facade | Keep thin wrapper |

## Methods (companion object)

| Method | Current | End-state owner | Decision |
|---|---|---|---|
| `requireInstance`, `getInstanceOrNull` | Companion | Remove service locator dependency from business paths | Keep temporarily |
| `checkWifiOrNotify` | Companion | `SteamService`/network policy helper | Move out of companion |
| `notifyDownloadStarted`, `notifyDownloadStopped`, `removeDownloadJob` | Companion | download coordinator (`SteamInstallDomain`) | Move |
| `hasPartialDownload` | Companion wrapper | `SteamLibraryDomain` | Keep facade |
| `getSteamId64`, `getSteam3AccountId` | Companion wrapper | `SteamAccountDomain` | Keep facade |
| `setPersonaState`, `requestUserPersona`, `getSelfCurrentlyPlayingAppId` | Companion wrapper | `SteamAccountDomain` | Keep facade |
| `kickPlayingSession` | Companion wrapper | `SteamSessionDomain` | Keep facade |
| `getLicensesFromDb`, `getPkgInfoOf`, `getAppInfoOf`, `getDownloadingAppInfoOf`, `getDownloadableDlcAppsOf`, `getHiddenDlcAppsOf`, `getInstalledApp`, `getInstalledDepotsOf`, `getInstalledDlcDepotsOf` | Companion wrappers | `SteamLibraryDomain` | Keep facades |
| `getAppDownloadInfo`, `isAppInstalled` | Companion | install/download domain | Move |
| `getAppDlc`, `getOwnedAppDlc`, `getMainAppDlcIdsWithoutProperDepotDlcIds`, `filterForDownloadableDepots`, `getMainAppDepots`, `getDownloadableDepots` (both overloads), `checkDlcOwnershipViaPICSBatch` | Companion/catalog logic | `SteamLibraryDomain` + `SteamCatalogManager` + `SteamPicsSyncDomain` | Move logic, keep optional facade |
| `refreshOwnedGamesFromServer`, `getOwnedGames` | Companion wrapper | `SteamLibraryDomain` | Keep facade |
| `getAppDirName`, `resolveExistingAppDir`, `getAppDirPath` | Companion path helper | `SteamPaths`/install utilities | Move |
| `choosePrimaryExe`, `getInstalledExe`, `getLaunchExecutable` | Companion install resolution | `SteamInstallDomain` | Move (facade allowed) |
| `deleteApp` | Companion orchestration | `SteamInstallDomain` + `SteamLibraryDomain` | Move |
| `downloadApp` (all 3 overloads), `completeAppDownload` | Companion orchestration | dedicated download coordinator under install domain | Move |
| `isImageFsInstalled`, `isImageFsInstallable`, `isSteamInstallable`, `isFileInstallable` | Companion installer checks | installer utility/service | Move |
| `fetchFile`, `fetchFileWithFallback`, `downloadImageFs`, `downloadImageFsPatches`, `downloadFile`, `downloadSteam` | Companion network installer logic | installer/downloader service | Move |
| `resolveSteamInputManifestFile`, `loadConfigFromManifest`, `readBuiltInSteamInputTemplate`, `readDownloadedSteamInputTemplate`, `resolveSteamControllerVdfText` | Companion Steam Input config resolution | `SteamInstallDomain` | Move |
| `getWindowsLaunchInfos` | Companion | `SteamInstallDomain` | Move |
| `notifyRunningProcesses`, `beginLaunchApp`, `forceSyncUserFiles`, `closeApp` | Companion wrappers | `SteamSessionDomain`/`SteamCloudStatsDomain` | Keep thin facades |
| `login`, `startLoginWithCredentials`, `startLoginWithQr`, `completeLoginWithAuthTokens`, `stopLoginWithQr`, `logOut` | Companion auth/login | `SteamAccountDomain` | Move `login`; keep API facades |
| `stop` (companion), `clearUserData`, `shouldClearUserDataForLoggedOnFailure`, `clearDatabase`, `cancelLongLivedSteamJobs`, `performLogOffDuties`, `hasActiveOperations`, `isUpdatePending` | Companion lifecycle/session orchestration | split across service shell + account/library/install domains | Move behavior out of companion |
| `generateAchievements`, `clearCachedAchievements`, `getGseSaveDirs`, `syncAchievementsFromGoldberg`, `storeAchievementUnlocks` | Companion wrappers | `SteamCloudStatsDomain` | Keep thin facades |

## Nested Types

| Member | Current | End-state owner | Decision |
|---|---|---|---|
| `AppDownloadListener` (nested class) | Companion helper | install/download coordinator package | Move |
| `AppDownloadListener.onItemAdded` | Nested class | download coordinator helper | Move with class |
| `AppDownloadListener.onDownloadStarted` | Nested class | download coordinator helper | Move with class |
| `AppDownloadListener.onDownloadCompleted` | Nested class | download coordinator helper | Move with class |
| `AppDownloadListener.onDownloadFailed` | Nested class | download coordinator helper | Move with class |
| `AppDownloadListener.onStatusUpdate` | Nested class | download coordinator helper | Move with class |
| `AppDownloadListener.onChunkCompleted` | Nested class | download coordinator helper | Move with class |
| `AppDownloadListener.onDepotCompleted` | Nested class | download coordinator helper | Move with class |
| `FileChanges` (data class) | Companion type | cloud/session sync model package | Move type |

## Migration Phases

1. **Service shell + DAO boundary cleanup** (done)
2. **PICS extraction** (done)
3. **Download extraction** (done)
4. **Auth/session extraction** (done)
   - `_loginResult`, `familyGroupMembers`, `localPersona` moved to `SteamAccountDomain`.
   - `isLoggingOut` managed via domain.
   - Updated callbacks to use domain state.
5. **Facade tightening** (in progress)
   - Remove unused companion wrappers after call-site migration.

## Done So Far

- PICS extraction: `SteamPicsSyncDomain` wired, removed channels/methods from SteamService
- Download extraction: `downloadJobs` in `SteamInstallDomain`, notification delegation
- Auth extraction: Account state in `SteamAccountDomain`, callbacks updated



