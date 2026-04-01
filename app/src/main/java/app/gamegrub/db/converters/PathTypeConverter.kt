package app.gamegrub.db.converters

import androidx.room.TypeConverter
import app.gamegrub.enums.PathType

class PathTypeConverter {
    @TypeConverter
    fun fromPathType(pathType: PathType?): String? = pathType?.name

    @TypeConverter
    fun toPathType(value: String?): PathType = PathType.from(value)
}
