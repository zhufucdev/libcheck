package model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class SetUpAppModel(val configurations: Configurations) {
    var step: Int by mutableStateOf(0)
    var working: Boolean by mutableStateOf(false)
    var connectionException: Exception? by mutableStateOf(null)

    suspend fun launchHome() {
        configurations.firstLaunch = false
        configurations.save()
    }
}