@file:OptIn(ExperimentalSerializationApi::class)

package model

import AesCipher
import androidx.compose.runtime.*
import com.sqlmaster.proto.LibraryOuterClass.ReaderTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
private data class TierStorageModel(
    val models: Map<ReaderTier, TierModel> = mapOf(
        ReaderTier.TIER_STARTER to TierModel(baseCredit = 0.2f),
        ReaderTier.TIER_SILVER to TierModel(baseCredit = 0.4f),
        ReaderTier.TIER_CHROMIUM to TierModel(baseCredit = 0.6f),
        ReaderTier.TIER_PLATINUM to TierModel(baseCredit = 0.8f)
    ),
    val step: Float = 0.01f,
    val unify: Boolean = true,
    val transformer: CreditTransformer = CreditTransformer(),
)

@Serializable
private data class ConfigurationsModel(
    val tiers: TierStorageModel = TierStorageModel(),
    val dataSource: DataSourceType = DataSourceType.Local,
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
    private val tokenFile = File(rootDir, "token")

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
        mutableStateMapOf(*(model.tiers.models.entries.map { it.key to it.value }).toTypedArray())
    override var useUnifiedTierModel: Boolean by mutableStateOf(model.tiers.unify)
    override var unifiedCreditTransformer: CreditTransformer by mutableStateOf(model.tiers.transformer)
    override var creditStep: Float by mutableFloatStateOf(model.tiers.step)

    override var currentSourceType: DataSourceType by mutableStateOf(model.dataSource)
    override var colorMode: ColorMode by mutableStateOf(model.colorMode)
    override var firstLaunch: Boolean by mutableStateOf(model.firstLaunch)

    override var token: ByteArray? by mutableStateOf(
        if (tokenFile.exists()) runBlocking { AesCipher() }.decrypt(
            tokenFile.readBytes()
        ) else null
    )

    var sortModels = model.sorting
    override val dataSourceContext: DataSource.Context
        get() = when (sources[currentSourceType]!!) {
            is DataSource.Local -> object : DataSource.Context.WithRootPath, DataSource.Context.WithSortModel {
                override val defaultRootPath: String = rootDir.absolutePath
                override var sortModel: SortModelSnapshot by ::sortModels

                override suspend fun save() = this@LocalMachineConfigurationViewModel.save()
            }

            is DataSource.Remote ->
                object : DataSource.Context.WithSortModel, DataSource.Context.WithToken {
                    override var sortModel: SortModelSnapshot by ::sortModels
                    override var token: ByteArray? by this@LocalMachineConfigurationViewModel::token

                    override suspend fun save() {
                        this@LocalMachineConfigurationViewModel.save()
                    }
                }
        }

    private fun DataSourceType.serializer(): KSerializer<DataSource> = serializer(
        kClass = clazz,
        typeArgumentsSerializers = emptyList(),
        isNullable = false
    ) as KSerializer<DataSource>

    override suspend fun save() {
        val model = ConfigurationsModel(
            tiers = TierStorageModel(
                unify = useUnifiedTierModel,
                transformer = unifiedCreditTransformer,
                models = tiers,
                step = creditStep
            ),
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
            token.let {
                if (it != null) {
                    tokenFile.writeBytes(AesCipher().encrypt(it))
                } else {
                    tokenFile.delete()
                }
            }
        }
    }
}