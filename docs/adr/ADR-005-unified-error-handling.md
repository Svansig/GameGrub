# ADR-005 - Unified Error Handling with GameStoreResult

## Status
Accepted

## Context
Each service was throwing different exception types and handling errors differently. No consistent way for UI to handle errors.

## Decision
Create `GameStoreResult<T>` wrapper that:
- Uses Kotlin `Result` as base
- Adds error codes for classification
- Provides user-safe error messages
- Supports chaining and transformation

```kotlin
sealed class GameStoreError {
    data class Network(val message: String) : GameStoreError()
    data class Auth(val code: AuthErrorCode) : GameStoreError()
    data class Download(val code: DownloadErrorCode) : GameStoreError()
    data class Storage(val message: String) : GameStoreError()
    data class Unknown(val message: String) : GameStoreError()
}
```

## Consequences

### Positive
- Consistent error handling across all services
- User-friendly error messages
- Error codes enable localization
- Easier testing

### Negative
- Requires migration of existing error handling

## Related Tickets
- ARCH-029: Unify Error Handling
