package library

import com.google.protobuf.Timestamp
import com.google.protobuf.timestamp
import com.sqlmaster.proto.*
import model.*

fun LibraryOuterClass.Book.toModel(): Book = Book(
    id = UuidIdentifier.parse(id),
    name = name,
    author = author,
    isbn = isbn,
    stock = stock.toUInt(),
    avatarUri = avatarUri
)

fun LibraryOuterClass.Reader.toModel(): Reader = Reader(
    id = UuidIdentifier.parse(id),
    name = name,
    avatarUri = avatarUri,
    tier = tier,
    creditability = creditability
)

fun LibraryOuterClass.Borrow.toModel(): Borrow = Borrow(
    id = UuidIdentifier.parse(id),
    readerId = UuidIdentifier.parse(readerId),
    bookId = UuidIdentifier.parse(bookId),
    time = time.toEpochMilli(),
    dueTime = dueTime.toEpochMilli(),
    returnTime = returnTime.takeIf { hasReturnTime() }?.toEpochMilli()
)

fun LibraryOuterClass.BorrowBatch.toModel(): BorrowBatch = BorrowBatch(
    id = UuidIdentifier.parse(id),
    readerId = UuidIdentifier.parse(readerId),
    bookIds = bookIdsList.map(UuidIdentifier.Companion::parse),
    time = time.toEpochMilli(),
    dueTime = dueTime.toEpochMilli(),
    returnTime = returnTime.takeIf { hasReturnTime() }?.toEpochMilli()
)

fun LibraryOuterClass.AddUserResponse.toModel() =
    AccountCapability.TemporaryPassword(password = temporaryPassword, expireSeconds = lifeSpanSeconds)

fun LibraryOuterClass.User.toModel() = User(
    id = IntegerIdentifier(id),
    deviceName = deviceName,
    role = role,
    readerId = readerId?.let { UuidIdentifier.parse(it) }
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

fun BorrowBatch.toProto(): LibraryOuterClass.BorrowBatch = borrowBatch {
    id = this@toProto.id.toString()
    readerId = this@toProto.readerId.toString()
    bookIds.addAll(this@toProto.bookIds.map(UuidIdentifier::toString))
    time = Timestamp(this@toProto.time)
    dueTime = Timestamp(this@toProto.dueTime)
    this@toProto.returnTime?.let { returnTime = Timestamp(it) }
}

fun User.toProto(): LibraryOuterClass.User = user {
    id = this@toProto.id.id
    deviceName = this@toProto.deviceName
    role = this@toProto.role
    this@toProto.readerId?.toString()?.let { readerId = it }
}