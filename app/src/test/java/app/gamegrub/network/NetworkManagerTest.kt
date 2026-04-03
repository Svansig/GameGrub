package app.gamegrub.network

import kotlinx.coroutines.test.runTest
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkManagerTest {
    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun buildGetRequest_addsHeaders() {
        val request = NetworkManager.buildGetRequest(
            url = "https://example.com/path",
            headers = mapOf("Authorization" to "Bearer token", "X-Test" to "value"),
        )

        assertEquals("GET", request.method)
        assertEquals("https://example.com/path", request.url.toString())
        assertEquals("Bearer token", request.header("Authorization"))
        assertEquals("value", request.header("X-Test"))
    }

    @Test
    fun executeForBodyString_returnsBodyForSuccessfulResponse() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"ok\":true}"),
        )

        val request = Request.Builder().url(mockWebServer.url("/health")).build()

        val result = NetworkManager.executeForBodyString(request)

        assertEquals("{\"ok\":true}", result)
        val recorded = mockWebServer.takeRequest()
        assertEquals("/health", recorded.path)
    }

    @Test
    fun executeForBodyString_returnsNullForNonSuccessResponse() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("failure"),
        )

        val request = Request.Builder().url(mockWebServer.url("/error")).build()

        val result = NetworkManager.executeForBodyString(request)

        assertNull(result)
        assertTrue(mockWebServer.takeRequest().path == "/error")
    }
}

