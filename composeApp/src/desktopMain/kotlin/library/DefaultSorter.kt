package library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import model.*

class DefaultSorter(private val context: DataSource.Context, private val parent: Library) : LibrarySortingModel {
    override var bookModel: SortModel<BookSortable> by mutableStateOf(
        if (context is DataSource.Context.WithSortModel)
            context.sortModel.books
        else SortModel(
            order = SortOrder.ASCENDING,
            by = BookSortable.ID
        )
    )
    override var readerModel: SortModel<ReaderSortable> by mutableStateOf(
        if (context is DataSource.Context.WithSortModel)
            context.sortModel.readers
        else SortModel(
            order = SortOrder.ASCENDING,
            by = ReaderSortable.ID
        )
    )
    override var borrowModel: SortModel<BorrowSortable> by mutableStateOf(
        if (context is DataSource.Context.WithSortModel)
            context.sortModel.borrows
        else SortModel(
            order = SortOrder.ASCENDING,
            by = BorrowSortable.BORROW_TIME
        )
    )

    override suspend fun sortBooks(order: SortOrder?, by: BookSortable?) {
        val list = SortedBookList(
            parent.books as MutableList<Book>,
            sortedBy = by ?: bookModel.by,
            sortOrder = order ?: bookModel.order
        )
        list.sort(parent)
        bookModel = list.model
        if (context is DataSource.Context.WithSortModel) {
            context.sortModel = context.sortModel.copy(books = list.model)
            context.save()
        }
    }

    override suspend fun sortReaders(order: SortOrder?, by: ReaderSortable?) {
        val list = SortedReaderList(
            parent.readers as MutableList<Reader>,
            sortedBy = by ?: readerModel.by,
            sortOrder = order ?: readerModel.order
        )
        list.sort()
        readerModel = list.model
        if (context is DataSource.Context.WithSortModel) {
            context.sortModel = context.sortModel.copy(readers = list.model)
            context.save()
        }
    }

    override suspend fun sortBorrows(order: SortOrder?, by: BorrowSortable?) {
        val list = SortedBorrowList(
            parent.borrows as MutableList<BorrowLike>,
            sortedBy = by ?: borrowModel.by,
            sortOrder = order ?: borrowModel.order
        )
        list.sort(parent)
        borrowModel = list.model
        if (context is DataSource.Context.WithSortModel) {
            context.sortModel = context.sortModel.copy(borrows = list.model)
            context.save()
        }
    }
}
