package model

import AesCipher
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.ui.graphics.vector.ImageVector
import com.sqlmaster.proto.LibraryOuterClass.ReaderTier
import getHostName
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import library.LocalMachineLibrary
import library.RemoteLibrary
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import redempt.crunch.Crunch
import redempt.crunch.functional.EvaluationEnvironment
import resources.*
import java.io.File
import kotlin.reflect.KClass

sealed class DataSource {
    abstract fun initialize(context: Configurations): Library

    @Serializable
    data class Local(val rootPath: String? = null) : DataSource() {
        override fun initialize(context: Configurations): Library =
            LocalMachineLibrary(File(rootPath ?: context.defaultRootPath), context)
    }

    @Serializable
    data class Remote(
        val remoteHost: String = "",
        val remotePort: Int = 5411,
        val deviceName: String = getHostName(),
        val useTransportSecurity: Boolean = true,
        val password: ByteArray = runBlocking { AesCipher() }.encrypt("".encodeToByteArray()),
    ) : DataSource() {
        override fun initialize(context: Configurations): Library {
            val channel = ManagedChannelBuilder
                .forAddress(remoteHost, remotePort)
                .let { if (useTransportSecurity) it.useTransportSecurity() else it.usePlaintext() }
                .build()
            val password = runBlocking { AesCipher() }.decrypt(password).decodeToString()

            return RemoteLibrary(channel, password, deviceName, context)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Remote

            if (remoteHost != other.remoteHost) return false
            if (useTransportSecurity != other.useTransportSecurity) return false
            if (remotePort != other.remotePort) return false
            if (!password.contentEquals(other.password)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = remoteHost.hashCode()
            result = 31 * result + useTransportSecurity.hashCode()
            result = 31 * result + remotePort
            result = 31 * result + password.contentHashCode()
            return result
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
enum class DataSourceType(
    val clazz: KClass<out DataSource>,
    val titleStrRes: StringResource,
    val descriptionStrRes: StringResource,
    val icon: ImageVector,
) {
    Local(DataSource.Local::class, Res.string.store_locally_para, Res.string.store_locally_des, Icons.Default.Computer),
    Remote(
        DataSource.Remote::class,
        Res.string.store_on_a_checkmate_server_para,
        Res.string.store_on_a_checkmate_server_des,
        Icons.Default.LocationCity
    );

    companion object {
        fun of(clazz: KClass<out DataSource>): DataSourceType = entries.first { it.clazz == clazz }
    }
}


enum class ColorMode {
    System, Dark, Light
}

@Serializable
data class SortModelSnapshot(
    val books: SortModel<BookSortable> = SortModel(SortOrder.ASCENDING, BookSortable.ID),
    val readers: SortModel<ReaderSortable> = SortModel(SortOrder.ASCENDING, ReaderSortable.ID),
    val borrows: SortModel<BorrowSortable> = SortModel(SortOrder.ASCENDING, BorrowSortable.BOOK_ID),
)

@Serializable(CreditTransformerSerializer::class)
data class CreditTransformer(val expr: String = "5 * x") {
    private val exprCompiled by lazy {
        Crunch.compileExpression(
            expr,
            EvaluationEnvironment().apply {
                setVariableNames("x")
            }
        )
    }

    fun compileOrNull(): CreditTransformer? = try {
        exprCompiled
        this
    } catch (e: Exception) {
        null
    }

    fun transform(credit: Float): Double = exprCompiled.evaluate((credit).toDouble())
}

class CreditTransformerSerializer : KSerializer<CreditTransformer> {
    private val delegateSerializer = serializer<String>()
    override val descriptor: SerialDescriptor = delegateSerializer.descriptor

    override fun deserialize(decoder: Decoder): CreditTransformer {
        return CreditTransformer(delegateSerializer.deserialize(decoder))
    }

    override fun serialize(encoder: Encoder, value: CreditTransformer) {
        delegateSerializer.serialize(encoder, value.expr)
    }
}

@Serializable
data class TierModel(val baseCredit: Float, val transformer: CreditTransformer = CreditTransformer())

interface Configurations {
    val tiers: MutableMap<ReaderTier, TierModel>
    var useUnifiedTierModel: Boolean
    var unifiedCreditTransformer: CreditTransformer

    val sources: MutableMap<DataSourceType, DataSource>
    var currentSourceType: DataSourceType
    var colorMode: ColorMode
    var sortModels: SortModelSnapshot
    var firstLaunch: Boolean

    val defaultRootPath: String

    suspend fun save()
}
