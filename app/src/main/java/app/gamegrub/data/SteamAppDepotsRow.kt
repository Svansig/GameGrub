package app.gamegrub.data

import androidx.room.ColumnInfo

/**
 * Projection for reading only the serialized depots column from steam_app.
 */
data class SteamAppDepotsRow(
    @ColumnInfo("depots")
    val depots: Map<Int, DepotInfo> = emptyMap(),
)

