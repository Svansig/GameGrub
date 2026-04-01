package app.gamegrub.db.converters

import android.util.Base64
import androidx.room.TypeConverter

class ByteArrayConverter {

    @TypeConverter
    fun toByteArray(value: String): ByteArray = Base64.decode(value, Base64.DEFAULT)
}
