# Container Layer

Manages Wine/Proton containers for running Windows games on Android.

## Structure

```
container/
├── ContainerManager.kt           # Main container lifecycle
├── launch/
│   ├── command/                 # Launch command builders
│   │   ├── BaseLaunchCommandBuilder.kt
│   │   ├── SteamLaunchCommandBuilder.kt
│   │   ├── GogLaunchCommandBuilder.kt
│   │   ├── EpicLaunchCommandBuilder.kt
│   │   └── AmazonLaunchCommandBuilder.kt
│   ├── manager/                 # Launch orchestration
│   ├── prep/                   # Pre-launch preparation
│   ├── env/                    # Environment setup
│   └── unpack/                # Unpack coordination
└── manager/                    # Container runtime
```

## Launch Flow

1. `StoreLaunchCommandResolver` resolves the appropriate builder
2. Builder creates command based on game source
3. `LaunchPreparationCoordinator` prepares container
4. `ContainerManager` starts the container

## Command Builders

All builders extend `BaseLaunchCommandBuilder`:

```kotlin
internal object SteamLaunchCommandBuilder : BaseLaunchCommandBuilder() {
    override val gameSource: GameSource = GameSource.STEAM

    override fun buildStoreCommand(context: LaunchCommandContext): String? {
        // Build Steam-specific launch command
    }
}
```
