package app.gamegrub.domain.usecase

import app.gamegrub.data.LibraryItem
import app.gamegrub.gateway.LaunchGateway
import app.gamegrub.gateway.LaunchState
import javax.inject.Inject

class LaunchGameUseCase @Inject constructor(
    private val launchGateway: LaunchGateway,
) {
    suspend operator fun invoke(libraryItem: LibraryItem): Result<Unit> {
        return launchGateway.launchGame(libraryItem)
    }
}

class PrepareLaunchUseCase @Inject constructor(
    private val launchGateway: LaunchGateway,
) {
    suspend operator fun invoke(libraryItem: LibraryItem): Result<Unit> {
        return launchGateway.prepareLaunch(libraryItem)
    }
}

class GetLaunchStateUseCase @Inject constructor(
    private val launchGateway: LaunchGateway,
) {
    operator fun invoke(appId: String): LaunchState {
        return launchGateway.getLaunchState(appId)
    }
}

class CancelLaunchUseCase @Inject constructor(
    private val launchGateway: LaunchGateway,
) {
    operator fun invoke(appId: String) {
        launchGateway.cancelLaunch(appId)
    }
}

class GetActiveLaunchCountUseCase @Inject constructor(
    private val launchGateway: LaunchGateway,
) {
    operator fun invoke(): Int {
        return launchGateway.getActiveLaunchCount()
    }
}
