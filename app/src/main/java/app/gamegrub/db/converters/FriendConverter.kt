package app.gamegrub.db.converters

import androidx.room.TypeConverter
import `in`.dragonbra.javasteam.types.SteamID
import java.util.Date

class FriendConverter {

    @TypeConverter
    fun fromTimestamp(value: Long): Date = Date(value)

    @TypeConverter
    fun dateToTimestamp(date: Date): Long = date.time

    @TypeConverter
    fun toSteamID(value: Long): SteamID = SteamID(value)
}
