package com.cognifide.gradle.aem.internal

object Behaviors {

    fun waitFor(duration: Int) {
        waitFor(duration.toLong())
    }

    fun waitFor(duration: Long) {
        Thread.sleep(duration)
    }

    fun waitUntil(interval: Int, condition: (Timer) -> Boolean) {
        waitUntil(interval.toLong(), condition)
    }

    fun waitUntil(interval: Long, condition: (Timer) -> Boolean) {
        val timer = Timer()
        while (condition(timer)) {
            waitFor(interval)
            timer.tick()
        }
    }

    class Timer {

        private var _started = time()

        private var _ticks = 0L

        private fun time(): Long {
            return System.currentTimeMillis()
        }

        fun reset() {
            this._ticks = 0
        }

        fun tick() {
            this._ticks++
        }

        val started: Long
            get() = _started

        val elapsed: Long
            get() = time() - _started

        val ticks: Long
            get() = _ticks

    }

}