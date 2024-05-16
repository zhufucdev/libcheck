package library

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.protobuf.ByteString
import com.sqlmaster.proto.*
import com.sqlmaster.proto.LibraryOuterClass.UpdateEffect
import currentPlatform
import getHostName
import io.grpc.ManagedChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import model.*
import java.time.Instant

class RemoteLibrary(
    private val channel: ManagedChannel,
    password: String,
    private val configurations: Configurations
) : Library {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val libraryChannel = LibraryGrpcKt.LibraryCoroutineStub(channel)
    private val authenticationChannel = AuthenticationGrpcKt.AuthenticationCoroutineStub(channel)

    private var password: String? = password
    private lateinit var accessToken: ByteString
    override var state: LibraryState by mutableStateOf(LibraryState.Initializing(0f))

    override val sorter: LibrarySortingModel by lazy {
        object : LibrarySortingModel {
            override val bookModel: SortModel<BookSortable>
                get() = TODO("Not yet implemented")
            override val readerModel: SortModel<ReaderSortable>
                get() = TODO("Not yet implemented")
            override val borrowModel: SortModel<BorrowSortable>
                get() = TODO("Not yet implemented")

            override suspend fun sortBooks(order: SortOrder?, by: BookSortable?) {
                TODO("Not yet implemented")
            }

            override suspend fun sortReaders(order: SortOrder?, by: ReaderSortable?) {
                TODO("Not yet implemented")
            }

            override suspend fun sortBorrows(order: SortOrder?, by: BorrowSortable?) {
                TODO("Not yet implemented")
            }
        }
    }

    override suspend fun Borrow.setReturned() {
        val res =
            libraryChannel.updateBorrow(newUpdateRequest(id) {
                borrow = this@setReturned.copy(returnTime = System.currentTimeMillis()).toProto()
            })
        res.effect.maybeThrow()
    }

    override fun Book.getStock(): UInt {
        val count by derivedStateOf { stock - borrows.count { it.bookId == id && it.returnTime == null }.toUInt() }
        return count
    }

    override val readers = SnapshotStateList<Reader>()
    override val books = SnapshotStateList<Book>()
    override val borrows = SnapshotStateList<Borrow>()

    override fun Reader.getBorrows(): List<Borrow> = borrows.filter { it.readerId == id }

    private fun newUpdateRequest(id: Identifier, init: UpdateRequestKt.Dsl.() -> Unit) = updateRequest {
        token = accessToken
        this.id = id.toString()
        init(this)
    }

    private fun newAddRequest(init: AddRequestKt.Dsl.() -> Unit) = addRequest {
        token = accessToken
        init(this)
    }

    private fun newGetRequest() = getRequest { token = accessToken }
    private fun newDeleteRequest(id: Identifier) = deleteRequest {
        token = accessToken
        this.id = id.toString()
    }

    override suspend fun connect() {
        val auth = authorizationRequest {
            os = currentPlatform::class.simpleName!!
            deviceName = getHostName()
            password = this@RemoteLibrary.password!!
        }
        val authResult = authenticationChannel.authorize(auth)
        password = null // for security
        if (!authResult.allowed) {
            throw AccessDeniedException("password authentication failed")
        }
        accessToken = authResult.token
        val syncProcess by mutableFloatStateOf(0f)
        state = LibraryState.Synchronizing(syncProcess, false)

        coroutineScope.launch {
            UniqueIdentifierStateList.bindTo(
                readers,
                libraryChannel
                    .getReaders(newGetRequest())
                    .filter { it.ok }
                    .map { Identifier.parse(it.id) to it.readerOrNull?.toModel() }
            )
        }
        coroutineScope.launch {
            UniqueIdentifierStateList.bindTo(
                books,
                libraryChannel
                    .getBooks(newGetRequest())
                    .filter { it.ok }
                    .map { Identifier.parse(it.id) to it.bookOrNull?.toModel() }
            )
        }
        coroutineScope.launch {
            UniqueIdentifierStateList.bindTo(
                borrows,
                libraryChannel
                    .getBorrows(newGetRequest())
                    .filter { it.ok }
                    .map { Identifier.parse(it.id) to it.borrowOrNull?.toModel() }
            )
        }
    }

    override suspend fun addBook(book: Book) {
        val res =
            libraryChannel.addBook(
                newAddRequest {
                    this.book = book.toProto()
                }
            )
        res.effect.maybeThrow()
    }

    override suspend fun updateBook(book: Book) {
        val res =
            libraryChannel.updateBook(
                newUpdateRequest(book.id) {
                    this.book = book.toProto()
                }
            )
        res.effect.maybeThrow()
    }

    override fun getBook(id: Identifier): Book? = books.firstOrNull { it.id == id }

    override suspend fun deleteBook(book: Book) {
        libraryChannel.deleteBook(newDeleteRequest(book.id))
    }

    override suspend fun addBorrow(borrower: Reader, book: Book, due: Instant) {
        val res =
            libraryChannel.addBorrow(newAddRequest {
                borrow = borrow {
                    id = Identifier().toString()
                    readerId = borrower.id.toString()
                    bookId = book.id.toString()
                    time = Timestamp(Instant.now().toEpochMilli())
                    dueTime = Timestamp(due.toEpochMilli())
                }
            })
        res.effect.maybeThrow()
    }

    override suspend fun addReader(reader: Reader) {
        val res =
            libraryChannel.addReader(newAddRequest {
                this.reader = reader.toProto()
            })
        res.effect.maybeThrow()
    }

    override fun getReader(id: Identifier): Reader? = readers.firstOrNull { it.id == id }

    override suspend fun updateReader(reader: Reader) {
        val res =
            libraryChannel.updateReader(newUpdateRequest(reader.id) {
                this.reader = reader.toProto()
            })
        res.effect.maybeThrow()
    }

    override suspend fun deleteReader(reader: Reader) {
        libraryChannel.deleteReader(newDeleteRequest(reader.id)).effect.maybeThrow()
    }

    override fun search(query: String) = searchFlow(query)

    override fun close() {
        coroutineScope.cancel()
        channel.shutdown()
    }

    private fun UpdateEffect.maybeThrow(): UpdateEffect =
        when (this) {
            UpdateEffect.EFFECT_UNSPECIFIED, UpdateEffect.EFFECT_OK -> this
            UpdateEffect.EFFECT_NOT_FOUND -> throw NullPointerException("book not found")
            UpdateEffect.EFFECT_FORBIDDEN -> throw AccessDeniedException("access forbidden")
            UpdateEffect.UNRECOGNIZED -> throw IllegalStateException("unrecognized effect")
        }

    class AccessDeniedException(message: String) : RuntimeException(message)
}