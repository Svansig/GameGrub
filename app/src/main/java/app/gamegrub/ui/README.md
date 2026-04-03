# UI Layer

Compose UI screens and components.

## Structure

```
ui/
├── model/              # ViewModels and UI state
├── screen/            # Screen composables
├── component/         # Reusable UI components
├── theme/             # Material theme
└── enums/            # UI enums
```

## Architecture

- **ViewModels**: Use Hilt injection, expose StateFlow
- **State**: Immutable data classes, use `copy()` for updates
- **Events**: One-way data flow (ViewModel → UI via State, UI → ViewModel via events)

## Key Patterns

### ViewModel Base

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val gateway: MyGateway,
) : ViewModel() {
    
    private val _state = MutableStateFlow(MyState())
    val state: StateFlow<MyState> = _state.asStateFlow()
    
    fun onEvent(event: MyEvent) {
        // Handle UI events
    }
}
```

### State Classes

```kotlin
data class MyState(
    val isLoading: Boolean = false,
    val items: List<Item> = emptyList(),
    val error: String? = null,
)
```

### Event Sealed Class

```kotlin
sealed class MyEvent {
    data class ItemClicked(val id: String) : MyEvent()
    data object RefreshClicked : MyEvent()
}
```
