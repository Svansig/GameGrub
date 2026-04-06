package app.gamegrub.service.steam.domain

/**
 * Domain-level constants for Steam service operations.
 *
 * Sentinel and capacity values used across Steam domain classes. Defined here
 * so domain objects do not need to import [app.gamegrub.service.steam.SteamService]
 * (which would invert the intended dependency direction).
 *
 * The companion-object copies in [app.gamegrub.service.steam.SteamService] are kept
 * for external call sites that are not yet migrated; they should eventually delegate
 * here or be removed. See tickets SRV-007, SRV-015.
 */

/** Maximum number of PICS requests to batch in a single chunk. */
const val DOMAIN_MAX_PICS_BUFFER: Int = 256

/** Sentinel value indicating an invalid/unknown Steam App ID. */
const val DOMAIN_INVALID_APP_ID: Int = Int.MAX_VALUE

/** Sentinel value indicating an invalid/unknown Steam Package ID. */
const val DOMAIN_INVALID_PKG_ID: Int = Int.MAX_VALUE

