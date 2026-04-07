# Update Package

Handles app update checking and installation.

## Structure

```
update/
├── UpdateChecker.kt    # Checks for available updates
└── UpdateInstaller.kt  # Installs downloaded updates
```

## Update Flow

1. `UpdateChecker` fetches update info from `Constants.Api.UPDATE_CHECK_URL`
2. If update available, notifies UI and can trigger download
3. `UpdateInstaller` handles APK installation via system installer

## Key Entry Points

- `UpdateChecker.checkForUpdate()` - Check if update available
- `UpdateInstaller.installUpdate()` - Install downloaded update APK