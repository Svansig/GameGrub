package app.gamegrub.service.steam.managers

import app.gamegrub.service.steam.managers.SteamControllerTemplateRoutingManager.TemplateSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamControllerTemplateRoutingManagerTest {
    @Test
    fun routeFor_returnsDownloaded_forIndexOne() {
        val route = SteamControllerTemplateRoutingManager.routeFor(1)

        assertEquals(TemplateSource.Downloaded, route.source)
        assertEquals(null, route.builtInTemplateName)
    }

    @Test
    fun routeFor_returnsManifest_forIndexThirteen() {
        val route = SteamControllerTemplateRoutingManager.routeFor(13)

        assertEquals(TemplateSource.Manifest, route.source)
        assertEquals(null, route.builtInTemplateName)
    }

    @Test
    fun routeFor_returnsMappedBuiltInTemplate_forKnownIndices() {
        assertEquals(
            "controller_xboxone_gamepad_fps.vdf",
            SteamControllerTemplateRoutingManager.routeFor(2).builtInTemplateName,
        )
        assertEquals(
            "controller_xboxone_gamepad_fps.vdf",
            SteamControllerTemplateRoutingManager.routeFor(12).builtInTemplateName,
        )
        assertEquals(
            "controller_xboxone_wasd.vdf",
            SteamControllerTemplateRoutingManager.routeFor(6).builtInTemplateName,
        )
        assertEquals(
            "gamepad_joystick.vdf",
            SteamControllerTemplateRoutingManager.routeFor(4).builtInTemplateName,
        )
    }

    @Test
    fun routeFor_returnsDefaultBuiltInTemplate_forUnknownIndex() {
        val route = SteamControllerTemplateRoutingManager.routeFor(999)

        assertEquals(TemplateSource.BuiltIn, route.source)
        assertEquals("gamepad+mouse.vdf", route.builtInTemplateName)
    }

    @Test
    fun requiresWorkshopDownload_trueOnlyForDownloadedRoute() {
        assertTrue(SteamControllerTemplateRoutingManager.requiresWorkshopDownload(1))
        assertFalse(SteamControllerTemplateRoutingManager.requiresWorkshopDownload(13))
        assertFalse(SteamControllerTemplateRoutingManager.requiresWorkshopDownload(2))
    }
}

