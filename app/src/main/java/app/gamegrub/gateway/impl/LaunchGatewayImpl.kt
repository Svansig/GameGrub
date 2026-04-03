package app.gamegrub.gateway.impl

import app.gamegrub.data.LibraryItem
import app.gamegrub.gateway.LaunchGateway
import app.gamegrub.gateway.LaunchState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LaunchGatewayImpl @Inject constructor() : LaunchGateway {

    private val launchStates = mutableMapOf<String, LaunchState>()

    override suspend fun launchGame(libraryItem: LibraryItem): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun prepareLaunch(libraryItem: LibraryItem): Result<Unit> {
        return Result.success(Unit)
    }

    override fun getLaunchState(appId: String): LaunchState {
        return launchStates[appId] ?: LaunchState.Idle
    }

    override fun cancelLaunch(appId: String) {
        launchStates.remove(appId)
    }

    override fun getActiveLaunchCount(): Int {
        return launchStates.count { it.value is LaunchState.Launching }
    }
}
