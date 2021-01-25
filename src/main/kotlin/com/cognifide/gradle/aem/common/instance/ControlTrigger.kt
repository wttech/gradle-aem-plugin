package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.common.build.Behaviors
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ControlTrigger(aem: AemExtension) {

    private val logger = aem.logger

    val repeatAfter = aem.obj.long {
        convention(90_000L)
        aem.prop.long("localInstance.controlTrigger.repeatAfter")?.let { set(it) }
    }

    val repeatTimes = aem.obj.int {
        convention(2)
        aem.prop.int("localInstance.controlTrigger.repeatTimes")?.let { set(it) }
    }

    val poolInterval = aem.obj.long {
        convention(1000L)
        aem.prop.long("localInstance.controlTrigger.poolInterval")?.let { set(it) }
    }

    val verifyTimeout = aem.obj.long {
        convention(1_000L)
        aem.prop.long("localInstance.controlTrigger.verifyTimeout")?.let { set(it) }
    }

    @Suppress("LoopWithTooManyJumpStatements", "TooGenericExceptionCaught")
    fun trigger(action: () -> Unit, verify: () -> Boolean, fail: () -> Unit) {
        var time = 0L
        var no = 0

        val executor = Executors.newSingleThreadExecutor()
        try {
            while (true) {
                if (time <= 0L || (System.currentTimeMillis() - time) >= repeatAfter.get()) {

                    action()
                    time = System.currentTimeMillis()
                    no++
                }
                Behaviors.waitFor(poolInterval.get())

                val verifyFuture = executor.submit(Callable { verify() })
                val verifyResult = try {
                    verifyFuture.get(verifyTimeout.get(), TimeUnit.MILLISECONDS)
                } catch (e: Exception) {
                    logger.debug("Cannot run control trigger verification properly!", e)
                    false
                }

                if (verifyResult) {
                    break
                }
                if (no == repeatTimes.get()) {
                    fail()
                    break
                }
            }
        } finally {
            executor.shutdownNow()
        }
    }
}
