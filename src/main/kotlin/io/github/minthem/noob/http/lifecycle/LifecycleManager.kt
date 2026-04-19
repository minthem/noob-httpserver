package io.github.minthem.noob.http.lifecycle

import io.github.minthem.noob.http.exception.LifecycleException
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal typealias HookId = String

@OptIn(ExperimentalAtomicApi::class)
internal class LifecycleManager {

    private val hooksRef = AtomicReference<LinkedHashMap<HookId, LifecycleEvent>>(LinkedHashMap())
    private val isStarted = AtomicBoolean(false)
    private val isStopped = AtomicBoolean(false)

    @OptIn(ExperimentalUuidApi::class)
    private val nextId: HookId
        get() = Uuid.generateV4().toHexString()


    fun register(hook: LifecycleEvent): HookId {
        if (isStarted.load()) {
            throw IllegalStateException("Cannot register hooks after start")
        }
        var id = nextId
        hooksRef.update {
            var candidateId = id
            while (it.containsKey(candidateId)) {
                candidateId = nextId
            }
            id = candidateId

            val updated = LinkedHashMap(it)
            updated[id] = hook
            updated
        }

        return id
    }

    fun unregister(id: HookId) {
        if (isStarted.load()) {
            throw IllegalStateException("Cannot unregister hooks after start")
        }
        hooksRef.update {
            val updated = LinkedHashMap(it)
            updated.remove(id)
            updated
        }
    }

    fun startAll() {
        if (!isStarted.compareAndSet(expectedValue = false, newValue = true)) {
            throw IllegalStateException("Cannot start hooks twice")
        }

        val errors = mutableListOf<Pair<HookId, Throwable>>()
        hooksRef.load().forEach { (id, hook) ->
            runCatching { hook.onStart() }.onFailure { e -> errors.add(id to e) }
        }

        if (errors.isNotEmpty()) {
            throw LifecycleException(
                "Failed to start lifecycle hooks",
                errors
            )
        }
    }

    fun stopAll() {
        if (!isStarted.load()) throw IllegalStateException("Cannot stop hooks before server start")
        if (!isStopped.compareAndSet(expectedValue = false, newValue = true)) {
            throw IllegalStateException("Cannot stop hooks twice")
        }

        val errors = mutableListOf<Pair<HookId, Throwable>>()
        hooksRef.load().reversed().forEach { (id, hook) ->
            runCatching { hook.onStop() }.onFailure { e -> errors.add(id to e) }
        }

        if (errors.isNotEmpty()) {
            throw LifecycleException(
                "Failed to stop lifecycle hooks",
                errors
            )
        }
    }
}