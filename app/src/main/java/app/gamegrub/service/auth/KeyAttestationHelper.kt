package app.gamegrub.service.auth

import android.content.Context
import android.content.pm.PackageManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.gamegrub.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.ProviderException
import java.security.spec.ECGenParameterSpec
import java.util.concurrent.TimeUnit

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

    /**
     * Backward-compatible API used by config/compatibility services.
     * Returns nonce + encoded attestation chain, or null when attestation cannot be produced.
     */
    suspend fun getAttestationFields(
        context: Context,
        baseUrl: String,
    ): Pair<String, List<String>>? = withContext(Dispatchers.IO) {
        if (!isAttestationSupported(context)) {
            return@withContext null
        }

        try {
            val nonce = fetchNonce(baseUrl)
            val keyAlias = "GameNativeAttestationKey"

            val keyGen = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                "AndroidKeyStore",
            )
            val spec = KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setAttestationChallenge(nonce.toByteArray())
                .build()
            keyGen.initialize(spec)
            keyGen.generateKeyPair()

            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val certificateChain = keyStore.getCertificateChain(keyAlias)
            if (certificateChain.isNullOrEmpty()) {
                return@withContext null
            }

            val encodedChain = certificateChain.map {
                Base64.encodeToString(it.encoded, Base64.NO_WRAP)
            }

            nonce to encodedChain
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to build attestation fields")
            null
        }
    }

    suspend fun fetchNonce(baseUrl: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/attestation/nonce")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        response.use {
            check(it.isSuccessful) { "Nonce request failed: HTTP ${it.code}" }
            val body = it.body.string()
            val json = JSONObject(body)
            json.getString("nonce")
        }
    }

    suspend fun attest(context: Context, baseUrl: String): AttestationResult = withContext(Dispatchers.IO) {
        if (!isAttestationSupported(context)) {
            return@withContext AttestationResult.Unsupported
        }

        val nonce = fetchNonce(baseUrl)
        val keyAlias = "GameNativeAttestationKey"

        try {
            val keyGen = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                "AndroidKeyStore",
            )
            val spec = KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setAttestationChallenge(nonce.toByteArray())
                .build()
            keyGen.initialize(spec)
            keyGen.generateKeyPair()
        } catch (e: ProviderException) {
            Timber.tag(TAG).e(e, "Key attestation failed - Provider error")
            logAttestationUnavailable()
            return@withContext AttestationResult.Failed(e.message ?: "Provider error")
        }

        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val certificateChain = keyStore.getCertificateChain(keyAlias)

        if (certificateChain.isNullOrEmpty()) {
            return@withContext AttestationResult.Failed("Empty certificate chain")
        }

        val encoded = certificateChain.joinToString("\n") {
            Base64.encodeToString(it.encoded, Base64.NO_WRAP)
        }

        val requestBody = JSONObject().apply {
            put("attestation", encoded)
            put("nonce", nonce)
        }.toString()

        val request = Request.Builder()
            .url("$baseUrl/api/attestation/verify")
            .post(requestBody.toRequestBody(Constants.Protocol.MIME_APPLICATION_JSON.toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) {
                Timber.tag(TAG).e("Attestation verification failed: HTTP ${it.code}")
                return@withContext AttestationResult.Failed("HTTP ${it.code}")
            }

            val body = it.body.string()
            val json = JSONObject(body)

            if (json.has("error")) {
                val error = json.getString("error")
                Timber.tag(TAG).e("Attestation error: $error")
                AttestationResult.Failed(error)
            } else {
                AttestationResult.Success
            }
        }
    }

    private fun isAttestationSupported(context: Context): Boolean {
        return try {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        } catch (e: Exception) {
            false
        }
    }

    private fun logAttestationUnavailable() {
        if (!hasLoggedAttestationUnavailable) {
            hasLoggedAttestationUnavailable = true
            Timber.tag(TAG).w("Device does not support hardware attestation")
        }
    }

    fun clearKey() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.deleteEntry("GameNativeAttestationKey")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to clear attestation key")
        }
    }
}

sealed class AttestationResult {
    data object Success : AttestationResult()
    data object Unsupported : AttestationResult()
    data class Failed(val error: String) : AttestationResult()
}
