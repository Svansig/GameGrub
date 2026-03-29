package app.gamegrub.data

import app.gamegrub.db.serializers.OsEnumSetSerializer
import app.gamegrub.enums.OS
import app.gamegrub.enums.OSArch
import java.util.EnumSet
import kotlinx.serialization.Serializable

@Serializable
data class LaunchInfo(
    val executable: String,
    val workingDir: String,
    val description: String,
    val type: String,
    @Serializable(with = OsEnumSetSerializer::class)
    val configOS: EnumSet<OS>,
    val configArch: OSArch,
)
