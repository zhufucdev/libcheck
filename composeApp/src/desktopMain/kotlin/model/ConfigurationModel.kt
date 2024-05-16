package model

import AesCipher
import io.grpc.ManagedChannelBuilder
import kotlinx.serialization.Serializable
import library.LocalMachineLibrary
import library.RemoteLibrary
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
        val useTransportSecurity: Boolean = true,
        val remotePort: Int = 5411,
        val password: ByteArray = AesCipher().encrypt("".encodeToByteArray()),
    ) : DataSource() {
        override fun initialize(context: Configurations): Library {
            val channel = ManagedChannelBuilder
                .forAddress(remoteHost, remotePort)
                .let { if (useTransportSecurity) it.useTransportSecurity() else it.usePlaintext() }
                .build()
            val password = AesCipher().decrypt(password).decodeToString()

            return RemoteLibrary(channel, password, context)
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

enum class DataSourceType(val clazz: KClass<out DataSource>) {
    Local(DataSource.Local::class), Remote(DataSource.Remote::class);

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

interface Configurations {
    var sources: Map<DataSourceType, DataSource>
    var currentSourceType: DataSourceType
    var colorMode: ColorMode
    var sortModels: SortModelSnapshot

    val defaultRootPath: String

    suspend fun save()
}
