package app.gamegrub.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryViewModelOwnerFilterTest {
    @Test
    fun resolveSteamOwnerIds_includesFamilyAndSelfIds() {
        val actual = resolveSteamOwnerIds(
            familyMembers = listOf(2001, 2002),
            steamUserAccountId = 1001,
            steam3AccountId = 1001L,
        )

        assertEquals(setOf(1001, 2001, 2002), actual)
    }

    @Test
    fun shouldIncludeForOwnerScope_emptyOwnerMetadata_includesItem() {
        val include = shouldIncludeForOwnerScope(
            itemOwnerAccountIds = emptyList(),
            resolvedOwnerIds = setOf(1001),
        )

        assertTrue(include)
    }

    @Test
    fun shouldIncludeForOwnerScope_nonMatchingOwners_excludesItem() {
        val include = shouldIncludeForOwnerScope(
            itemOwnerAccountIds = listOf(5005),
            resolvedOwnerIds = setOf(1001, 2002),
        )

        assertFalse(include)
    }

    @Test
    fun shouldIncludeForSharedFilter_sharedDisabledAndUnknownOwner_includesItem() {
        val include = shouldIncludeForSharedFilter(
            itemOwnerAccountIds = emptyList(),
            currentUserAccountId = 1001,
            sharedFilterEnabled = false,
        )

        assertTrue(include)
    }

    @Test
    fun shouldIncludeForSharedFilter_sharedDisabledAndNotOwned_excludesItem() {
        val include = shouldIncludeForSharedFilter(
            itemOwnerAccountIds = listOf(3003),
            currentUserAccountId = 1001,
            sharedFilterEnabled = false,
        )

        assertFalse(include)
    }
}

