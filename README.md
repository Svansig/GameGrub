# GameGrub

This repository is a **Slop Fork of GameNative**.

GameGrub is an Android app that lets you access your supported PC-store libraries, install titles, tune compatibility settings, and launch games on Android.

It is built by hobbyists who want to make portable PC gaming more practical, more fun, and a lot less "stuck at your desk."

## What This App Is

- A community-maintained game launcher/client for Android, built by hobbyists.
- Built for users who want to run games they already own from supported storefronts.
- Focused on practical playability: install, launch, troubleshoot, and tune each game until it behaves.
- Intended for legally owned games only.

## Supported Stores

- **Steam**: Primary and most actively used path.
- **GOG**: Supported.
- **Epic**: Supported.
- **Amazon**: In project scope, but support maturity may vary by release and workflow.

Store integration and compatibility are actively evolving. Results can vary by title, account state, and device.

## Capabilities

### 1) Library and Game Management

- Browse supported store libraries from within the app.
- Launch installed games directly from game pages.
- Run common maintenance actions: update, verify files, and uninstall.
- Open a game's store page from in-app options when available.

Think of this as your portable control center for "what do I play next?"

### 2) Download and Install Workflows

- Install games from supported libraries.
- Manage install lifecycle actions in-game options (install/update/verify/uninstall).
- Handle game content management workflows, including DLC/content operations where available.

### 3) Cloud Save Controls

- Use cloud-sync workflows on supported games/stores.
- Trigger force cloud sync manually when needed.
- Use advanced recovery actions such as force download of remote saves or force upload of local saves.
- Browse online saves where the workflow is available.

### 4) Storage and Device Integration

- Move installed data between internal and external storage from game options.
- Create launcher shortcuts to jump straight into specific games from your Android home screen.

### 5) Per-Game Compatibility Tuning

- Access per-game configuration options for compatibility tuning.
- Apply known configs, import/export configs, and reset to defaults when troubleshooting.
- Use graphics test tools and container controls to isolate game-specific issues.

Some titles launch cleanly, others need a bit of tuning. The tools are here for both.

### 6) Custom Games and Metadata

- Add and manage custom game entries in the library.
- Optionally fetch game artwork/images through SteamGridDB workflows (when configured).

### 7) Troubleshooting-Oriented Options

- Built-in actions are designed to recover common failures quickly (verify files, sync saves, reset config).
- Useful for diagnosing launch failures, save-sync problems, and bad config states without reinstalling the whole app.
- Community compatibility guidance is improved using anonymized hardware/configuration run signals; see `Privacy and Data Handling` below.

Capability availability depends on the game, store backend behavior, and current release maturity.

## Device Requirements

- Android 13 or newer (project minimum SDK is 33).
- Enough free storage for game files and compatibility layers.
- Stable network for authentication, downloads, and cloud workflows.

## Quick Start (End Users)

1. Open the repository [Releases](../../releases) page and download the latest APK.
2. Install the APK on your Android device.
3. Sign in to a supported store account inside the app.
4. Install a game from your library.
5. Launch the game and tune settings if needed.

## What To Expect

- Game compatibility is title-specific and device-specific.
- Some games work right away; others need a few rounds of per-game tuning.
- GPU, driver stack, storage speed, and anti-cheat requirements can materially affect results.

## Troubleshooting Quick Path

If a game fails to launch or behaves incorrectly:

1. Run `Verify files` from game options.
2. Re-check game config and try known/default config paths.
3. For save issues, use cloud actions (`Force cloud sync`, `Force download remote saves`, `Force upload local saves`) as appropriate.
4. If still blocked, gather details and ask in Discord.

Short version: verify, tweak, sync, then ask the community if it is still being stubborn.

## Privacy and Data Handling

- Account/session material is stored locally on your device.
- Your device communicates with platform services directly for auth/library operations.
- The project may collect limited non-personal technical metadata to improve compatibility and performance.
- This includes anonymized game-run signals across hardware categories and configuration choices so we can learn what setups work best.
- Findings are intended to be shared back as aggregated compatibility guidance for everyone, not as personal user profiles.

Read details in `PrivacyPolicy/README.md`.

## Support

- For support, use this project's community channels (Discord).
- Please do not direct support requests for this app to the GameNative team; this project is separate.

## Legal and Safety Notes

- Use this app only for games you legally own.
- Respect platform terms of service for Steam, GOG, Epic, and Amazon.
- This project is unofficial and not affiliated with those storefronts.

## Acknowledgements

Special thanks to the projects this app builds on top of:

- **GameNative**
- **Pluvia**
- **JavaSteam**
- **Winlator**

And special thanks to everyone who runs GameGrub, tests different setups, and helps make PC games everywhere more real every day.

## License

- `GPL-3.0`: see `LICENSE`.

## Contributor Appendix (Short)

- Build/workflow guide: `AGENTS.md`
- Work tracking: `todo/INDEX.md`, `todo/README.md`
- Architecture/refactor docs: `docs/`

Optional SteamGridDB setup for development builds:

```properties
STEAMGRIDDB_API_KEY=your_api_key_here
```

API key source: https://www.steamgriddb.com/profile/preferences
