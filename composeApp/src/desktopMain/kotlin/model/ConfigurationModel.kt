package model

import AesCipher
import io.grpc.ManagedChannelBuilder
import kotlinx.serialization.Serializable
import library.LocalMachineLibrary
import library.RemoteLibrary
import java.io.File

@Serializable
sealed class DataSource {
    abstract fun initialize(context: Configurations): Library

    @Serializable
    data class Local(val rootPath: String) : DataSource() {
        override fun initialize(context: Configurations): Library = LocalMachineLibrary(File(rootPath), context)
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

enum class ColorMode {
    System, Dark, Light
}

@Serializable
data class SortModelSnapshot(
    val books: SortModel<BookSortable> = SortModel(SortOrder.ASCENDING, BookSortable.ID),
    val readers: SortModel<ReaderSortable> = SortModel(SortOrder.ASCENDING, ReaderSortable.ID),
    val borrows: SortModel<ReaderSortable> = SortModel(SortOrder.ASCENDING, ReaderSortable.ID),
)

interface Configurations {
    var dataSource: DataSource
    var colorMode: ColorMode
    var sortModels: SortModelSnapshot

    suspend fun save()
}
