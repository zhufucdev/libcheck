package model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.sqlmaster.proto.LibraryOuterClass
import com.sqlmaster.proto.LibraryOuterClass.UserRole
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.compose.resources.stringResource
import resources.*
import java.time.Instant
import java.util.*

interface Identifiable<T : Identifier> {
    val id: T
}

interface Identifier {
    override fun equals(other: Any?): Boolean
}

interface Searchable : Identifiable<UuidIdentifier> {
    fun matches(keyword: String): Boolean
    val name: String

    @Composable
    fun displayName(): String
}

@Serializable(IdentifierSerializer::class)
data class UuidIdentifier(val uuid: UUID = UUID.randomUUID()) : Identifier {
    override fun toString(): String {
        return uuid.toString()
    }

    companion object {
        fun parse(str: String) = UuidIdentifier(UUID.fromString(str))
    }
}

class IdentifierSerializer : KSerializer<UuidIdentifier> {
    private val serializer = String.serializer()

    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: UuidIdentifier) {
        encoder.encodeSerializableValue(serializer, value.uuid.toString())
    }

    override fun deserialize(decoder: Decoder): UuidIdentifier {
        val str = decoder.decodeSerializableValue(serializer)
        return UuidIdentifier(UUID.fromString(str))
    }
}

@Serializable
data class Book(
    override val name: String,
    val author: String,
    val isbn: String,
    override val id: UuidIdentifier,
    val avatarUri: String,
    val stock: UInt,
) : Searchable {
    override fun matches(keyword: String) = name.contains(keyword, ignoreCase = true)
            || author.contains(keyword, ignoreCase = true)
            || isbn.contains(keyword, ignoreCase = true)

    @Composable
    override fun displayName(): String = name
}

@Serializable
data class Reader(
    override val name: String,
    override val id: UuidIdentifier,
    val avatarUri: String,
    val tier: LibraryOuterClass.ReaderTier,
    val creditability: Float = 0f,
) : Searchable {
    override fun matches(keyword: String) = name.contains(keyword, ignoreCase = true)

    @Composable
    override fun displayName(): String = name
}

data class IntegerIdentifier(val id: Int) : Identifier

data class User(
    override val id: IntegerIdentifier,
    val deviceName: String,
    val role: UserRole,
    val readerId: UuidIdentifier?,
) : Identifiable<IntegerIdentifier>

@Serializable
sealed interface BorrowLike : Identifiable<UuidIdentifier> {
    val readerId: UuidIdentifier
    val time: Long
    val dueTime: Long
    val returnTime: Long?
    val expired get() = Instant.now().toEpochMilli() >= dueTime
    fun hasBook(id: UuidIdentifier): Boolean
    fun instance(library: Library): BorrowLikeInstanced
}

sealed interface BorrowLikeInstanced : Searchable {
    val readerId: UuidIdentifier
    val reader: Reader?
    val bookId: UuidIdentifier
    val book: Book?
    val time: Long
    val dueTime: Long
    val returnTime: Long?
    val original: BorrowLike
}

@Serializable
data class Borrow(
    override val id: UuidIdentifier,
    override val readerId: UuidIdentifier,
    val bookId: UuidIdentifier,
    override val time: Long,
    override val dueTime: Long,
    override val returnTime: Long? = null,
) : BorrowLike {
    override fun instance(library: Library) =
        BorrowInstanced(
            id,
            readerId,
            library.getReader(readerId),
            bookId,
            library.getBook(bookId),
            time,
            dueTime,
            returnTime,
            this
        )

    override fun hasBook(id: UuidIdentifier): Boolean = bookId == id
}

@Serializable
data class BorrowBatch(
    override val id: UuidIdentifier,
    override val readerId: UuidIdentifier,
    val bookIds: List<UuidIdentifier>,
    override val time: Long,
    override val dueTime: Long,
    override val returnTime: Long? = null,
) : BorrowLike {
    override fun instance(library: Library) =
        BorrowBatchInstanced(
            id,
            readerId,
            library.getReader(readerId),
            bookIds.first(),
            bookIds.map { library.getBook(it) },
            time,
            dueTime,
            returnTime,
            this
        )

    override fun hasBook(id: UuidIdentifier): Boolean = bookIds.contains(id)
}

@Stable
class BorrowInstanced(
    override val id: UuidIdentifier,
    override val readerId: UuidIdentifier,
    override val reader: Reader?,
    override val bookId: UuidIdentifier,
    override val book: Book?,
    override val time: Long,
    override val dueTime: Long,
    override val returnTime: Long?,
    override val original: Borrow,
) : BorrowLikeInstanced {
    override fun matches(keyword: String) = reader?.matches(keyword) == true || book?.matches(keyword) == true
    override val name: String
        get() = "${book?.name ?: "Unknown book"} lent to ${reader?.name ?: "unknown reader"}"

    @Composable
    override fun displayName(): String = stringResource(
        Res.string.lent_to_para,
        book?.displayName() ?: stringResource(Res.string.unknown_book_para),
        reader?.displayName() ?: stringResource(Res.string.unknown_reader_span)
    )
}

