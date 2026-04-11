package app.gamegrub.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import app.gamegrub.network.NetworkManager.http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.dnsoverhttps.DnsOverHttps
import okio.IOException
import timber.log.Timber
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Central manager for network infrastructure concerns.
 *
 * Responsibilities:
 * - Process-wide connectivity state tracking (`hasInternet`, `hasWifiOrEthernet`)
 * - Shared OkHttp client and DNS fallback policy
 * - Request helper and coroutine-friendly execution primitives
 *
 * Business-specific API behavior (payload mapping, endpoint semantics, feature policy)
 * should remain in service/domain code and use these infrastructure primitives.
 */
object NetworkManager {
    private val initialized = AtomicBoolean(false)

    private val _hasInternet = MutableStateFlow(false)

    /**
     * Process-wide reactive internet availability state.
     *
     * Semantics:
     * - True when at least one non-VPN validated network exists, or
     * - True for validated VPN only when a non-VPN physical network is also present.
     */
    val hasInternet: StateFlow<Boolean> = _hasInternet.asStateFlow()

    private val _hasWifiOrEthernet = MutableStateFlow(false)

    /**
     * Process-wide reactive WiFi/Ethernet availability state.
     *
     * This only considers validated non-VPN transport capabilities to avoid stale
     * VPN transport reporting from being interpreted as WiFi/Ethernet availability.
     */
    val hasWifiOrEthernet: StateFlow<Boolean> = _hasWifiOrEthernet.asStateFlow()

    private val bootstrapClient: OkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        .build()

    private val doh: DnsOverHttps = DnsOverHttps.Builder()
        .client(bootstrapClient)
        .url("https://dns.google/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("8.8.8.8"),
            InetAddress.getByName("8.8.4.4"),
        )
        .build()

    /**
     * DNS resolver preferring DNS-over-HTTPS with system DNS fallback.
     */
    val fallbackDns: Dns = Dns { hostname ->
        try {
            doh.lookup(hostname)
        } catch (e: Exception) {
            Timber.w(e, "DoH lookup failed for %s, falling back to system DNS", hostname)
            Dns.SYSTEM.lookup(hostname)
        }
    }

    /**
     * Shared project-default HTTP client for general network infrastructure use.
     */
    val http: OkHttpClient by lazy {
        createHttpClient()
    }

    /**
     * Initializes process-wide connectivity tracking exactly once for app process lifetime.
     *
     * Safe to call repeatedly; only the first call registers callbacks.
     *
     * Internal state policy:
     * - `hasInternet` can trust validated VPN only when a non-VPN network exists.
     * - `hasWifiOrEthernet` trusts only validated non-VPN WiFi/Ethernet transports.
     *
     * @param context Any context; application context is preferred.
     */
    fun init(context: Context) {
        if (!initialized.compareAndSet(false, true)) {
            return
        }

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCaps = ConcurrentHashMap<Network, NetworkCapabilities>()

        fun skip(caps: NetworkCapabilities): Boolean {
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)
        }

        fun hasVpn(caps: NetworkCapabilities): Boolean {
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        }

        fun updateState() {
            val validatedCaps = networkCaps.values.filter {
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
            val nonVpnValidatedCaps = validatedCaps.filter { !hasVpn(it) }
            val hasAnyNonVpnNetwork = networkCaps.values.any { !hasVpn(it) }
            val hasValidatedVpn = validatedCaps.any { hasVpn(it) }

            _hasInternet.value = nonVpnValidatedCaps.isNotEmpty() || (hasValidatedVpn && hasAnyNonVpnNetwork)
            _hasWifiOrEthernet.value = nonVpnValidatedCaps.any {
                it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            }
        }

        connectivityManager.activeNetwork?.let { network ->
            connectivityManager.getNetworkCapabilities(network)?.let { caps ->
                if (!skip(caps)) {
                    networkCaps[network] = caps
                    updateState()
                }
            }
        }

        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    if (skip(caps)) {
                        return
                    }
                    networkCaps[network] = caps
                    updateState()
                }

                override fun onLost(network: Network) {
                    networkCaps.remove(network)
                    updateState()
                }
            },
        )
    }

    /**
     * Build a standard OkHttp client with project defaults.
     *
     * @param connectTimeoutSeconds Connection timeout in seconds.
     * @param readTimeoutSeconds Read timeout in seconds.
     * @param writeTimeoutSeconds Write timeout in seconds.
     * @param callTimeoutMinutes Full-call timeout in minutes.
     * @param pingIntervalSeconds HTTP/2 ping interval in seconds.
     * @param dns DNS resolver to use for hostname lookups.
     * @return Configured [OkHttpClient] instance.
     */
    fun createHttpClient(
        connectTimeoutSeconds: Long = 30,
        readTimeoutSeconds: Long = 60,
        writeTimeoutSeconds: Long = 30,
        callTimeoutMinutes: Long = 5,
        pingIntervalSeconds: Long = 30,
        dns: Dns = fallbackDns,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(dns)
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
            .callTimeout(callTimeoutMinutes, TimeUnit.MINUTES)
            .pingInterval(pingIntervalSeconds, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Build a GET request with optional headers.
     *
     * @param url Absolute URL for the request.
     * @param headers Optional request headers mapped by header name.
     * @return Built [Request] instance.
     */
    fun buildGetRequest(url: String, headers: Map<String, String> = emptyMap()): Request {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (name, value) -> requestBuilder.header(name, value) }
        return requestBuilder.build()
    }

    /**
     * Execute an OkHttp call with coroutine cancellation support.
     *
     * @param call Prepared OkHttp call.
     * @return HTTP [Response] when the call completes successfully.
     * @throws IOException When the request fails before receiving a response.
     */
    suspend fun await(call: Call): Response = suspendCancellableCoroutine { cont ->
        call.enqueue(
            object : okhttp3.Callback {
                override fun onResponse(call: Call, response: Response) {
                    cont.resume(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    cont.resumeWithException(e)
                }
            },
        )
        cont.invokeOnCancellation { call.cancel() }
    }

    /**
     * Execute a request using the provided client.
     *
     * @param request Request to execute.
     * @param client Client used to execute the request; defaults to shared [http].
     * @return HTTP [Response].
     * @throws IOException When the underlying transport fails.
     */
    suspend fun execute(request: Request, client: OkHttpClient = http): Response {
        return await(client.newCall(request))
    }

    /**
     * Execute a request on IO and return body text for successful responses only.
     *
     * Returns null for non-2xx responses or thrown transport/parsing exceptions.
     *
     * @param request Request to execute.
     * @param client Client used to execute the request; defaults to shared [http].
     * @return Response body text for successful requests; null otherwise.
     */
    suspend fun executeForBodyString(request: Request, client: OkHttpClient = http): String? {
        return withContext(Dispatchers.IO) {
            runCatching {
                execute(request, client).use { response ->
                    response.takeIf { it.isSuccessful }?.body?.string()
                }
            }.onFailure { error ->
                Timber.e(error, "Network request failed for %s", request.url)
            }.getOrNull()
        }
    }

    /**
     * True when the active network is internet-validated.
     *
     * This is an immediate snapshot and does not subscribe to updates.
     *
     * @param context Any context.
     * @return True if the current active network has validated internet capability.
     */
    fun hasValidatedInternetNow(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * True when active network is validated and uses WiFi or Ethernet transport.
     *
     * This is an immediate snapshot and does not subscribe to updates.
     *
     * @param context Any context.
     * @return True for validated WiFi/Ethernet active network.
     */
    fun hasWifiOrEthernetNow(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            return false
        }
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
