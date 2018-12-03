package com.cognifide.gradle.aem.base

class Retry private constructor() {

    var times = 0L

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
        fun none() = Retry()

        const val SECOND_MILIS = 1000L
    }
}