package app.gamegrub.service.auth

import android.content.Context
import android.content.pm.PackageManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.gamegrub.Constants
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.ProviderException
import java.security.spec.ECGenParameterSpec
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber

object KeyAttestationHelper {

    private const val TAG = "KeyAttestationHelper"
    private const val STRONGBOX_UNKNOWN = -1
    private const val STRONGBOX_UNSUPPORTED = 0
    private const val STRONGBOX_SUPPORTED = 1

    // Why: provider failures are frequently device/firmware specific and can repeat for long periods.
    // A long cooldown avoids hammering keystore and keeps background API paths responsive.
    private const val PROVIDER_FAILURE_RETRY_COOLDOWN_MS = 6 * 60 * 60 * 1000L

    // Why: generic failures (network races, transient keystore state) can recover quickly,
    // so we still back off, but for a shorter window.
    private const val GENERIC_FAILURE_RETRY_COOLDOWN_MS = 30 * 60 * 1000L

    @Volatile
    private var hasLoggedAttestationUnavailable = false

    @Volatile
    // Why: this is an in-memory brake only; we do not persist it because attestation support can
    // change across reboot/OS update and we want fresh evaluation on next process start.
    private var nextAttestationAttemptAtMs: Long = 0L

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Backward-compatible API used by config/compatibility services.
     * Returns nonce + encoded attestation chain, or null when attestation cannot be produced.
     *
     * Why: callers should treat attestation as optional enrichment, not a hard dependency.
     * Returning null keeps requests functional on devices where keystore attestation is flaky.
     */
    suspend fun getAttestationFields(
        context: Context,
        baseUrl: String,
    ): Pair<String, List<String>>? = withContext(Dispatchers.IO) {
        // Why: attestation failures can be expensive and noisy. If we're inside cooldown,
        // skip work early and allow caller to proceed without attestation payload.
        if (!shouldAttemptAttestation(context)) {
            return@withContext null
        }

        try {
            val nonce = fetchNonce(baseUrl)
            // Why: isolate key generation + chain extraction in one place so both attestation
            // entry points share the same keystore behavior and failure semantics.
            val certificateChain = generateAttestationCertificateChain(nonce)
            if (certificateChain.isNullOrEmpty()) {
                // Why: no chain means there is nothing verifiable server-side; send no fields
                // instead of partial/invalid payload.
                return@withContext null
            }

            val encodedChain = certificateChain.map {
                Base64.encodeToString(it.encoded, Base64.NO_WRAP)
            }

            nonce to encodedChain
        } catch (e: ProviderException) {
            // Why: ProviderException is often deterministic per device state, so retrying every
            // request causes repeated failures and can trigger API throttling.
            nextAttestationAttemptAtMs = System.currentTimeMillis() + PROVIDER_FAILURE_RETRY_COOLDOWN_MS
            Timber.tag(TAG).w(e, "Hardware key attestation unavailable; temporarily disabling attestation")
            logAttestationUnavailable()
            null
        } catch (e: Exception) {
            // Why: unknown failures may recover sooner, but still should not be retried instantly.
            nextAttestationAttemptAtMs = System.currentTimeMillis() + GENERIC_FAILURE_RETRY_COOLDOWN_MS
            Timber.tag(TAG).w(e, "Failed to build attestation fields")
            null
        }
    }

    suspend fun fetchNonce(baseUrl: String): String = withContext(Dispatchers.IO) {
        // Why: server-issued nonce binds this attestation to one request window and prevents
        // replay of an old certificate chain in a future API call.
        val request = Request.Builder()
            .url("$baseUrl/api/attestation/nonce")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        response.use {
            // Why: attestation without a valid nonce is meaningless; fail fast so caller can
            // apply fallback behavior instead of uploading unverifiable data.
            check(it.isSuccessful) { "Nonce request failed: HTTP ${it.code}" }
            val body = it.body.string()
            val json = JSONObject(body)
            json.getString("nonce")
        }
    }

