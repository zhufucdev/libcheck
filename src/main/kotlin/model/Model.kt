package model

import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

@Serializable(IdentifierSerializer::class)
data class Identifier(val uuid: UUID = UUID.randomUUID())

class IdentifierSerializer : KSerializer<Identifier> {
    private val serializer = String.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: Identifier) {
        encoder.encodeSerializableValue(serializer, value.uuid.toString())
    }

    override fun deserialize(decoder: Decoder): Identifier {
        val str = decoder.decodeSerializableValue(serializer)
        return Identifier(UUID.fromString(str))
    }
}

@Serializable
data class Book(
    val name: String,
    val author: String,
    val isbn: String,
    val id: Identifier,
    val avatarUri: String,
    val stock: UInt
)

@Serializable
class Reader(val name: String, val id: Identifier, val avatarUri: String)

@Serializable
class Borrow(
    val readerId: Identifier,
    val bookId: Identifier,
    val time: Long,
    val dueTime: Long,
    val returned: Boolean = false
) {
    val expired get() = System.currentTimeMillis() >= dueTime
}

enum class SortOrder {
    A2Z,
    Z2A
}

enum class BookSortable {
    NAME, ID, AUTHOR, ISBN, STOCK
}

enum class ReaderSortable {
    NAME, ID
}

enum class BorrowSortable {
    BOOK_NAME, BOOK_ID, BOOK_AUTHOR, READER_NAME, READER_ID
}

@Serializable
data class SortedBookList(
    val items: MutableList<Book>,
    val sortedBy: BookSortable = BookSortable.NAME,
    val sortOrder: SortOrder = SortOrder.A2Z
)

@Serializable
data class SortedReaderList(
    val items: MutableList<Reader>,
    val sortedBy: ReaderSortable = ReaderSortable.NAME,
    val sortOrder: SortOrder = SortOrder.A2Z
)

@Serializable
data class SortedBorrowList(
    val items: MutableList<Borrow>,
    val sortedBy: BorrowSortable = BorrowSortable.BOOK_NAME,
    val sortOrder: SortOrder = SortOrder.A2Z
)
