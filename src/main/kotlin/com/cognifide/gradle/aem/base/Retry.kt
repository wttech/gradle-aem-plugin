package com.cognifide.gradle.aem.base

class Retry private constructor() {

    var times = 1L

    var delay: (Long) -> Long = { 0L }

    fun retry(times: Long, delay: (Long) -> Long) {
        this.delay = delay
        this.times = times
    }

    fun afterSecond(times: Long) {
        retry(times) { 1000L }
    }

    fun afterSquaredSecond(times: Long) {
        retry(times) { n -> n * n * 1000L }
    }

    companion object {
        fun once() = Retry()
    }
}