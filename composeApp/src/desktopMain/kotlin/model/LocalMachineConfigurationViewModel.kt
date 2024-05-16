@file:OptIn(ExperimentalSerializationApi::class)

package model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import java.io.File

private enum class DataSourceType {
    Local, Remote
}

@Serializable
private data class ConfigurationsModel(
    val dataSource: DataSourceType = DataSourceType.Local,
    val colorMode: ColorMode = ColorMode.System,
    val sorting: SortModelSnapshot = SortModelSnapshot(),
)

class LocalMachineConfigurationViewModel(private val rootDir: File) : Configurations {
    private val configFile = File(rootDir, "preferences.json")
    private val localSourceConfig get() = File(rootDir, "local_library.preferences.json")
    private val remoteSourceConfig get() = File(rootDir, "remote_library.preferences.json")

    private val model =
        if (configFile.exists()) {
            configFile.inputStream().use { Json.decodeFromStream<ConfigurationsModel>(it) }
        } else {
            ConfigurationsModel()
        }
    override var dataSource: DataSource by mutableStateOf(
        when (model.dataSource) {
            DataSourceType.Local ->
                if (localSourceConfig.exists()) {
                    localSourceConfig.inputStream()
                        .use { Json.decodeFromStream(serializer<DataSource.Local>(), it) }
                } else {
                    DataSource.Local(rootDir.absolutePath)
                }

            DataSourceType.Remote -> if (remoteSourceConfig.exists()) {
                remoteSourceConfig.inputStream()
                    .use { Json.decodeFromStream(serializer<DataSource.Remote>(), it) }
            } else {
                DataSource.Remote()
            }
        }
    )
    override var colorMode: ColorMode by mutableStateOf(model.colorMode)
    override var sortModels: SortModelSnapshot by mutableStateOf(model.sorting)

    override suspend fun save() {
        val model = ConfigurationsModel(
            dataSource = when (dataSource) {
                is DataSource.Local -> DataSourceType.Local
                is DataSource.Remote -> DataSourceType.Remote
            },
            colorMode = colorMode,
            sorting = sortModels
        )
        withContext(Dispatchers.IO) {
            configFile.outputStream().use { Json.encodeToStream(model, it) }
            (when (dataSource) {
                is DataSource.Local -> localSourceConfig
                is DataSource.Remote -> remoteSourceConfig
            }).outputStream().use {
                Json.encodeToStream(dataSource, it)
            }
        }
    }
}