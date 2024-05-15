package library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sqlmaster.proto.AuthenticationGrpcKt
import com.sqlmaster.proto.LibraryGrpcKt
import io.grpc.ManagedChannel
import kotlinx.coroutines.flow.Flow
import model.*
import java.time.Instant

class RemoteLibrary(channel: ManagedChannel, private val password: String) : Library {
    private val libraryChannel = LibraryGrpcKt.LibraryCoroutineStub(channel)
    private val authenticationChannel = AuthenticationGrpcKt.AuthenticationCoroutineStub(channel)

    override var state: LibraryState by mutableStateOf(LibraryState.Initializing(0f))
    override val sorter: LibrarySorter
        get() = TODO("Not yet implemented")

    override suspend fun Borrow.setReturned() {
        TODO("Not yet implemented")
    }

    override fun Book.getStock(): UInt {
        TODO("Not yet implemented")
    }

    override val readers: List<Reader>
        get() = TODO("Not yet implemented")
    override val books: List<Book>
        get() = TODO("Not yet implemented")
    override val borrows: List<Borrow>
        get() = TODO("Not yet implemented")

    override fun Reader.getBorrows(): List<Borrow> {
        TODO("Not yet implemented")
    }

    override suspend fun connect() {

    }

    override suspend fun addBook(book: Book) {
        TODO("Not yet implemented")
    }

    override suspend fun updateBook(book: Book) {
        TODO("Not yet implemented")
    }

    override fun getBook(id: Identifier): Book? {
        TODO("Not yet implemented")
    }

    override suspend fun deleteBook(book: Book) {
        TODO("Not yet implemented")
    }

    override suspend fun addBorrow(borrower: Reader, book: Book, due: Instant) {
        TODO("Not yet implemented")
    }

    override suspend fun addReader(reader: Reader) {
        TODO("Not yet implemented")
    }

    override fun getReader(id: Identifier): Reader? {
        TODO("Not yet implemented")
    }

    override suspend fun updateReader(reader: Reader) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteReader(reader: Reader) {
        TODO("Not yet implemented")
    }

    override fun search(query: String): Flow<Searchable> {
        TODO("Not yet implemented")
    }
}