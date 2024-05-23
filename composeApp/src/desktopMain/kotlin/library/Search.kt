package library

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

fun Library.searchFlow(query: String) = channelFlow {
    if (query.isNotEmpty()) {
        coroutineScope {
            launch {
                books.forEach {
                    if (it.matches(query)) {
                        channel.send(it)
                    }
                }
            }
            launch {
                readers.forEach {
                    if (it.matches(query)) {
                        channel.send(it)
                    }
                }
            }
            launch {
                borrows.forEach {
                    val ins = it.instance(this@searchFlow)
                    if (ins.matches(query)) {
                        channel.send(ins)
                    }
                }
            }
        }
    }
    close()
}