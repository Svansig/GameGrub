package app.gamegrub.domain.library.policy

import app.gamegrub.enums.AppType

/**
 * Resolves all Steam account ids that should be treated as local ownership identities.
 *
 * Why: ownership metadata can arrive in different id fields; this normalization allows
 * downstream filters to use one consistent lookup set.
 */
fun resolveSteamOwnerIds(
    familyMembers: List<Int>,
    steamUserAccountId: Int,
    steam3AccountId: Long?,
): Set<Int> {
    return buildSet {
        addAll(familyMembers.filter { it > 0 })
        if (steamUserAccountId > 0) {
            add(steamUserAccountId)
        }
        val steam3IdAsInt = steam3AccountId?.toInt() ?: 0
        if (steam3IdAsInt > 0) {
            add(steam3IdAsInt)
        }
    }
}

/**
 * Returns whether a Steam item passes owner scoping.
 *
 * Why: missing owner metadata should not hide likely-owned games.
 */
fun shouldIncludeForOwnerScope(itemOwnerAccountIds: List<Int>, resolvedOwnerIds: Set<Int>): Boolean {
    if (resolvedOwnerIds.isEmpty()) {
        return true
    }
    if (itemOwnerAccountIds.isEmpty()) {
        return true
    }
    return itemOwnerAccountIds.any { it in resolvedOwnerIds }
}

/**
 * Returns whether an item passes the shared-library toggle semantics.
 *
 * Why: when shared is disabled, the user expects only own titles; unknown owner data fails open.
 */
fun shouldIncludeForSharedFilter(
    itemOwnerAccountIds: List<Int>,
    currentUserAccountId: Int,
    sharedFilterEnabled: Boolean,
): Boolean {
    if (sharedFilterEnabled) {
        return true
    }
    if (currentUserAccountId == 0) {
        return true
    }
    if (itemOwnerAccountIds.isEmpty()) {
        return true
    }
    return itemOwnerAccountIds.contains(currentUserAccountId)
}

/**
 * Returns whether an app type is accepted by the active type filter set.
 */
fun shouldIncludeForTypeFilter(
    itemType: AppType,
    allowedTypes: Set<AppType>,
): Boolean {
    return allowedTypes.isEmpty() || itemType in allowedTypes
}

/**
 * Indicates whether ownership/type/shared filters should be bypassed for installed entries.
 *
 * Why: installed tab should remain stable even when owner metadata is incomplete.
 */
fun shouldBypassSteamFiltersForInstalledTab(
    appId: Int,
    installedTabActive: Boolean,
    downloadedAppIds: Set<Int>,
): Boolean {
    return installedTabActive && appId in downloadedAppIds
}

