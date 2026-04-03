package app.gamegrub.db.converters

import androidx.room.TypeConverter
import app.gamegrub.data.GameSource

class GameSourceConverter {
    @TypeConverter
    fun fromGameSource(source: GameSource): String {
        return source.name
    }

    @TypeConverter
    fun toGameSource(value: String): GameSource {
        return GameSource.valueOf(value)
    }
}
