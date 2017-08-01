package com.cognifide.gradle.aem.internal

object Behaviors {

    fun waitFor(duration: Int) {
        waitFor(duration.toLong())
    }

    fun waitFor(duration: Long) {
        Thread.sleep(duration)
    }

    fun waitUntil(interval: Int, condition: (Long, Long) -> Boolean) {
        waitUntil(interval.toLong(), condition)
    }

    fun waitUntil(interval: Long, condition: (Long, Long) -> Boolean) {
        val started = System.currentTimeMillis()
        var attempt = 0L
        while (condition(System.currentTimeMillis() - started, attempt++)) {
            waitFor(interval)
        }
    }

}