    suspend fun attest(context: Context, baseUrl: String): AttestationResult = withContext(Dispatchers.IO) {
        // Why: "Unsupported" here means "not currently viable" (cooldown or capability issue),
        // which callers can use to avoid treating this as a hard error.
        if (!shouldAttemptAttestation(context)) {
            return@withContext AttestationResult.Unsupported
        }

        val nonce = fetchNonce(baseUrl)

        try {
            generateAttestationCertificateChain(nonce)
        } catch (e: ProviderException) {
            // Why: same cooldown strategy as getAttestationFields to keep behavior predictable
            // regardless of which API surface uses attestation.
            nextAttestationAttemptAtMs = System.currentTimeMillis() + PROVIDER_FAILURE_RETRY_COOLDOWN_MS
            Timber.tag(TAG).e(e, "Key attestation failed - Provider error")
            logAttestationUnavailable()
            return@withContext AttestationResult.Failed(e.message ?: "Provider error")
        } catch (e: Exception) {
            // Why: expose a failed result (instead of throw) so networking code can degrade gracefully.
            nextAttestationAttemptAtMs = System.currentTimeMillis() + GENERIC_FAILURE_RETRY_COOLDOWN_MS
            Timber.tag(TAG).e(e, "Key attestation failed")
            return@withContext AttestationResult.Failed(e.message ?: "Unknown error")
        }

        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val certificateChain = keyStore.getCertificateChain("GameNativeAttestationKey")

        if (certificateChain.isNullOrEmpty()) {
            // Why: empty chain means there is no cryptographic proof to verify.
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
                // Why: keep error surface explicit for telemetry/debugging. HTTP failures are
                // materially different from local keystore failures.
                Timber.tag(TAG).e("Attestation verification failed: HTTP ${it.code}")
                return@withContext AttestationResult.Failed("HTTP ${it.code}")
            }

            val body = it.body.string()
            val json = JSONObject(body)

            if (json.has("error")) {
                // Why: backend can reject an otherwise well-formed payload (policy mismatch,
                // invalid chain, replay). Surface backend reason to help diagnose drift.
                val error = json.getString("error")
                Timber.tag(TAG).e("Attestation error: $error")
                AttestationResult.Failed(error)
            } else {
                AttestationResult.Success
            }
        }
    }

    private fun shouldAttemptAttestation(context: Context): Boolean {
        val now = System.currentTimeMillis()
        if (now < nextAttestationAttemptAtMs) {
            // Why: this short-circuit protects user flows from repeated costly failure paths.
            return false
        }

        // StrongBox is only a capability hint here; attestation may still work via TEE-backed keystore.
        val strongBoxState = try {
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
                STRONGBOX_SUPPORTED
            } else {
                STRONGBOX_UNSUPPORTED
            }
        } catch (_: Exception) {
            STRONGBOX_UNKNOWN
        }
        if (strongBoxState == STRONGBOX_UNSUPPORTED && !hasLoggedAttestationUnavailable) {
            // Why: one-time informational log clarifies why attestation behavior may be degraded
            // without flooding logs on every call.
            Timber.tag(TAG).i("StrongBox unavailable; attempting attestation with available keystore backend")
        }
        // Why: absence of StrongBox does not imply absence of attestation capability.
        return true
    }

    private fun generateAttestationCertificateChain(nonce: String): Array<java.security.cert.Certificate>? {
        val keyAlias = "GameNativeAttestationKey"
        // Why: regenerate key material per nonce so the cert chain challenge always matches the
        // current request and stale aliases cannot leak old attestation state.
        clearKey()
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
        return keyStore.getCertificateChain(keyAlias)
    }

    private fun logAttestationUnavailable() {
        if (!hasLoggedAttestationUnavailable) {
            hasLoggedAttestationUnavailable = true
            // Why: warn once to keep logs useful; repeated warnings provide little extra signal.
            Timber.tag(TAG).w("Device does not support hardware attestation")
        }
    }

    fun clearKey() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            // Why: deletion is idempotent and keeps alias lifecycle deterministic before re-issuing
            // attestation keys tied to a new challenge.
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
