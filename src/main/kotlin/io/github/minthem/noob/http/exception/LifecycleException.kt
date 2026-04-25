package io.github.minthem.noob.http.exception

import io.github.minthem.noob.http.lifecycle.HookId

class LifecycleException internal constructor(
    message: String? = null,
    val causes: List<Pair<HookId, Throwable>> = emptyList(),
) : RuntimeException(message) {
    override fun toString(): String {
        val messageBuilder = StringBuilder()

        messageBuilder.append("LifecycleException: $message")

        causes.forEach { (hookId, cause) ->
            messageBuilder.append("\n  Hook: $hookId, Cause: $cause\n")
        }

        return messageBuilder.toString().trimEnd()
    }
}
