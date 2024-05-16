package library

import com.google.protobuf.Timestamp
import com.google.protobuf.timestamp
import com.sqlmaster.proto.LibraryOuterClass
import com.sqlmaster.proto.book
import com.sqlmaster.proto.borrow
import com.sqlmaster.proto.reader
import model.Book
import model.Borrow
import model.Identifier
import model.Reader
import java.util.*

fun LibraryOuterClass.Book.toModel(): Book = Book(
    id = Identifier.parse(id),
    name = name,
    author = author,
    isbn = isbn,
    stock = stock.toUInt(),
    avatarUri = avatarUri
)

fun LibraryOuterClass.Reader.toModel(): Reader = Reader(
    id = Identifier.parse(id),
    name = name,
    avatarUri = avatarUri,
    tier = tier,
    creditability = creditability
)

fun LibraryOuterClass.Borrow.toModel(): Borrow = Borrow(
    id = Identifier.parse(id),
    readerId = Identifier(UUID.fromString(readerId)),
    bookId = Identifier(UUID.fromString(bookId)),
    time = time.toEpochMilli(),
    dueTime = dueTime.toEpochMilli(),
    returnTime = returnTime?.toEpochMilli()
)

fun Timestamp(millis: Long) = timestamp {
    seconds = millis / 1000
    nanos = (millis % 1000 * 1000).toInt()
}

fun Timestamp.toEpochMilli() = seconds * 1000L + nanos / 1000000L

fun Book.toProto(): LibraryOuterClass.Book = book {
    id = this@toProto.id.toString()
    name = this@toProto.name
    author = this@toProto.author
    isbn = this@toProto.isbn
    stock = this@toProto.stock.toInt()
    avatarUri = this@toProto.avatarUri
}

fun Reader.toProto(): LibraryOuterClass.Reader = reader {
    id = this@toProto.id.toString()
    name = this@toProto.name
    avatarUri = this@toProto.avatarUri
    tier = this@toProto.tier
    creditability = this@toProto.creditability
}

fun Borrow.toProto(): LibraryOuterClass.Borrow = borrow {
    id = this@toProto.id.toString()
    readerId = this@toProto.readerId.toString()
    bookId = this@toProto.bookId.toString()
    time = Timestamp(this@toProto.time)
    dueTime = Timestamp(this@toProto.dueTime)
    this@toProto.returnTime?.let { returnTime = Timestamp(it) }
}