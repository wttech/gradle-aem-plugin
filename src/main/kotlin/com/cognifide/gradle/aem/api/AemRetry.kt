package com.cognifide.gradle.aem.api

class AemRetry {

    var times = 0L

    var delay: (Long) -> Long = { 0L }

    fun repeat(delay: (Long) -> Long, times: Long) {
        this.delay = delay
        this.times = times
    }

    fun afterSecond(times: Long) {
        repeat({ 1000L }, times)
    }

    fun afterSquaredSecond(times: Long) {
        repeat({ n -> n * n * 1000L }, times)
    }

}