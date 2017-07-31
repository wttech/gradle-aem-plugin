package com.cognifide.gradle.aem.internal

object Behaviors {

    fun waitUntil(condition: (Int, Int) -> Boolean, intervalMilis: Long = 1000) {
        waitUntil(condition, intervalMilis, -1, {})
    }

    fun waitUntil(condition: (Int, Int) -> Boolean, attemptInterval: Long = 1000, attempts: Int = 60 * 5, timeout: () -> Unit) {
        var attempt = 0
        while (!condition(attempt, attempts)) {
            attempt++
            if (attempt == attempts) {
                timeout()
                break
            }

            Thread.sleep(attemptInterval)
        }
    }

}