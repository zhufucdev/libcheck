package library

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.protobuf.ByteString
import com.sqlmaster.proto.*
import com.sqlmaster.proto.LibraryOuterClass.UpdateEffect
import currentPlatform
import io.grpc.ManagedChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import model.*
import java.time.Instant

class RemoteLibrary(
    private val deviceName: String,
    private val context: DataSource.Context,
    channelBuilder: () -> ManagedChannel,
) : Library {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val channel by lazy { channelBuilder.invoke() }
    private val libraryChannel by lazy { LibraryGrpcKt.LibraryCoroutineStub(channel) }
    private val authenticationChannel by lazy { AuthenticationGrpcKt.AuthenticationCoroutineStub(channel) }

    override var state: LibraryState by mutableStateOf(LibraryState.Initializing(0f))
    private lateinit var accessToken: ByteString

    override val sorter: LibrarySortingModel by lazy {
        DefaultSorter(context, this)
    }

    override suspend fun BorrowLike.setReturned() {
        val res =
            when (val p = this@setReturned) {
                is Borrow ->
                    libraryChannel.updateBorrow(newUpdateRequest(id) {
                        borrow = p.copy(returnTime = System.currentTimeMillis()).toProto()
                    })

                is BorrowBatch ->
                    libraryChannel.updateBorrowBatch(newUpdateRequest(id) {
                        batch = p.copy(returnTime = System.currentTimeMillis()).toProto()
                    })
            }
        res.effect.maybeThrow()
    }

    override fun Book.getStock(): UInt {
        val count by derivedStateOf { stock - borrows.count { it.hasBook(id) && it.returnTime == null }.toUInt() }
        return count
    }

    override val readers = SnapshotStateList<Reader>()
    override val books = SnapshotStateList<Book>()
    override val borrows = SnapshotStateList<BorrowLike>()

    override fun Reader.getBorrows(): List<BorrowLike> = borrows.filter { it.readerId == id }

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
        val capturedState = state
        if (capturedState !is LibraryState.Initializing || capturedState.progress > 0) {
            // already connected or is connecting
            return
        }

        if (context is DataSource.Context.WithToken && context.token != null) {
            val init = ByteString.copyFrom(context.token)
            val auth = authenticationReqeust {
                token = init
            }
            val authResult = authenticationChannel.authenticate(auth)
            if (authResult.allowed) {
                accessToken = init
            }
        } else if (context is DataSource.Context.WithPassword) {
            val auth = authorizationRequest {
                password = context.password
                deviceName = this@RemoteLibrary.deviceName
                os = currentPlatform::class.simpleName!!
            }
            val res = authenticationChannel.authorize(auth)
            if (res.allowed) {
                accessToken = res.token
                if (context is DataSource.Context.WithToken) {
                    context.token = res.token.toByteArray()
                    context.save()
                }
            } else {
                throw AccessDeniedException("Password authorization failed")
            }
        } else {
            val pwdChan = Channel<String>()
            val resChan = Channel<LibraryState.PasswordRequired.AuthResult>()
            state = LibraryState.PasswordRequired(pwdChan, resChan)
            while (true) {
                val pwd = pwdChan.receive()
                val auth = authorizationRequest {
                    password = pwd
                    deviceName = this@RemoteLibrary.deviceName
                    os = currentPlatform::class.simpleName!!
                }
                val res = authenticationChannel.authorize(auth)
                if (res.allowed) {
                    accessToken = res.token
                    if (context is DataSource.Context.WithToken) {
                        context.token = res.token.toByteArray()
                        context.save()
                    }
                    resChan.send(LibraryState.PasswordRequired.AuthResult(true, res.token.toByteArray()))
                    break
                }
                resChan.send(LibraryState.PasswordRequired.AuthResult(false, null))
            }
        }

        state = LibraryState.Synchronizing(0f, false)

        var ended = 0
        fun bumpEnded() {
            ended++
            if (ended == 4) {
                state = LibraryState.Idle
                coroutineScope.launch {
                    coroutineScope {
                        launch {
                            sorter.sortBooks()
                        }
                        launch {
                            sorter.sortReaders()
                        }
                        launch {
                            sorter.sortBorrows()
                        }
                    }
                }
            } else if (ended < 4) {
                state = LibraryState.Initializing(ended / 4f)
            }
        }

        coroutineScope.launch {
            UniqueIdentifierStateList.bindTo(
                readers,
                libraryChannel
                    .getReaders(newGetRequest())
                    .onEach {
                        if (it.end) {
                            bumpEnded()
                        }
                    }
                    .filter { !it.end }
                    .map { Identifier.parse(it.id) to it.readerOrNull?.toModel() }
            ) {
                if (state !is LibraryState.Initializing) {
                    sorter.sortReaders()
                }
            }
        }
        coroutineScope.launch {
            UniqueIdentifierStateList.bindTo(
                books,
                libraryChannel
                    .getBooks(newGetRequest())
                    .onEach {
                        if (it.end) {
                            bumpEnded()
                        }
                    }
                    .filter { !it.end }
                    .map { Identifier.parse(it.id) to it.bookOrNull?.toModel() }
            ) {
                if (state !is LibraryState.Initializing) {
                    sorter.sortBooks()
                }
            }
        }
        coroutineScope.launch {
            UniqueIdentifierStateList.bindTo(
                borrows,
                merge(
                    libraryChannel.getBorrowBatches(newGetRequest())
                        .onEach {
                            if (it.end) {
                                bumpEnded()
                            }
                        }
                        .filter { !it.end }
                        .map { Identifier.parse(it.id) to it.batchOrNull?.toModel() },
                    libraryChannel
                        .getBorrows(newGetRequest())
                        .onEach {
                            if (it.end) {
                                bumpEnded()
                            }
                        }
                        .filter { !it.end }
                        .map { Identifier.parse(it.id) to it.borrowOrNull?.toModel() }
                )
            ) {
                if (state !is LibraryState.Initializing) {
                    sorter.sortBorrows()
                }
            }
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

    override suspend fun addBorrowBatch(borrower: Reader, books: List<Book>, due: Instant) {
        val res =
            libraryChannel.addBorrowBatch(newAddRequest {
                batch = borrowBatch {
                    id = Identifier().toString()
                    readerId = borrower.id.toString()
                    bookIds.addAll(books.map { it.id.toString() })
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
        if (state is LibraryState.Initializing) {
            return
        }
        readers.clear()
        books.clear()
        borrows.clear()
        coroutineScope.cancel()
        channel.shutdown()
    }

    private fun UpdateEffect.maybeThrow(): UpdateEffect =
        when (this) {
            UpdateEffect.EFFECT_UNSPECIFIED, UpdateEffect.EFFECT_OK -> this
            UpdateEffect.EFFECT_NOT_FOUND -> throw NullPointerException("Book not found")
            UpdateEffect.EFFECT_FORBIDDEN -> throw AccessDeniedException("Access forbidden")
            UpdateEffect.UNRECOGNIZED -> throw IllegalStateException("Unrecognized effect")
        }

    class AccessDeniedException(message: String) : RuntimeException(message)
}