package app.gamegrub.service.steam.managers

import com.winlator.xenvironment.ImageFs
import com.winlator.xenvironment.components.GuestProgramLauncherComponent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamSessionManagerTest {
    private val appSessionManager = mockk<SteamAppSessionManager>(relaxed = true)
    private val sessionFilesManager = mockk<SteamSessionFilesManager>(relaxed = true)
    private val ticketManager = mockk<SteamTicketManager>(relaxed = true)

    private val manager = SteamSessionManager(
        appSessionManager = appSessionManager,
        sessionFilesManager = sessionFilesManager,
        ticketManager = ticketManager,
    )

    @Test
    fun keepAlive_accessors_delegateToAppSessionManager() {
        every { appSessionManager.keepAlive } returns false

        assertTrue(!manager.getKeepAlive())
        manager.setKeepAlive(true)

        verify { appSessionManager.keepAlive = true }
    }

    @Test
    fun sessionFiles_methods_delegateToSessionFilesManager() {
        val imageFs = mockk<ImageFs>(relaxed = true)
        val guestProgramLauncherComponent = mockk<GuestProgramLauncherComponent>(relaxed = true)
        val session = SteamSessionContext(
            steamId64 = "1",
            account = "test",
            refreshToken = "refresh",
        )

        manager.applyAutoLoginUserChanges(imageFs, session)
        manager.setupRealSteamSessionFiles(session, imageFs, guestProgramLauncherComponent)

        verify { sessionFilesManager.applyAutoLoginUserChanges(imageFs, session) }
        verify { sessionFilesManager.setupRealSteamSessionFiles(session, imageFs, guestProgramLauncherComponent) }
    }

    @Test
    fun ticket_methods_delegateToTicketManager() = runBlocking {
        coEvery { ticketManager.getEncryptedAppTicket(10) } returns byteArrayOf(1, 2)
        coEvery { ticketManager.getEncryptedAppTicketBase64(10) } returns "abc"

        val ticket = manager.getEncryptedAppTicket(10)
        val ticketBase64 = manager.getEncryptedAppTicketBase64(10)
        manager.clearAllTickets()

        assertEquals(2, ticket?.size)
        assertEquals("abc", ticketBase64)
        coVerify { ticketManager.getEncryptedAppTicket(10) }
        coVerify { ticketManager.getEncryptedAppTicketBase64(10) }
        verify { ticketManager.clearAllTickets() }
    }
}

