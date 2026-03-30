package app.gamegrub.service.steam.managers

import app.gamegrub.utils.steam.SteamTokenHelper
import com.auth0.android.jwt.JWT
import com.winlator.core.FileUtils
import com.winlator.core.TarCompressorUtils
import com.winlator.core.WineRegistryEditor
import com.winlator.xenvironment.ImageFs
import com.winlator.xenvironment.components.GuestProgramLauncherComponent
import `in`.dragonbra.javasteam.types.KeyValue
import java.io.File
import java.nio.file.Files
import java.util.zip.CRC32
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import timber.log.Timber

private const val NULL_CHAR = '\u0000'
private const val TOKEN_EXPIRE_TIME = 86400L

data class SteamSessionContext(
    val steamId64: String,
    val account: String,
    val refreshToken: String,
    val accessToken: String? = null,
    val personaName: String = account,
)

@Singleton
class SteamSessionFilesManager @Inject constructor() {

    private fun hdr(login: String): String {
        val crc = CRC32()
        crc.update(login.toByteArray())
        return "${crc.value.toString(16)}1"
    }

    private fun execCommand(
        command: String,
        guestProgramLauncherComponent: GuestProgramLauncherComponent,
    ): String {
        return guestProgramLauncherComponent.execShellCommand(command, false)
    }

    private fun killWineServer(guestProgramLauncherComponent: GuestProgramLauncherComponent) {
        try {
            execCommand("wineserver -k", guestProgramLauncherComponent)
        } catch (e: Exception) {
            Timber.tag("SteamSessionFilesManager").e("Failed to kill wineserver: ${e.message}")
        }
    }

    private fun encryptToken(
        login: String,
        token: String,
        imageFs: ImageFs,
        guestProgramLauncherComponent: GuestProgramLauncherComponent,
    ): String {
        return execCommand(
            "wine ${imageFs.rootDir}/opt/apps/steam-token.exe encrypt $login $token",
            guestProgramLauncherComponent,
        )
    }

    private fun decryptToken(
        login: String,
        vdfValue: String,
        imageFs: ImageFs,
        guestProgramLauncherComponent: GuestProgramLauncherComponent,
    ): String {
        return execCommand(
            "wine ${imageFs.rootDir}/opt/apps/steam-token.exe decrypt $login $vdfValue",
            guestProgramLauncherComponent,
        )
    }

