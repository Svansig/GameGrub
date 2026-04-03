# Result Layer

Unified error handling with `GameStoreResult`.

## Usage

```kotlin
suspend fun doSomething(): GameStoreResult<String> {
    return runGameStoreCatching {
        // Code that might throw
        riskyOperation()
    }
}

// Usage
val result = doSomething()
result.onSuccess { value ->
    // Handle success
}.onError { error ->
    // Handle error
    log(error.message)
}

// Or with getOrElse
val value = result.getOrElse { error ->
    defaultValue
}
```

## Error Types

```kotlin
enum class ErrorCode {
    NETWORK_ERROR,
    AUTH_ERROR,
    DOWNLOAD_ERROR,
    LAUNCH_ERROR,
    NOT_FOUND_ERROR,
    PERMISSION_ERROR,
    UNKNOWN_ERROR,
}
```

## Creating Errors

```kotlin
GameStoreError(
    code = ErrorCode.NETWORK_ERROR,
    message = "Failed to connect",
    source = GameSource.STEAM,
    recoverable = true,
)
```
