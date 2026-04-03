package app.gamegrub.data

data class GameFilter(
    val source: GameSource? = null,
    val isInstalled: Boolean? = null,
    val searchQuery: String = "",
    val sortBy: SortOption = SortOption.NAME_ASC,
    val genre: String? = null,
)

enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    LAST_PLAYED,
    PLAY_TIME,
    INSTALL_SIZE,
    RELEASE_DATE,
}
