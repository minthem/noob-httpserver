package io.github.minthem.noobhttpserver.lifecycle

interface LifecycleEvent {

    fun onStart()

    fun onStop()
}
