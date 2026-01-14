package io.github.minthem.noobhttpserver.io

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal class TimeoutExecutor(
    private val scheduler: ScheduledExecutorService
) {

    fun <T> run(timeoutMs: Long, block: () -> T): T {
        if (timeoutMs <= 0) {
            return block()
        }

        val currentThread = Thread.currentThread()
        val future = scheduler.schedule({
            currentThread.interrupt()
        }, timeoutMs, TimeUnit.MILLISECONDS)

        try {
            val result = block()
            if(Thread.interrupted()) {
                // TODO 専用の例外作ることを検討
                throw TimeoutException("Operation timed out after $timeoutMs ms")
            }

            return result
        } catch (e: InterruptedException) {
            // TODO 専用の例外作ることを検討
            throw TimeoutException("Operation timed out after $timeoutMs ms")
        }finally {
            future.cancel(false)
            Thread.interrupted() // clear interrupted status
        }
    }
}