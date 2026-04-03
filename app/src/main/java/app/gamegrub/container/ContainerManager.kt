package app.gamegrub.container

import app.gamegrub.data.LibraryItem

interface ContainerManager {
    suspend fun createContainer(appId: String): Result<ContainerHandle>

    suspend fun getContainer(appId: String): ContainerHandle?

    suspend fun deleteContainer(appId: String): Result<Unit>

    suspend fun isContainerRunning(appId: String): Boolean

    suspend fun startContainer(appId: String): Result<Unit>

    suspend fun stopContainer(appId: String): Result<Unit>

    suspend fun exportContainerConfig(appId: String, path: String): Result<Unit>

    suspend fun importContainerConfig(appId: String, path: String): Result<Unit>

    fun getAllContainers(): List<ContainerHandle>
}

interface ContainerHandle {
    val appId: String
    val isRunning: Boolean
    val executablePath: String
    val workingDirectory: String

    suspend fun saveConfig()

    suspend fun resetToDefaults(preserveDrives: Boolean = true)
}

sealed class ContainerState {
    data object NotCreated : ContainerState()
    data object Creating : ContainerState()
    data class Ready(val handle: ContainerHandle) : ContainerState()
    data object Starting : ContainerState()
    data class Running(val handle: ContainerHandle) : ContainerState()
    data object Stopping : ContainerState()
    data class Error(val message: String) : ContainerState()
}
