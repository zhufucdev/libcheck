package library

import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.flow.Flow
import model.Identifiable
import model.Identifier

suspend fun <T : Identifiable> UniqueIdentifierStateList(flow: Flow<Pair<Identifier, T?>>): SnapshotStateList<T> {
    val list = SnapshotStateList<T>()
    UniqueIdentifierStateList.bindTo(list, flow)
    return list
}

object UniqueIdentifierStateList {
    suspend fun <T : Identifiable> bindTo(
        list: SnapshotStateList<T>,
        flow: Flow<Pair<Identifier, T?>>,
        onUpdate: (suspend (SnapshotStateList<T>) -> Unit)? = null,
    ) {
        flow.collect { next ->
            val value = next.second
            if (value != null) {
                val index = list.indexOfFirst { it.id == next.first }
                if (index >= 0) {
                    list[index] = value
                } else {
                    list.add(value)
                }
                onUpdate?.invoke(list)
            } else {
                val removed = list.removeIf { it.id == next.first }
                if (removed) {
                    onUpdate?.invoke(list)
                }
            }
        }
    }
}
