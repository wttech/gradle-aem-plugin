package com.cognifide.gradle.aem.base

class Retry private constructor() {

    var times = 1L

    var delay: (Long) -> Long = { 0L }

    fun retry(times: Long, delay: (Long) -> Long) {
        this.delay = delay
        this.times = times
    }

    fun afterSecond(times: Long) {
        retry(times) { SECOND_MILIS }
    }

    fun afterSquaredSecond(times: Long) {
        retry(times) { n -> n * n * SECOND_MILIS }
    }

    companion object {
        fun once() = Retry()

        const val SECOND_MILIS = 1000L
    }
}