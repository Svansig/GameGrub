package app.gamegrub.domain.library.search

import app.gamegrub.utils.general.unaccent

/**
 * Performs library query matching with accent-insensitive fallback.
 */
object LibraryQueryMatcher {
    fun matches(gameName: String, searchQuery: String): Boolean {
        return gameName.contains(searchQuery, ignoreCase = true) ||
            gameName.unaccent().contains(searchQuery, ignoreCase = true)
    }
}

