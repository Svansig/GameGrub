# Events Layer

Application-wide event system for cross-component communication.

## Structure

```
events/
├── GameStoreEvent.kt    # Unified event definitions
├── SteamEvent.kt       # Steam-specific events
└── EventDispatcher.kt  # Event bus implementation
```

## Usage

### Publishing Events

```kotlin
XServerRuntime.get().events.emit(GameStoreEvent.LibraryUpdated(GameSource.STEAM))
```

### Subscribing

```kotlin
XServerRuntime.get().events.on<GameStoreEvent.LibraryUpdated> { event ->
    // Handle event
}
```

### Unsubscribing

```kotlin
XServerRuntime.get().events.off<GameStoreEvent.LibraryUpdated, Unit>(handler)
```

## Event Types

- `LibraryUpdated` - Library data changed
- `DownloadProgress` - Download progress update
- `GameLaunched` - Game launch completed
- `AuthStateChanged` - Authentication state changed

## Best Practices

1. Use sparingly - prefer direct injection when possible
2. Unsubscribe in ViewModel `onCleared()`
3. Keep events simple and serializable where possible
