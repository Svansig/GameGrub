package app.gamegrub.container.launch.prep

import android.content.Context
import com.winlator.container.Container
import com.winlator.core.TarCompressorUtils
import com.winlator.xenvironment.ImageFs
import java.io.File
import timber.log.Timber

/**
 * Handles optional input-DLL preparation for specific Wine builds.
 *
 * This keeps legacy guard checks and log messages intact while moving
 * launch-prep details out of XServerScreen.
 */
internal object InputDllPreparationCoordinator {
    fun extractArm64ecInputDlls(context: Context, container: Container) {
        val inputAsset = "arm64ec_input_dlls.tzst"
        val imageFs = ImageFs.find(context)
        val wineVersion: String? = container.wineVersion
        Timber.tag("XServerDisplayActivity").d(
            "arm64ec Input DLL Extraction Verification: Container Wine version: $wineVersion",
        )

        if (wineVersion != null && wineVersion.contains("proton-9.0-arm64ec")) {
            val wineFolder = File(imageFs.getWinePath() + "/lib/wine/")
            Timber.tag("XServerDisplayActivity").d(
                "Wine version contains arm64ec. Extracting input dlls to ${wineFolder.path}",
            )
            val success = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context.assets, inputAsset, wineFolder)
            if (!success) {
                Timber.tag("XServerDisplayActivity").d("Failed to extract input dlls")
            }
        } else {
            Timber.tag("XServerDisplayActivity").d(
                "Wine version is not arm64ec, skipping input dlls extraction.",
            )
        }
    }

    fun extractX8664InputDlls(context: Context, container: Container) {
        val imageFs = ImageFs.find(context)
        val wineVersion: String? = container.wineVersion
        Timber.tag("XServerDisplayActivity").d(
            "x86_64 Input DLL Extraction Verification: Container Wine version: $wineVersion",
        )
        if ("proton-9.0-x86_64" == wineVersion) {
            val wineFolder = File(imageFs.getWinePath() + "/lib/wine/")
            Timber.tag("XServerDisplayActivity").d("Extracting input dlls to ${wineFolder.path}")
        } else {
            Timber.tag("XServerDisplayActivity").d(
                "Wine version is not proton-9.0-x86_64, skipping input dlls extraction",
            )
        }
    }
}

