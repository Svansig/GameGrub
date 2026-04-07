# Launch Package

Entry points and coordination for game launching.

## Structure

```
launch/
├── IntentLaunchManager.kt    # Handles external intent-based launch requests
├── GameLaunchResolution.kt   # Resolves game appId from various sources
└── GameLaunchTelemetry.kt    # Tracks launch metrics and events
```

## Intent Launch Flow

1. External app/intent triggers launch via `IntentLaunchManager`
2. `IntentLaunchManager` parses the launch request and stores temporary override
3. `GameLaunchResolution` resolves the final game appId
4. `GameLaunchTelemetry` records the launch for analytics

## Key Entry Points

- `IntentLaunchManager.handleIntent()` - Process incoming launch intents
- `GameLaunchResolution.resolveGameAppId()` - Resolve game ID from various sources
- `GameLaunchTelemetry.trackLaunch()` - Record launch event