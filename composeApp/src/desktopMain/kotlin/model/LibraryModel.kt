package model

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import java.io.Closeable
import java.time.Instant

interface Library : Closeable {
    val state: LibraryState
    val sorter: LibrarySortingModel
    suspend fun BorrowLike.setReturned()
    fun Book.getStock(): UInt
    val readers: List<Reader>
    val books: List<Book>
    val borrows: List<BorrowLike>
    fun Reader.getBorrows(): List<BorrowLike>

    suspend fun connect()
    suspend fun addBook(book: Book)
    suspend fun updateBook(book: Book)
    fun getBook(id: Identifier): Book?
    suspend fun deleteBook(book: Book)
    suspend fun addBorrow(borrower: Reader, book: Book, due: Instant)
    suspend fun addBorrowBatch(borrower: Reader, books: List<Book>, due: Instant)
    suspend fun addReader(reader: Reader)
    fun getReader(id: Identifier): Reader?
    suspend fun updateReader(reader: Reader)
    suspend fun deleteReader(reader: Reader)

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