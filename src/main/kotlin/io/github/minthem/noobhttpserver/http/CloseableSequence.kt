package io.github.minthem.noobhttpserver.http

interface CloseableSequence<T> : Sequence<T>, AutoCloseable

fun <T> Sequence<T>.asCloseable(closeAction: () -> Unit): CloseableSequence<T> {
    return object : CloseableSequence<T> {
        override fun iterator(): Iterator<T> = this@asCloseable.iterator()
        override fun close() = closeAction()
    }
}
