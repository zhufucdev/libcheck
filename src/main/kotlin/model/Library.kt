package model

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalSerializationApi::class)
class Library(private val workingDir: File) {
    private val booksFile get() = File(workingDir, "books.json")
    private val readerFile get() = File(workingDir, "readers.json")
    private val borrowFile get() = File(workingDir, "borrow.json")

    var bookList by mutableStateOf(SortedBookList(SnapshotStateList()))
    var readerList by mutableStateOf(SortedReaderList(SnapshotStateList()))
    var borrowList by mutableStateOf(SortedBorrowList(SnapshotStateList()))

    private var mInitialized: Boolean by mutableStateOf(false)
    private var mInitProgress: Float by mutableStateOf(0f)
    private var mSaving: Boolean by mutableStateOf(false)
    val initialized by derivedStateOf { mInitialized }
    val loadProgress by derivedStateOf { mInitProgress }
    val saving by derivedStateOf { mSaving }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        mInitialized = false
        mInitProgress = 0f

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
        mInitProgress += 1 / 3f
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
        mInitProgress += 1 / 3f
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
        mInitProgress += 1 / 3f
        mInitialized = true
    }

    fun addBook(book: Book) {
        bookList.items.add(book)
    }

    fun updateBook(book: Book) {
        val index = bookList.items.indexOfFirst { it.id == book.id }
        if (index < 0) {
            throw NoSuchElementException(book.name)
        }
        bookList.items[index] = book
    }

    fun getBook(id: Identifier) = bookList.items.firstOrNull { it.id == id }

    fun sortBooks(order: SortOrder, by: BookSortable) {
        bookList = bookList.copy(sortOrder = order, sortedBy = by)
        bookList.sort(this)
    }

    fun deleteBook(book: Book) {
        val index = bookList.items.indexOfFirst { it.id == book.id }
        if (index < 0) {
            throw NoSuchElementException(book.name)
        }
        bookList.items.removeAt(index)
    }

    fun borrow(borrower: Reader, book: Book, due: Instant) {
        borrowList.items.add(Borrow(Identifier(), borrower.id, book.id, System.currentTimeMillis(), due.toEpochMilli()))
    }

    val Book.inStock
        get() = stock - borrowList.items.count { it.bookId == id && !it.returned }.toUInt()

    fun addReader(reader: Reader) {
        readerList.items.add(reader)
    }

    fun getReader(id: Identifier) = readerList.items.firstOrNull { it.id == id }

    fun updateReader(reader: Reader) {
        val index = readerList.items.indexOfFirst { it.id == reader.id }
        if (index < 0) {
            throw NoSuchElementException(reader.name)
        }
        readerList.items[index] = reader
    }

    val Reader.borrows get() = borrowList.items.count { it.readerId == id && !it.returned }

    fun sortReaders(order: SortOrder, by: ReaderSortable) {
        readerList = readerList.copy(sortedBy = by, sortOrder = order)
        readerList.sort()
    }

    fun deleteReader(reader: Reader) {
        val index = readerList.items.indexOfFirst { it.id == reader.id }
        if (index < 0) {
            throw NoSuchElementException(reader.name)
        }
        readerList.items.removeAt(index)
    }

    fun sortBorrows(order: SortOrder, by: BorrowSortable) {
        borrowList = borrowList.copy(sortOrder = order, sortedBy = by)
        borrowList.sort(this)
    }

    suspend fun writeToFile() = withContext(Dispatchers.IO) {
        mSaving = true
        booksFile.outputStream().use {
            Json.encodeToStream(bookList, it)
        }
        mInitProgress += 1 / 3f
        readerFile.outputStream().use {
            Json.encodeToStream(readerList, it)
        }
        mInitProgress += 1 / 3f
        borrowFile.outputStream().use {
            Json.encodeToStream(borrowList, it)
        }
        mInitProgress += 1 / 3f
        mSaving = false
    }
}