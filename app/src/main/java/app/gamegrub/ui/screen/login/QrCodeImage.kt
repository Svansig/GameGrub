package app.gamegrub.ui.screen.login

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import app.gamegrub.ui.theme.GameGrubTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Displays a QR code for [content] at the specified [size].
 *
 * Generates the QR code asynchronously with a 2-module quiet zone,
 * error correction level M, and high-contrast black-on-white colors.
 * Shows a loading indicator while generating.
 */
@Composable
fun QrCodeImage(
    modifier: Modifier = Modifier,
    content: String,
    size: Dp,
) {
    val qrBitmap = rememberQrBitmap(content = content, size = size)

    Crossfade(
        modifier = Modifier,
        targetState = qrBitmap,
    ) { bitmap ->
        Box(
            modifier = modifier
                .size(size)
                .background(androidx.compose.ui.graphics.Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                val bitmapPainter = remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) }
                Image(
                    painter = bitmapPainter,
                    contentDescription = "QR code for Steam login",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.size(size),
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(92.dp))
            }
        }
    }
}

/**
 * Generates and remembers a QR code bitmap for the given [content] and [size].
 * Regenerates when content or size changes, running on IO dispatcher.
 */
@Composable
private fun rememberQrBitmap(content: String, size: Dp): Bitmap? {
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(content, sizePx) {
        bitmap = null
        bitmap = withContext(Dispatchers.IO) {
            generateQrBitmap(content, sizePx)
        }
    }

    return bitmap
}

/** Generates a QR code [Bitmap] with error correction level M and 2-module margin. */
private fun generateQrBitmap(content: String, sizePx: Int): Bitmap? {
    val hints = mapOf(
        EncodeHintType.MARGIN to 2,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
    )

    val matrix = try {
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    } catch (e: WriterException) {
        return null
    }

    return matrix.toBitmap()
}

/** Converts a ZXing [BitMatrix] to an Android [Bitmap] with black modules on white background. */
private fun BitMatrix.toBitmap(): Bitmap {
    val width = width
    val height = height
    val pixels = IntArray(width * height)

    for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
            pixels[offset + x] = if (get(x, y)) Color.BLACK else Color.WHITE
        }
    }

    return createBitmap(width, height).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_QrCodeImage() {
    GameGrubTheme {
        Surface {
            QrCodeImage(Modifier, "Hello World", 256.dp)
        }
    }
}
