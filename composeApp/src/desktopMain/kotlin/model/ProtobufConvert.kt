package model

import com.sqlmaster.proto.LibraryOuterClass
import java.util.*

fun LibraryOuterClass.Book.toModel(): Book = Book(
    id = Identifier(UUID.fromString(id)),
    name = name,
    author = author,
    isbn = isbn,
    stock = stock.toUInt(),
    avatarUri = avatarUri
)

fun LibraryOuterClass.Reader.toModel(): Reader = Reader(
    id = Identifier(UUID.fromString(id)),
    name = name,
    avatarUri = avatarUri,
    tier = tier,
    creditability = creditability
)

fun LibraryOuterClass.Borrow.toModel(): Borrow = Borrow(
    id = Identifier(UUID.fromString(id)),
    readerId = Identifier(UUID.fromString(readerId)),
    bookId = Identifier(UUID.fromString(bookId)),
    time = time.seconds * 1000L + time.nanos / 1000000L,
    dueTime = dueTime.seconds * 1000L + dueTime.nanos / 1000000L,
    returnTime = returnTime?.let { it.seconds * 1000L + it.nanos / 1000000L }
)
