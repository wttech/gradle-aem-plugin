package com.cognifide.gradle.aem.common

class Retry private constructor(val aem: AemExtension) {

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

    @Suppress("TooGenericExceptionCaught")
    inline fun <T, reified E> launch(operation: String, block: (Long) -> T): T {
        lateinit var exception: Exception
        for (i in 0..times) {
            val no = i + 1

            try {
                return block(i)
            } catch (e: Exception) {
                exception = e

                if (e !is E) {
                    throw exception
                }

                if (i < times) {
                    val delay = delay(no)

                    aem.logger.lifecycle("Retrying ($no/$times) $operation after delay: ${Formats.duration(delay)}")
                    aem.logger.debug("Retrying due to exception", e)

                    ProgressCountdown(aem.project, delay).run()
                }
            }
        }

        throw exception
    }

    @Suppress("TooGenericExceptionCaught")
    inline fun <T, reified E> launchSimply(block: (Long) -> T): T {
        lateinit var exception: Exception
        for (i in 0..times) {
            val no = i + 1
            try {
                return block(i)
            } catch (e: Exception) {
                exception = e

                if (e !is E) {
                    throw exception
                }

                if (i < times) {
                    val delay = delay(no)
                    Thread.sleep(delay)
                }
            }
        }

        throw exception
    }

    companion object {
        fun none(aem: AemExtension) = Retry(aem)

        const val SECOND_MILIS = 1000L
    }
}