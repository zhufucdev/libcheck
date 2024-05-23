package model

import androidx.compose.runtime.Stable
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import java.io.Closeable

interface Library : Closeable {
    val state: LibraryState
    val sorter: LibrarySortingModel

    val readers: List<Reader>
    val books: List<Book>
    val borrows: List<BorrowLike>
    val components: LibraryComponentsCollection

    fun Book.getStock(): UInt
    fun Reader.getBorrows(): List<BorrowLike>

    suspend fun connect()
    fun getBook(id: UuidIdentifier): Book?
    fun getReader(id: UuidIdentifier): Reader?

    fun search(query: String): Flow<Searchable>
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