@Stable
class BorrowBatchInstanced(
    override val id: UuidIdentifier,
    override val readerId: UuidIdentifier,
    override val reader: Reader?,
    firstBookId: UuidIdentifier,
    val books: List<Book?>,
    override val time: Long,
    override val dueTime: Long,
    override val returnTime: Long?,
    override val original: BorrowBatch,
) : BorrowLikeInstanced {
    override val bookId: UuidIdentifier = firstBookId
    override val book: Book?
        get() = books.first()

    override fun matches(keyword: String): Boolean =
        reader?.matches(keyword) == true || books.any { it?.matches(keyword) == true }

    override val name: String
        get() = "${books.joinToString { it?.name ?: "unknown book" }} lent to ${reader?.name ?: "unknown reader"}"

    @Composable
    override fun displayName(): String {
        val unknownBookName = stringResource(Res.string.unknown_book_span)
        return stringResource(
            Res.string.lent_to_para,
            books.joinToString { it?.name ?: unknownBookName },
            reader?.displayName() ?: stringResource(Res.string.unknown_reader_span)
        )
    }
}

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
    READER_NAME("Reader Name"), READER_ID("Reader UUID"),
    BORROW_TIME("Borrow Time"), RETURN_TIME("Return Time")
}

@Serializable
data class SortedBookList(
    val items: MutableList<Book>,
    val sortedBy: BookSortable = BookSortable.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
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
                val stockOf = items.associateWith {
                    with(library) { it.getStock() }
                }
                if (sortOrder == SortOrder.ASCENDING) {
                    items.sortBy { stockOf[it] }
                } else {
                    items.sortByDescending { stockOf[it] }
                }
            }
        }
    }

    val model: SortModel<BookSortable> get() = SortModel(sortOrder, sortedBy)
}

@Serializable
data class SortedReaderList(
    val items: MutableList<Reader>,
    val sortedBy: ReaderSortable = ReaderSortable.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
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

    val model: SortModel<ReaderSortable> get() = SortModel(sortOrder, sortedBy)
}

@Serializable
data class SortedBorrowList(
    val items: MutableList<BorrowLike>,
    val sortedBy: BorrowSortable = BorrowSortable.BOOK_NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
) {
    fun sort(library: Library) {
        val instanced = items.map { it.instance(library) }.toMutableList()

        when (sortedBy) {
            BorrowSortable.BOOK_NAME ->
                if (sortOrder == SortOrder.ASCENDING) {
                    instanced.sortBy { it.book?.name ?: "" }
                } else {
                    instanced.sortByDescending { it.book?.name ?: "" }
                }

            BorrowSortable.BOOK_ID ->
                if (sortOrder == SortOrder.ASCENDING) {
                    instanced.sortBy { it.bookId.uuid }
                } else {
                    instanced.sortByDescending { it.bookId.uuid }
                }

            BorrowSortable.BOOK_AUTHOR ->
                if (sortOrder == SortOrder.ASCENDING) {
                    instanced.sortBy { it.book?.author ?: "" }
                } else {
                    instanced.sortByDescending { it.book?.author ?: "" }
                }

            BorrowSortable.READER_NAME ->
                if (sortOrder == SortOrder.ASCENDING) {
                    instanced.sortBy { it.reader?.name ?: "" }
                } else {
                    instanced.sortByDescending { it.reader?.name ?: "" }
                }

            BorrowSortable.READER_ID ->
                if (sortOrder == SortOrder.ASCENDING) {
                    instanced.sortBy { it.readerId.uuid }
                } else {
                    instanced.sortByDescending { it.readerId.uuid }
                }

            BorrowSortable.BORROW_TIME ->
                if (sortOrder == SortOrder.ASCENDING) {
                    instanced.sortBy { it.time }
                } else {
                    instanced.sortByDescending { it.time }
                }

            BorrowSortable.RETURN_TIME ->
                if (sortOrder == SortOrder.ASCENDING) {
                    instanced.sortBy { it.returnTime }
                } else {
                    instanced.sortByDescending { it.returnTime }
                }
        }

        instanced.forEachIndexed { index, borrowInstanced ->
            items[index] = borrowInstanced.original
        }
    }

    val model: SortModel<BorrowSortable> get() = SortModel(sortOrder, sortedBy)
}
