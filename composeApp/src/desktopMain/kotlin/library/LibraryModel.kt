package library

import androidx.compose.runtime.Stable
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import model.*
import java.io.Closeable
import java.time.Instant

sealed interface Library : Closeable {
    val state: LibraryState
    val sorter: LibrarySortingModel
    fun Book.getStock(): UInt
    val readers: List<Reader>
    val books: List<Book>
    val borrows: List<BorrowLike>
    fun Reader.getBorrows(): List<BorrowLike>

    suspend fun connect()
    fun getBook(id: Identifier): Book?
    fun getReader(id: Identifier): Reader?

    fun search(query: String): Flow<Searchable>

    interface WithModificationCapability : Library {
        suspend fun addBook(book: Book)
        suspend fun updateBook(book: Book)
        suspend fun deleteBook(book: Book)
        suspend fun addReader(reader: Reader)
        suspend fun updateReader(reader: Reader)
        suspend fun deleteReader(reader: Reader)
    }

    interface WithBorrowCapability : Library {
        suspend fun addBorrow(borrower: Reader, book: Book, due: Instant)
        suspend fun addBorrowBatch(borrower: Reader, books: List<Book>, due: Instant)
    }

    interface WithReturnCapability : Library {
        suspend fun BorrowLike.setReturned(readerCredit: Float)
    }
}

sealed interface LibraryState {
    @Stable
    data object Idle : LibraryState

    interface HasProgress {
        val progress: Float
    }

    @Stable
    data class Initializing(override val progress: Float) : LibraryState, HasProgress

    @Stable
    data class Synchronizing(override val progress: Float, val upload: Boolean) : LibraryState, HasProgress

    @Stable
    data class PasswordRequired(
        private val passwordChannel: SendChannel<String>,
        private val resultChannel: ReceiveChannel<AuthResult>,
    ) : LibraryState {
        class AuthResult(val allowed: Boolean, val token: ByteArray?)

        suspend fun retry(password: String): AuthResult {
            passwordChannel.send(password)
            return resultChannel.receive()
        }
    }
}

@Serializable
data class SortModel<T>(val order: SortOrder, val by: T)

interface LibrarySortingModel {
    val bookModel: SortModel<BookSortable>
    val readerModel: SortModel<ReaderSortable>
    val borrowModel: SortModel<BorrowSortable>

    suspend fun sortBooks(order: SortOrder? = null, by: BookSortable? = null)
    suspend fun sortReaders(order: SortOrder? = null, by: ReaderSortable? = null)
    suspend fun sortBorrows(order: SortOrder? = null, by: BorrowSortable? = null)
}