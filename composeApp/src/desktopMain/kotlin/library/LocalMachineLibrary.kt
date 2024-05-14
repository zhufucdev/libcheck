package library

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import currentOS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import model.*
import java.io.File
import java.time.Instant

@OptIn(ExperimentalSerializationApi::class)
class LocalMachineLibrary : Library {
    private val workingDir: File get() = currentOS.dataDir
    private val booksFile get() = File(workingDir, "books.json")
    private val readerFile get() = File(workingDir, "readers.json")
    private val borrowFile get() = File(workingDir, "borrow.json")

    var bookList by mutableStateOf(SortedBookList(SnapshotStateList()))
    var readerList by mutableStateOf(SortedReaderList(SnapshotStateList()))
    var borrowList by mutableStateOf(SortedBorrowList(SnapshotStateList()))

    override val books: List<Book> get() = bookList.items
    override val readers: List<Reader> get() = readerList.items
    override val borrows: List<Borrow> get() = borrowList.items

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

    override val sorter: LibrarySorter by lazy {
        object : LibrarySorter {
            override var bookModel: SortModel<BookSortable> by mutableStateOf(bookList.model)
            override var readerModel: SortModel<ReaderSortable> by mutableStateOf(readerList.model)
            override var borrowModel: SortModel<BorrowSortable> by mutableStateOf(borrowList.model)

            override suspend fun sortBooks(order: SortOrder?, by: BookSortable?) {
                bookList = bookList.copy(
                    sortOrder = order ?: bookList.sortOrder,
                    sortedBy = by ?: bookList.sortedBy
                )
                bookList.sort(this@LocalMachineLibrary)
                bookModel = SortModel(bookList.sortOrder, bookList.sortedBy)
                saveBooks()
            }

            override suspend fun sortReaders(order: SortOrder?, by: ReaderSortable?) {
                readerList = readerList.copy(
                    sortedBy = by ?: readerList.sortedBy,
                    sortOrder = order ?: readerList.sortOrder
                )
                readerList.sort()
                readerModel = SortModel(readerList.sortOrder, readerList.sortedBy)
                saveReaders()
            }

            override suspend fun sortBorrows(order: SortOrder?, by: BorrowSortable?) {
                borrowList = borrowList.copy(
                    sortOrder = order ?: bookList.sortOrder,
                    sortedBy = by ?: borrowList.sortedBy
                )
                borrowList.sort(this@LocalMachineLibrary)
                borrowModel = SortModel(borrowList.sortOrder, borrowList.sortedBy)
                saveBorrows()
            }
        }
    }

    override suspend fun connect() = withContext(Dispatchers.IO) {
        mInitialized = false
        mProgress = 0f

        if (!workingDir.exists()) {
            workingDir.mkdirs()
        }
        booksFile.apply {
            if (exists()) {
                inputStream().use {
                    if (it.available() > 0) {
                        bookList = Json.decodeFromStream<SortedBookList>(it).let { s ->
                            s.copy(items = mutableStateListOf(*s.items.toTypedArray()))
                        }
                        return@apply
                    }
                }
            }
            bookList = SortedBookList(SnapshotStateList())
        }
        mProgress += 1 / 3f
        readerFile.apply {
            if (exists()) {
                inputStream().use {
                    if (it.available() > 0) {
                        readerList = Json.decodeFromStream<SortedReaderList>(it).let { s ->
                            s.copy(items = mutableStateListOf(*s.items.toTypedArray()))
                        }
                        return@apply
                    }
                }
            }
            readerList = SortedReaderList(SnapshotStateList())
        }
        mProgress += 1 / 3f
        borrowFile.apply {
            if (exists()) {
                inputStream().use {
                    if (it.available() > 0) {
                        borrowList = Json.decodeFromStream<SortedBorrowList>(it).let { s ->
                            s.copy(items = mutableStateListOf(*s.items.toTypedArray()))
                        }
                        return@apply
                    }
                }
            }
            borrowList = SortedBorrowList(SnapshotStateList())
        }
        mProgress += 1 / 3f
        mInitialized = true
    }

    override suspend fun addBook(book: Book) {
        bookList.items.add(book)
        saveBooks()
    }

    override suspend fun updateBook(book: Book) {
        val index = bookList.items.indexOfFirst { it.id == book.id }
        if (index < 0) {
            throw NoSuchElementException(book.name)
        }
        bookList.items[index] = book
        saveBooks()
    }

    override fun getBook(id: Identifier) = bookList.items.firstOrNull { it.id == id }

    override suspend fun deleteBook(book: Book) {
        val index = bookList.items.indexOfFirst { it.id == book.id }
        if (index < 0) {
            throw NoSuchElementException(book.name)
        }
        bookList.items.removeAt(index)
        saveBooks()
    }

    override suspend fun addBorrow(borrower: Reader, book: Book, due: Instant) {
        borrowList.items.add(Borrow(Identifier(), borrower.id, book.id, System.currentTimeMillis(), due.toEpochMilli()))
        saveBorrows()
    }

    override suspend fun Borrow.setReturned() {
        val index = borrowList.items.indexOf(this)
        if (index < 0) {
            throw NoSuchElementException()
        }
        borrowList.items[index] = copy(returnTime = Instant.now().toEpochMilli())
        saveBorrows()
    }

    override fun Book.getStock() =
        stock - borrowList.items.count { it.bookId == id && it.returnTime == null }.toUInt()

    override suspend fun addReader(reader: Reader) {
        readerList.items.add(reader)
        saveReaders()
    }

    override fun getReader(id: Identifier) = readerList.items.firstOrNull { it.id == id }

    override suspend fun updateReader(reader: Reader) {
        val index = readerList.items.indexOfFirst { it.id == reader.id }
        if (index < 0) {
            throw NoSuchElementException(reader.name)
        }
        readerList.items[index] = reader
        saveReaders()
    }

    override fun Reader.getBorrows(): List<Borrow> =
        borrowList.items.filter { it.readerId == id && it.returnTime == null }

    override suspend fun deleteReader(reader: Reader) {
        val index = readerList.items.indexOfFirst { it.id == reader.id }
        if (index < 0) {
            throw NoSuchElementException(reader.name)
        }
        readerList.items.removeAt(index)
        saveReaders()
    }

    override fun search(query: String): Flow<Searchable> = channelFlow {
        if (query.isEmpty()) {
            return@channelFlow
        }
        coroutineScope {
            launch {
                bookList.items.forEach {
                    if (it.matches(query)) {
                        channel.send(it)
                    }
                }
            }
            launch {
                readerList.items.forEach {
                    if (it.matches(query)) {
                        channel.send(it)
                    }
                }
            }
            launch {
                borrowList.items.forEach {
                    val ins = it.instance(this@LocalMachineLibrary)
                    if (ins.matches(query)) {
                        channel.send(ins)
                    }
                }
            }
        }
    }

    private suspend fun saveBooks() = withContext(Dispatchers.IO) {
        mSaving = true
        mProgress = 0f
        booksFile.outputStream().use {
            Json.encodeToStream(bookList, it)
        }
        mProgress = 1f
        mSaving = false
    }

    private suspend fun saveReaders() = withContext(Dispatchers.IO) {
        mSaving = true
        mProgress = 0f
        readerFile.outputStream().use {
            Json.encodeToStream(readerList, it)
        }
        mProgress = 1f
        mSaving = false
    }

    private suspend fun saveBorrows() = withContext(Dispatchers.IO) {
        mSaving = true
        mProgress = 0f
        borrowFile.outputStream().use {
            Json.encodeToStream(borrowList, it)
        }
        mProgress = 1f
        mSaving = false
    }
}