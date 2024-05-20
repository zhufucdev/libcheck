@file:OptIn(ExperimentalSerializationApi::class)

package model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sqlmaster.proto.LibraryOuterClass.ReaderTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import java.io.File
import kotlin.reflect.full.createInstance

@Serializable
private data class ConfigurationsModel(
    val dataSource: DataSourceType = DataSourceType.Local,
    val tiers: Map<ReaderTier, TierModel> = mapOf(
        ReaderTier.TIER_STARTER to TierModel(baseCredit = 0.2f),
        ReaderTier.TIER_SILVER to TierModel(baseCredit = 0.4f),
        ReaderTier.TIER_CHROMIUM to TierModel(baseCredit = 0.6f),
        ReaderTier.TIER_PLATINUM to TierModel(baseCredit = 0.8f)
    ),
    val unifyTier: Boolean = true,
    val uniTransformer: CreditTransformer = CreditTransformer(),
    val colorMode: ColorMode = ColorMode.System,
    val sorting: SortModelSnapshot = SortModelSnapshot(),
    val firstLaunch: Boolean = true,
)

class LocalMachineConfigurationViewModel(private val rootDir: File) : Configurations {
    private val configFile = File(rootDir, "preferences.json")
    private val libraryConfigFiles = buildMap {
        DataSource::class.sealedSubclasses.forEach {
            put(DataSourceType.of(it), "${it.simpleName!!.lowercase()}_lib.preferences.json")
        }
    }

    private val model =
        if (configFile.exists()) {
            configFile.inputStream().use { Json.decodeFromStream<ConfigurationsModel>(it) }
        } else {
            ConfigurationsModel()
        }
    override val sources: MutableMap<DataSourceType, DataSource> = mutableStateMapOf(
        *(libraryConfigFiles.entries.map { (type, path) ->
            type to (File(rootDir, path).takeIf { it.exists() }?.inputStream()
                ?.use { Json.decodeFromStream(type.serializer(), it) } ?: type.clazz.createInstance())
        }.toTypedArray())
    )
    override val tiers: MutableMap<ReaderTier, TierModel> =
        mutableStateMapOf(*(model.tiers.entries.map { it.key to it.value }).toTypedArray())
    override var useUnifiedTierModel: Boolean by mutableStateOf(model.unifyTier)
    override var unifiedCreditTransformer: CreditTransformer by mutableStateOf(model.uniTransformer)
    override var currentSourceType: DataSourceType by mutableStateOf(model.dataSource)
    override var colorMode: ColorMode by mutableStateOf(model.colorMode)
    override var sortModels: SortModelSnapshot by mutableStateOf(model.sorting)
    override var firstLaunch: Boolean by mutableStateOf(model.firstLaunch)
    override val defaultRootPath: String
        get() = rootDir.absolutePath

    private fun DataSourceType.serializer(): KSerializer<DataSource> = serializer(
        kClass = clazz,
        typeArgumentsSerializers = emptyList(),
        isNullable = false
    ) as KSerializer<DataSource>

    override suspend fun save() {
        val model = ConfigurationsModel(
            tiers = tiers,
            unifyTier = useUnifiedTierModel,
            uniTransformer = unifiedCreditTransformer,
            firstLaunch = firstLaunch,
            dataSource = currentSourceType,
            colorMode = colorMode,
            sorting = sortModels
        )
        withContext(Dispatchers.IO) {
            configFile.outputStream().use { Json.encodeToStream(model, it) }
            sources.forEach { (type, source) ->
                File(rootDir, libraryConfigFiles[type]!!).outputStream().use {
                    Json.encodeToStream(
                        type.serializer(),
                        source,
                        it
                    )
                }
            }
        }
    }
}