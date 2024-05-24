package model

import com.sqlmaster.proto.LibraryOuterClass.Session
import com.sqlmaster.proto.LibraryOuterClass.UserRole
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Instant

class LibraryComponentsCollection(vararg component: LibraryComponent) {
    private val _list = mutableListOf(*component)
    val list: List<LibraryComponent>
        get() = _list

    fun add(vararg component: LibraryComponent) {
        _list.addAll(component)
    }

    fun remove(component: LibraryComponent) {
        _list.remove(component)
    }

    suspend fun connectAll() {
        coroutineScope {
            list.forEach {
                launch {
                    it.connect()
                }
            }
        }
    }

    inline fun <reified T : LibraryComponent> of() = list.firstOrNull { it is T }?.let { it as T }
    inline fun <reified T : LibraryComponent> has() = list.any { it is T }
}

sealed interface LibraryComponent {
    suspend fun connect()
}

interface ModificationCapability : LibraryComponent {
    suspend fun addBook(book: Book)
    suspend fun updateBook(book: Book)
    suspend fun deleteBook(book: Book)
    suspend fun addReader(reader: Reader)
    suspend fun updateReader(reader: Reader)
    suspend fun deleteReader(reader: Reader)
}

interface BorrowCapability : LibraryComponent {
    suspend fun addBorrow(borrower: Reader, book: Book, due: Instant)
    suspend fun addBorrowBatch(borrower: Reader, books: List<Book>, due: Instant)
}

interface ReturnCapability : LibraryComponent {
    suspend fun BorrowLike.setReturned(readerCredit: Float)
}

interface AccountCapability : LibraryComponent {
    enum class ChangePasswordResult {
        OK, Forbidden, Invalid
    }

    val account: Flow<User>
    val sessions: List<Session>
    suspend fun changePassword(oldPassword: String, newPassword: String): ChangePasswordResult
    suspend fun revokeSession(session: Session)
}

interface ModAccountCapability : LibraryComponent {
    data class TemporaryPassword(val password: String, val expireSeconds: Int, val createTime: Instant = Instant.now())

    val users: List<User>

    suspend fun inviteUser(role: UserRole, readerId: UuidIdentifier? = null): Flow<TemporaryPassword>
    suspend fun updateUser(user: User)
    suspend fun deleteUser(user: User)
}

