package model

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
    val id: Identifier,
    val readerId: Identifier,
    val bookId: Identifier,
    val time: Long,
    val dueTime: Long,
    val returned: Boolean = false
) {
    val expired get() = System.currentTimeMillis() >= dueTime
}

private class BorrowInstanced(
    val reader: Reader,
    val book: Book,
    val time: Long,
    val dueTime: Long,
    val returned: Boolean
)

enum class SortOrder(val label: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending")
}

enum class BookSortable(val label: String) {
    NAME("Name"), ID("UUID"), AUTHOR("Author"), ISBN("ISBN"),
    STOCK("Stock"), IN_STOCK("In Stock")
}

enum class ReaderSortable(val label: String) {
    NAME("Name"), ID("UUID")
}

enum class BorrowSortable(val label: String) {
    BOOK_NAME("Book Name"), BOOK_ID("Book UUID"), BOOK_AUTHOR("Book Author"),
    READER_NAME("Reader Name"), READER_ID("Reader UUID")
}

@Serializable
data class SortedBookList(
    val items: MutableList<Book>,
    val sortedBy: BookSortable = BookSortable.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING
) {
    fun sort(library: Library) {
        when (sortedBy) {
            BookSortable.NAME ->
                if (sortOrder == SortOrder.ASCENDING) {
                    items.sortBy { it.name }
                } else {
                    items.sortByDescending { it.name }
                }

            BookSortable.ID ->
                if (sortOrder == SortOrder.ASCENDING) {
                    items.sortBy { it.id.uuid }
                } else {
                    items.sortByDescending { it.id.uuid }
                }

            BookSortable.AUTHOR ->
                if (sortOrder == SortOrder.ASCENDING) {
                    items.sortBy { it.author }
                } else {
                    items.sortByDescending { it.author }
                }

            BookSortable.ISBN ->
                if (sortOrder == SortOrder.ASCENDING) {
                    items.sortBy { it.isbn }
                } else {
                    items.sortByDescending { it.isbn }
                }

            BookSortable.STOCK ->
                if (sortOrder == SortOrder.ASCENDING) {
                    items.sortBy { it.stock }
                } else {
                    items.sortByDescending { it.stock }
                }

            BookSortable.IN_STOCK -> {
                val inStock = items.associateWith {
                    with(library) { it.inStock }
                }
                if (sortOrder == SortOrder.ASCENDING) {
                    items.sortBy { inStock[it] }
                } else {
                    items.sortByDescending { inStock[it] }
                }
            }
        }
    }
}

@Serializable
data class SortedReaderList(
    val items: MutableList<Reader>,
    val sortedBy: ReaderSortable = ReaderSortable.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING
) {
    fun sort() {
        when (sortedBy) {
            ReaderSortable.NAME ->
                if (sortOrder == SortOrder.ASCENDING) {
                    items.sortBy { it.name }
                } else {
                    items.sortByDescending { it.name }
                }

            ReaderSortable.ID ->
                if (sortOrder == SortOrder.ASCENDING) {
                    items.sortBy { it.id.uuid }
                } else {
                    items.sortByDescending { it.id.uuid }
                }
        }
    }
}

@Serializable
data class SortedBorrowList(
    val items: MutableList<Borrow>,
    val sortedBy: BorrowSortable = BorrowSortable.BOOK_NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING
) {
    fun sort(library: Library) {
        val instanced = items.map {
            BorrowInstanced(
                library.getReader(it.readerId)!!,
                library.getBook(it.bookId)!!,
                it.time,
                it.dueTime,
                it.returned
            )
        }.toMutableList()

        when (sortedBy) {
            BorrowSortable.BOOK_NAME ->
                if (sortOrder == SortOrder.ASCENDING) {
                    instanced.sortBy { it.book.name }
                } else {
                    instanced.sortByDescending { it.book.name }
                }

            BorrowSortable.BOOK_ID ->
                if (sortOrder == SortOrder.ASCENDING) {
                    instanced.sortBy { it.book.id.uuid }
                } else {
                    instanced.sortByDescending { it.book.id.uuid }
                }

            BorrowSortable.BOOK_AUTHOR ->
                if (sortOrder == SortOrder.ASCENDING) {
                    instanced.sortBy { it.book.author }
                } else {
                    instanced.sortByDescending { it.book.author }
                }

            BorrowSortable.READER_NAME ->
                if (sortOrder == SortOrder.ASCENDING) {
                    instanced.sortBy { it.reader.name }
                } else {
                    instanced.sortByDescending { it.reader.name }
                }

            BorrowSortable.READER_ID ->
                if (sortOrder == SortOrder.ASCENDING) {
                    instanced.sortBy { it.reader.id.uuid }
                } else {
                    instanced.sortByDescending { it.reader.id.uuid }
                }
        }
    }
}
