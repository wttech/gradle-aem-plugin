package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.common.build.Behaviors
import com.cognifide.gradle.common.build.ProgressIndicator
import kotlinx.coroutines.isActive
import org.apache.commons.lang3.time.StopWatch
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CheckRunner(internal val aem: AemExtension) {

    internal val logger = aem.logger

    private val common = aem.common

    private var checks: CheckFactory.() -> List<Check> = { throw InstanceException("No instance checks defined!") }

    /**
     * Defines which checks should be performed (and repeated).
     */
    fun checks(definitions: CheckFactory.() -> List<Check>) {
        checks = definitions
    }

    private var progresses = listOf<CheckProgress>()

    /**
     * Get current checking progress of concrete instance.
     */
    fun progress(instance: Instance): CheckProgress {
        return progresses.firstOrNull { it.instance == instance }
                ?: throw InstanceException("No progress available for instance '${instance.name}'!")
    }

    /**
     * How long to wait after failed checking before checking again.
     */
    val delay = aem.obj.long { convention(0L) }

    /**
     * Definitive timeout for single check group execution.
     */
    val timeout = aem.obj.long { convention(10_000L) }

    /**
     * Controls if aborted running should fail build.
     */
    val verbose = aem.obj.boolean { convention(true) }

    /**
     * Time since running started.
     */
    val runningTime: Long get() = runningWatch.time

    private val runningWatch = StopWatch()

    /**
     * Error causing running stopped.
     */
    internal var abortCause: Exception? = null

    /**
     * Verify if running is stopped.
     */
    val aborted: Boolean get() = abortCause != null

    /**
     * Controls logging behavior
     *
     * By default it just keeps clean console if info logging level is not enabled.
     */
    val logInstantly = aem.obj.boolean { convention(logger.isInfoEnabled) }

    fun check(instances: Collection<Instance>) {
        common.progressIndicator {
            doChecking(instances)
            doAbort()
        }
    }

    @Suppress("LoopWithTooManyJumpStatements", "TooGenericExceptionCaught")
    private fun ProgressIndicator.doChecking(instances: Collection<Instance>) {
        step = "Checking"

        progresses = instances.map { CheckProgress(it) }
        updater {
            update(progresses.sortedBy { it.instance.name }.joinToString(" | ") {
                if (instances.size <= 2) it.summary else it.abbreviatedSummary
            })
        }

        runningWatch.start()

        val executors = Executors.newFixedThreadPool(progresses.size)
        try {
            common.parallel.each(progresses) { progress ->
                val instance = progress.instance
                progress.stateWatch.start()

                logger.info("Checking started for $instance")

                do {
                    if (aborted) {
                        logger.info("Checking aborted for $instance!")
                        break
                    }

                    val checks = try {
                        val future = executors.submit(Callable { doChecking(progress) })
                        future.get(timeout.get(), TimeUnit.MILLISECONDS)
                    } catch (e: Exception) {
                        logger.error("Checking failed for $instance!")
                        logger.debug("Checking error of $instance!", e)
                        null
                    }
                    if (checks != null && checks.done) {
                        logger.info("Checking done for $instance")
                        break
                    }

                    Behaviors.waitFor(delay.get())
                } while (isActive)

                logger.info("Checking ended for $instance")
            }
        } finally {
            executors.shutdownNow()
        }

        runningWatch.stop()
    }

    private fun doChecking(progress: CheckProgress): CheckGroup {
        val checks = check(progress.instance)
        progress.currentCheck = checks
        if (progress.stateChanged) {
            progress.stateChanges++
            progress.stateWatch.apply { reset(); start() }
        }
        progress.previousCheck = progress.currentCheck
        return checks
    }

    fun check(instance: Instance) = CheckGroup(this@CheckRunner, instance, this.checks).apply {
        check()
        if (logInstantly.get()) {
            log()
        }
    }

    private fun ProgressIndicator.doAbort() {
        if (aborted) {
            step = "Aborting"

            if (!logInstantly.get()) {
                progresses.forEach { it.currentCheck?.log() }
            }

            if (verbose.get()) {
                abortCause?.let { throw it }
            } else {
                logger.error("Checking error", abortCause)
            }
        }
    }
}
