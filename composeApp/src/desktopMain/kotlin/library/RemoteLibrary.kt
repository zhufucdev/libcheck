package library

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.protobuf.ByteString
import com.sqlmaster.proto.*
import com.sqlmaster.proto.LibraryOuterClass.UpdateEffect
import com.sqlmaster.proto.LibraryOuterClass.UserRole
import currentPlatform
import io.grpc.ManagedChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.*
import model.*
import java.time.Instant

open class RemoteLibrary(
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
    override val components = LibraryComponentsCollection(basicComponent)

    private val basicComponent
        get() = object : AccountCapability {
            override val account: Flow<User> = flow {
                val res = authenticationChannel.getUser(getRequest {
                    token = accessToken
                })
                if (res.allowed) {
                    emit(res.user.toModel())
                }
            }
            override val sessions: SnapshotStateList<LibraryOuterClass.Session> = mutableStateListOf()

            override suspend fun changePassword(oldPassword: String, newPassword: String): AccountCapability.ChangePasswordResult {
                val res = authenticationChannel.changePassword(changePasswordRequest {
                    password = oldPassword
                    token = accessToken
                    this.newPassword = newPassword
                })
                return when (res.effect!!) {
                    UpdateEffect.EFFECT_UNSPECIFIED, UpdateEffect.UNRECOGNIZED -> {
                        res.effect.maybeThrow()
                        error("not reachable")
                    }
                    UpdateEffect.EFFECT_OK -> AccountCapability.ChangePasswordResult.OK
                    UpdateEffect.EFFECT_NOT_FOUND -> AccountCapability.ChangePasswordResult.Invalid
                    UpdateEffect.EFFECT_FORBIDDEN -> AccountCapability.ChangePasswordResult.Forbidden
                }
            }

            override suspend fun revokeSession(session: LibraryOuterClass.Session) {
                val res = authenticationChannel.revoke(revokeTokenRequest {
                    token = accessToken
                    sessionId = session.id
                })
                if (!res.allowed) {
                    throw AccessDeniedException("Revoke session")
                }
            }

            override suspend fun connect() {
                sessions.clear()

                val res = authenticationChannel.getSessions(getRequest {
                    token = accessToken
                })
                res.collect {
                    sessions.add(it)
                }
            }
        }

    private val librarianComponent
        get() = object : ModificationCapability, BorrowCapability, ReturnCapability {
            private fun newUpdateRequest(id: UuidIdentifier, init: UpdateRequestKt.Dsl.() -> Unit) = updateRequest {
                token = accessToken
                this.id = id.toString()
                init(this)
            }

            private fun newAddRequest(init: AddRequestKt.Dsl.() -> Unit) = addRequest {
                token = accessToken
                init(this)
            }

            private fun newDeleteRequest(id: UuidIdentifier) = deleteRequest {
                token = accessToken
                this.id = id.toString()
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

            override suspend fun deleteBook(book: Book) {
                libraryChannel.deleteBook(newDeleteRequest(book.id))
            }

            override suspend fun addReader(reader: Reader) {
                val res =
                    libraryChannel.addReader(newAddRequest {
                        this.reader = reader.toProto()
                    })
                res.effect.maybeThrow()
            }

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

            override suspend fun addBorrow(borrower: Reader, book: Book, due: Instant) {
                val res =
                    libraryChannel.addBorrow(newAddRequest {
                        borrow = borrow {
                            id = UuidIdentifier().toString()
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
                            id = UuidIdentifier().toString()
                            readerId = borrower.id.toString()
                            bookIds.addAll(books.map { it.id.toString() })
                            time = Timestamp(Instant.now().toEpochMilli())
                            dueTime = Timestamp(due.toEpochMilli())
                        }
                    })
                res.effect.maybeThrow()
            }

            override suspend fun BorrowLike.setReturned(readerCredit: Float) {
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
                getReader(readerId)?.let {
                    updateReader(it.copy(creditability = readerCredit))
                }
            }

            override suspend fun connect() {
            }
        }

    private val adminComponent
        get() = object : ModAccountCapability {
            override val users: SnapshotStateList<User> = mutableStateListOf()

            override suspend fun connect() {
                coroutineScope.launch {
                    UniqueIdentifierStateList.bindTo(
                        users,
                        libraryChannel.getUsers(newGetRequest())
                            .filter { !it.end }
                            .map { IntegerIdentifier(it.id) to it.user.toModel() }
                    )
                }
            }

            override suspend fun inviteUser(
                role: UserRole,
                readerId: UuidIdentifier?,
            ): Flow<ModAccountCapability.TemporaryPassword> {
                val pwdChan = Channel<String>()
                val revChan = Channel<LibraryState.PasswordRequired.AuthResult>()
                state = LibraryState.PasswordRequired(pwdChan, revChan)

                return flow {
                    while (true) {
                        val pwd = try {
                            pwdChan.receive()
                        } catch (e: ClosedReceiveChannelException) {
                            break
                        }
                        val res = libraryChannel.addUser(addUserRequest {
                            deviceName = this@RemoteLibrary.deviceName
                            password = pwd
                            this.role = role
                            readerId?.toString()?.let { this.readerId = it }
                        })
                        var allowed = true
                        res.collect {
                            if (it.allowed) {
                                emit(it.toModel())
                            } else {
                                allowed = false
                            }
                        }
                        if (allowed) {
                            break
                        }
                    }
                }
            }

            override suspend fun updateUser(user: User) {
                val res = libraryChannel.updateUser(updateUserRequest {
                    token = accessToken
                    userId = user.id.id
                    this.user = user.toProto()
                })
                res.effect.maybeThrow()
            }

            override suspend fun deleteUser(user: User) {
                val pwdChan = Channel<String>()
                val revChan = Channel<LibraryState.PasswordRequired.AuthResult>()
                state = LibraryState.PasswordRequired(pwdChan, revChan)

                libraryChannel.deleteUser(deleteUserRequest {
                    userId = user.id.id

                })
            }
        }

    override fun Book.getStock(): UInt {
        val count by derivedStateOf { stock - borrows.count { it.hasBook(id) && it.returnTime == null }.toUInt() }
        return count
    }

    override val readers = SnapshotStateList<Reader>()
    override val books = SnapshotStateList<Book>()
    override val borrows = SnapshotStateList<BorrowLike>()

    override fun Reader.getBorrows(): List<BorrowLike> = borrows.filter { it.readerId == id }

    private fun newGetRequest() = getRequest { token = accessToken }

    private suspend fun signIn(): UserRole {
        if (context is DataSource.Context.WithToken && context.token != null) {
            val init = ByteString.copyFrom(context.token)
            val auth = authenticationRequest {
                token = init
            }
            val authResult = authenticationChannel.authenticate(auth)
            if (authResult.allowed) {
                accessToken = init
                return authResult.role
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
                return res.role
            } else {
                throw AccessDeniedException("Password authorization failed")
            }
        }

        if (!::accessToken.isInitialized) {
            val pwdChan = Channel<String>()
            val resChan = Channel<LibraryState.PasswordRequired.AuthResult>()
            state = LibraryState.PasswordRequired(pwdChan, resChan, cancelable = false)
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
                    return res.role
                }
                resChan.send(LibraryState.PasswordRequired.AuthResult(false, null))
            }
        }

        return UserRole.ROLE_UNSPECIFIC
    }

    override suspend fun connect() {
        val capturedState = state
        if (capturedState !is LibraryState.Initializing || capturedState.progress > 0) {
            // already connected or is connecting
            return
        }

        val role = signIn()
        when (role) {
            UserRole.ROLE_UNSPECIFIC, UserRole.UNRECOGNIZED, UserRole.ROLE_READER -> {}
            UserRole.ROLE_ADMIN -> components.add(librarianComponent, adminComponent)
            UserRole.ROLE_LIBRARIAN -> components.add(librarianComponent)
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
                    .map { UuidIdentifier.parse(it.id) to it.readerOrNull?.toModel() }
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
                    .map { UuidIdentifier.parse(it.id) to it.bookOrNull?.toModel() }
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
                        .map { UuidIdentifier.parse(it.id) to it.batchOrNull?.toModel() },
                    libraryChannel
                        .getBorrows(newGetRequest())
                        .onEach {
                            if (it.end) {
                                bumpEnded()
                            }
                        }
                        .filter { !it.end }
                        .map { UuidIdentifier.parse(it.id) to it.borrowOrNull?.toModel() }
                )
            ) {
                if (state !is LibraryState.Initializing) {
                    sorter.sortBorrows()
                }
            }
        }
    }

    override fun getBook(id: UuidIdentifier): Book? = books.firstOrNull { it.id == id }

    override fun getReader(id: UuidIdentifier): Reader? = readers.firstOrNull { it.id == id }

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