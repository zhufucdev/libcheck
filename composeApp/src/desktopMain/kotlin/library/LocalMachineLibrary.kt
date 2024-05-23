package library

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import currentPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import model.*
import java.io.File
import java.time.Instant

@OptIn(ExperimentalSerializationApi::class)
class LocalMachineLibrary(private val context: DataSource.Context) : Library {
    private val workingDir by lazy { if (context is DataSource.Context.WithRootPath) File(context.defaultRootPath) else currentPlatform.dataDir }
    private val booksFile get() = File(workingDir, "books.json")
    private val readerFile get() = File(workingDir, "readers.json")
    private val borrowFile get() = File(workingDir, "borrow.json")

    override val books = SnapshotStateList<Book>()
    override val readers = SnapshotStateList<Reader>()
    override val borrows = SnapshotStateList<BorrowLike>()

    private var mInitialized: Boolean by mutableStateOf(false)
    private var mProgress: Float by mutableStateOf(0f)
    private var mSaving: Boolean by mutableStateOf(false)
    override val state: LibraryState by derivedStateOf {
        if (mSaving) {
            LibraryState.Synchronizing(mProgress, true)
        } else if (!mInitialized) {
            LibraryState.Initializing(mProgress)
        } else {
            LibraryState.Idle
        }
    }

    override val sorter: LibrarySortingModel by lazy {
        DefaultSorter(context, this)
    }
    override val components = LibraryComponentsCollection(defaultComponent)

    override suspend fun connect() = withContext(Dispatchers.IO) {
        mInitialized = false
        mProgress = 0f

        if (!workingDir.exists()) {
            workingDir.mkdirs()
        }

        components.connectAll()
        booksFile.apply {
            if (exists()) {
                inputStream().use {
                    if (it.available() > 0) {
                        books.addAll(Json.decodeFromStream<List<Book>>(it))
                        return@apply
                    }
                }
            }
        }
        mProgress += 1 / 3f
        readerFile.apply {
            if (exists()) {
                inputStream().use {
                    if (it.available() > 0) {
                        readers.addAll(Json.decodeFromStream<List<Reader>>(it))
                    }
                }
            }
        }
        mProgress += 1 / 3f
        borrowFile.apply {
            if (exists()) {
                inputStream().use {
                    if (it.available() > 0) {
                        borrows.addAll(Json.decodeFromStream<List<BorrowLike>>(it))
                    }
                }
            }
        }
        mProgress += 1 / 3f
        mInitialized = true
    }

    private val defaultComponent
        get() = object : ModificationCapability, BorrowCapability, ReturnCapability {
            override suspend fun addBook(book: Book) {
                books.add(book)
                sorter.sortBooks()
                saveBooks()
            }

            override suspend fun updateBook(book: Book) {
                val index = books.indexOfFirst { it.id == book.id }
                if (index < 0) {
                    throw NoSuchElementException(book.name)
                }
                books[index] = book
                sorter.sortBooks()
                saveBooks()
            }


            override suspend fun deleteBook(book: Book) {
                val index = books.indexOfFirst { it.id == book.id }
                if (index < 0) {
                    throw NoSuchElementException(book.name)
                }
                books.removeAt(index)
                saveBooks()
            }

            override suspend fun addBorrow(borrower: Reader, book: Book, due: Instant) {
                borrows.add(Borrow(UuidIdentifier(), borrower.id, book.id, System.currentTimeMillis(), due.toEpochMilli()))
                sorter.sortBorrows()
                saveBorrows()
            }

            override suspend fun addBorrowBatch(borrower: Reader, books: List<Book>, due: Instant) {
                val batch =
                    BorrowBatch(UuidIdentifier(), borrower.id, books.map(Book::id), System.currentTimeMillis(), due.toEpochMilli())
                borrows.add(batch)
                sorter.sortBorrows()
                saveBorrows()
            }

            override suspend fun BorrowLike.setReturned(readerCredit: Float) {
                val index = borrows.indexOf(this)
                if (index < 0) {
                    throw NoSuchElementException()
                }
                borrows[index] = when (this) {
                    is Borrow -> copy(returnTime = Instant.now().toEpochMilli())
                    is BorrowBatch -> copy(returnTime = Instant.now().toEpochMilli())
                }
                sorter.sortBorrows()
                saveBorrows()
                getReader(readerId)?.let {
                    updateReader(it.copy(creditability = readerCredit))
                }
            }

            override suspend fun connect() {
            }

            override suspend fun addReader(reader: Reader) {
                readers.add(reader)
                sorter.sortReaders()
                saveReaders()
            }

            override suspend fun updateReader(reader: Reader) {
                val index = readers.indexOfFirst { it.id == reader.id }
                if (index < 0) {
                    throw NoSuchElementException(reader.name)
                }
                readers[index] = reader
                saveReaders()
            }

            override suspend fun deleteReader(reader: Reader) {
                val index = readers.indexOfFirst { it.id == reader.id }
                if (index < 0) {
                    throw NoSuchElementException(reader.name)
                }
                readers.removeAt(index)
                saveReaders()
            }
        }

    override fun getBook(id: UuidIdentifier) = books.firstOrNull { it.id == id }

    override fun Book.getStock() = stock - borrows.count { it.hasBook(id) && it.returnTime == null }.toUInt()

    override fun getReader(id: UuidIdentifier) = readers.firstOrNull { it.id == id }

    override fun Reader.getBorrows(): List<BorrowLike> = borrows.filter { it.readerId == id && it.returnTime == null }

    override fun search(query: String): Flow<Searchable> = searchFlow(query)

    override fun close() {
        if (mSaving) {
            throw IllegalStateException("cannot close while saving")
        }
        books.clear()
        readers.clear()
        borrows.clear()
    }

    private suspend fun saveBooks() = withContext(Dispatchers.IO) {
        mSaving = true
        mProgress = 0f
        booksFile.outputStream().use {
            Json.encodeToStream<List<Book>>(books, it)
        }
        mProgress = 1f
        mSaving = false
    }

    private suspend fun saveReaders() = withContext(Dispatchers.IO) {
        mSaving = true
        mProgress = 0f
        readerFile.outputStream().use {
            Json.encodeToStream<List<Reader>>(readers, it)
        }
        mProgress = 1f
        mSaving = false
    }

    private suspend fun saveBorrows() = withContext(Dispatchers.IO) {
        mSaving = true
        mProgress = 0f
        borrowFile.outputStream().use {
            Json.encodeToStream<List<BorrowLike>>(borrows, it)
        }
        mProgress = 1f
        mSaving = false
    }
}