package com.cognifide.gradle.aem.internal

object Behaviors {

    fun waitUntil(condition: (Int, Int) -> Boolean) {
        waitUntil(condition, {}, 1000, 60 * 15)
    }

    fun waitUntil(condition: (Int, Int) -> Boolean, timeout: () -> Unit, attemptInterval: Long = 1000, attempts: Int = 60 * 15) {
        var attempt = 0
        while (condition(attempt, attempts)) {
            attempt++
            if (attempt == attempts) {
                timeout()
                break
            }

            Thread.sleep(attemptInterval)
        }
    }

}