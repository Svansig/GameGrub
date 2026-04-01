package app.gamegrub.utils.auth

import android.content.Context
import android.content.pm.PackageManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.gamegrub.PrefManager
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.ProviderException
import java.security.spec.ECGenParameterSpec
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber

/**
 * Handles Android Key Attestation: fetches a server nonce, generates an attested
 * EC key pair in the hardware-backed KeyStore, and extracts the certificate chain
 * for server-side verification.
 */
object KeyAttestationHelper {

    private const val TAG = "KeyAttestationHelper"
    private const val STRONGBOX_UNKNOWN = -1
    private const val STRONGBOX_UNSUPPORTED = 0
    private const val STRONGBOX_SUPPORTED = 1

    @Volatile
    private var hasLoggedAttestationUnavailable = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchNonce(baseUrl: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/attestation/nonce")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        response.use {
            check(it.isSuccessful) { "Nonce request failed: HTTP ${it.code}" }
            val body = it.body?.string() ?: error("Empty nonce response body")
            JSONObject(body).getString("nonce")
        }
    }

    fun generateAttestedKey(context: Context, nonce: String): List<String> {
        val alias = "key_attestation_${System.nanoTime()}"
        val challengeBytes = nonce.toByteArray(Charsets.UTF_8)

        val advertisedStrongBox = isStrongBoxAdvertised(context)
        val cachedStrongBoxState = PrefManager.strongBoxSupportState
        val shouldUseStrongBox = advertisedStrongBox && cachedStrongBoxState != STRONGBOX_UNSUPPORTED

        if (!advertisedStrongBox) {
            PrefManager.strongBoxSupportState = STRONGBOX_UNSUPPORTED
            Timber.tag(TAG).d("StrongBox not advertised by PackageManager; using TEE")
        }

        val usedStrongBox = generateKeyPair(alias, challengeBytes, useStrongBox = shouldUseStrongBox)
        if (usedStrongBox) {
            PrefManager.strongBoxSupportState = STRONGBOX_SUPPORTED
        }

        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val chain = keyStore.getCertificateChain(alias)
        val result = chain.map { Base64.encodeToString(it.encoded, Base64.NO_WRAP) }
        keyStore.deleteEntry(alias)
        return result
    }

    private fun generateKeyPair(alias: String, challenge: ByteArray, useStrongBox: Boolean): Boolean {
        try {
            val specBuilder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAttestationChallenge(challenge)

            if (useStrongBox) {
                specBuilder.setIsStrongBoxBacked(true)
            }

            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                "AndroidKeyStore",
            )
            keyPairGenerator.initialize(specBuilder.build())
            keyPairGenerator.generateKeyPair()
            return useStrongBox
        } catch (e: ProviderException) {
            if (useStrongBox) {
                PrefManager.strongBoxSupportState = STRONGBOX_UNSUPPORTED
                Timber.tag(TAG).w("StrongBox failed, falling back to TEE: ${e.message}")
                return generateKeyPair(alias, challenge, useStrongBox = false)
            } else {
                throw e
            }
        }
    }

    /**
     * Fetches a nonce and generates an attested key, returning both for inclusion
     * in API request bodies. Returns null if attestation is unavailable or fails
     * for any reason, allowing the app to continue without attestation.
     */
    suspend fun getAttestationFields(context: Context, baseUrl: String): Pair<String, List<String>>? {
        return try {
            val nonce = fetchNonce(baseUrl)
            val chain = generateAttestedKey(context, nonce)
            PrefManager.keyAttestationAvailable = true
            Pair(nonce, chain)
        } catch (e: Exception) {
            if (isExpectedAttestationUnavailable(e)) {
                // Many devices/ROMs cannot produce attestable hardware keys; continue without hard failure.
                if (!hasLoggedAttestationUnavailable) {
                    Timber.tag(TAG).w("Key attestation unavailable on this device; continuing without it")
                    hasLoggedAttestationUnavailable = true
                } else {
                    Timber.tag(TAG).d("Key attestation unavailable; continuing without it")
                }
            } else {
                Timber.tag(TAG).e(e, "Key attestation failed, continuing without it")
            }
            PrefManager.keyAttestationAvailable = false
            null
        }
    }

    private fun isExpectedAttestationUnavailable(error: Throwable): Boolean {
        if (error is ProviderException) {
            return true
        }

        val message = error.message?.lowercase().orEmpty()
        return message.contains("attestation") ||
            message.contains("failed to generate key pair") ||
            message.contains("strongbox")
    }

    private fun isStrongBoxAdvertised(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
    }
}
