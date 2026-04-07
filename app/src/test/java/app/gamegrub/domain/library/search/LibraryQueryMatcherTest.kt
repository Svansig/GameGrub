package app.gamegrub.domain.library.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryQueryMatcherTest {
    @Test
    fun matches_plainContains_returnsTrue() {
        val matches = LibraryQueryMatcher.matches("Sea of Stars", "sea")

        assertTrue(matches)
    }

    @Test
    fun matches_accentInsensitive_returnsTrue() {
        val matches = LibraryQueryMatcher.matches("Pokémon", "pokemon")

        assertTrue(matches)
    }

    @Test
    fun matches_nonMatching_returnsFalse() {
        val matches = LibraryQueryMatcher.matches("Factorio", "hades")

        assertFalse(matches)
    }
}