    private fun createConfigVdf(
        steamId: String,
        login: String,
        token: String,
    ): String {
        val header = hdr(login)
        val minMTBF = 1000000000L
        val maxMTBF = 2000000000L
        var mtbf = kotlin.random.Random.nextLong(minMTBF, maxMTBF)
        var encoded = ""

        do {
            try {
                encoded = SteamTokenHelper.obfuscate("$token$NULL_CHAR".toByteArray(), mtbf)
            } catch (_: Exception) {
                mtbf = kotlin.random.Random.nextLong(minMTBF, maxMTBF)
            }
        } while (encoded == "")

        return """
            "InstallConfigStore"
            {
                "Software"
                {
                    "Valve"
                    {
                        "Steam"
                        {
                            "MTBF"        "$mtbf"
                            "ConnectCache"
                            {
                                "$header"        "$encoded$NULL_CHAR"
                            }
                            "Accounts"
                            {
                                "$login"
                                {
                                    "SteamID"        "$steamId"
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
    }

    private fun createLocalVdf(
        login: String,
        token: String,
        imageFs: ImageFs,
        guestProgramLauncherComponent: GuestProgramLauncherComponent,
    ): String {
        val header = hdr(login)
        val encoded = encryptToken(
            login = login,
            token = token,
            imageFs = imageFs,
            guestProgramLauncherComponent = guestProgramLauncherComponent,
        )

        return """
            "MachineUserConfigStore"
            {
                "Software"
                {
                    "Valve"
                    {
                        "Steam"
                        {
                            "ConnectCache"
                            {
                                "$header"        "$encoded"
                            }
                        }
                    }
                }
            }
        """.trimIndent()
    }

    fun buildLoginUsersVdfOauth(
        steamId64: String,
        account: String,
        @Suppress("UNUSED") refreshToken: String,
        @Suppress("UNUSED") accessToken: String? = null,
        personaName: String = account,
    ): String {
        val epoch = System.currentTimeMillis() / 1_000

        return buildString {
            appendLine("\"users\"")
            appendLine("{")
            appendLine("    \"$steamId64\"")
            appendLine("    {")
            appendLine("        \"AccountName\"          \"$account\"")
            appendLine("        \"PersonaName\"          \"$personaName\"")
            appendLine("        \"RememberPassword\"     \"1\"")
            appendLine("        \"WantsOfflineMode\"     \"0\"")
            appendLine("        \"SkipOfflineModeWarning\"     \"0\"")
            appendLine("        \"AllowAutoLogin\"       \"1\"")
            appendLine("        \"MostRecent\"           \"1\"")
            appendLine("        \"Timestamp\"            \"$epoch\"")
            appendLine("    }")
            appendLine("}")
        }
    }

    fun applyAutoLoginUserChanges(
        imageFs: ImageFs,
        session: SteamSessionContext,
    ) {
        val vdfFileText = buildLoginUsersVdfOauth(
            steamId64 = session.steamId64,
            account = session.account,
            refreshToken = session.refreshToken,
            accessToken = session.accessToken,
            personaName = session.personaName,
        )
        val steamConfigDir = File(imageFs.wineprefix, "drive_c/Program Files (x86)/Steam/config")
        try {
            File(steamConfigDir, "loginusers.vdf").writeText(vdfFileText)
            val rootDir = imageFs.rootDir
            val userRegFile = File(rootDir, ImageFs.WINEPREFIX + "/user.reg")
            val steamRoot = "C:\\Program Files (x86)\\Steam"
            val steamExe = "$steamRoot\\steam.exe"
            val hkcu = "Software\\Valve\\Steam"
            WineRegistryEditor(userRegFile).use { reg ->
                reg.setStringValue("Software\\Valve\\Steam", "AutoLoginUser", session.account)
                reg.setStringValue(hkcu, "SteamExe", steamExe)
                reg.setStringValue(hkcu, "SteamPath", steamRoot)
                reg.setStringValue(hkcu, "InstallPath", steamRoot)
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not apply Steam auto-login session file changes")
        }
    }

    fun runTokenSessionConfigPhases(
        session: SteamSessionContext,
        imageFs: ImageFs,
        guestProgramLauncherComponent: GuestProgramLauncherComponent,
    ) {
        runPhase1SteamConfig(
            session = session,
            imageFs = imageFs,
            guestProgramLauncherComponent = guestProgramLauncherComponent,
        )
    }

    fun runPhase1SteamConfig(
        session: SteamSessionContext,
        imageFs: ImageFs,
        guestProgramLauncherComponent: GuestProgramLauncherComponent,
    ) {
        val steamConfigDir = File(imageFs.wineprefix, "drive_c/Program Files (x86)/Steam/config").toPath()
        Files.createDirectories(steamConfigDir)

        val configVdfPath = steamConfigDir.resolve("config.vdf")
        var shouldWriteConfig = true
        var shouldProcessPhase2 = false

        if (Files.exists(configVdfPath)) {
            val vdfContent = FileUtils.readString(configVdfPath.toFile())
            if (vdfContent.contains("ConnectCache")) {
                val vdfData = KeyValue.loadFromString(vdfContent)!!
                val mtbf = vdfData["Software"]["Valve"]["Steam"]["MTBF"].value
                val connectCacheValue = vdfData["Software"]["Valve"]["Steam"]["ConnectCache"][hdr(session.account)].value

                if (mtbf != null && connectCacheValue != null) {
                    try {
                        val dToken = SteamTokenHelper.deobfuscate(connectCacheValue.trimEnd(NULL_CHAR), mtbf.toLong())
                            .trimEnd(NULL_CHAR)
                        shouldWriteConfig = JWT(dToken).isExpired(TOKEN_EXPIRE_TIME)
                    } catch (_: Exception) {
                        shouldWriteConfig = true
                    }
                } else {
                    if (mtbf == null && connectCacheValue == null) {
                        shouldWriteConfig = true
                    } else if (mtbf != null) {
                        shouldWriteConfig = false
                        shouldProcessPhase2 = true
                    }
                }
            } else if (vdfContent.contains("MTBF")) {
                shouldWriteConfig = false
                shouldProcessPhase2 = true
            }
        }

        if (shouldWriteConfig) {
            Files.write(
                steamConfigDir.resolve("config.vdf"),
                createConfigVdf(
                    steamId = session.steamId64,
                    login = session.account,
                    token = session.refreshToken,
                ).toByteArray(),
            )

            FileUtils.chmod(File(steamConfigDir.absolutePathString(), "loginusers.vdf"), 505)
            FileUtils.chmod(File(steamConfigDir.absolutePathString(), "config.vdf"), 505)

            val localSteamDir = File(imageFs.wineprefix, "drive_c/users/${ImageFs.USER}/AppData/Local/Steam").toPath()
            localSteamDir.createDirectories()
            if (localSteamDir.resolve("local.vdf").exists()) {
                Files.delete(localSteamDir.resolve("local.vdf"))
            }
        } else if (shouldProcessPhase2) {
            runPhase2LocalConfig(
                session = session,
                imageFs = imageFs,
                guestProgramLauncherComponent = guestProgramLauncherComponent,
            )
        }
    }

    fun runPhase2LocalConfig(
        session: SteamSessionContext,
        imageFs: ImageFs,
        guestProgramLauncherComponent: GuestProgramLauncherComponent,
    ) {
        try {
            val extractDir = File(imageFs.rootDir, "/opt/apps/")
            Files.createDirectories(extractDir.toPath())
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, File(imageFs.filesDir, "steam-token.tzst"), extractDir)

            val localSteamDir = File(imageFs.wineprefix, "drive_c/users/${ImageFs.USER}/AppData/Local/Steam").toPath()
            Files.createDirectories(localSteamDir)

            if (localSteamDir.resolve("local.vdf").exists()) {
                val vdfContent = FileUtils.readString(localSteamDir.resolve("local.vdf").toFile())
                val vdfData = KeyValue.loadFromString(vdfContent)!!
                val connectCacheValue = vdfData["Software"]["Valve"]["Steam"]["ConnectCache"][hdr(session.account)].value
                if (connectCacheValue != null) {
                    try {
                        val dToken = decryptToken(
                            login = session.account,
                            vdfValue = connectCacheValue.trimEnd(NULL_CHAR),
                            imageFs = imageFs,
                            guestProgramLauncherComponent = guestProgramLauncherComponent,
                        )
                        val savedJWT = JWT(dToken)
                        if (!savedJWT.isExpired(TOKEN_EXPIRE_TIME)) {
                            return
                        }
                    } catch (e: Exception) {
                        Timber.tag("SteamSessionFilesManager").d("An unexpected error occurred: ${e.message}")
                    }
                }
            }

            Files.write(
                localSteamDir.resolve("local.vdf"),
                createLocalVdf(
                    login = session.account,
                    token = session.refreshToken,
                    imageFs = imageFs,
                    guestProgramLauncherComponent = guestProgramLauncherComponent,
                ).toByteArray(),
            )

            killWineServer(guestProgramLauncherComponent)
            FileUtils.chmod(File(localSteamDir.absolutePathString(), "local.vdf"), 505)
        } catch (e: Exception) {
            Timber.tag("SteamSessionFilesManager").d("An unexpected error occurred: ${e.message}")
        }
    }

    fun setupRealSteamSessionFiles(
        session: SteamSessionContext,
        imageFs: ImageFs,
        guestProgramLauncherComponent: GuestProgramLauncherComponent,
    ) {
        applyAutoLoginUserChanges(
            imageFs = imageFs,
            session = session,
        )

        runTokenSessionConfigPhases(
            session = session,
            imageFs = imageFs,
            guestProgramLauncherComponent = guestProgramLauncherComponent,
        )
    }
}
