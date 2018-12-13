package com.cognifide.gradle.aem.common

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.apache.commons.lang3.time.StopWatch
import org.gradle.api.Project
import org.gradle.internal.logging.progress.ProgressLogger as BaseLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory

@Suppress("SpreadOperator")
open class ProgressLogger private constructor(val project: Project) {

    private lateinit var base: BaseLogger

    private lateinit var stopWatch: StopWatch

    var running = AtomicBoolean(false)

    var header: String = "Operation in progress"

    var progressEach: (String) -> Unit = { message -> project.logger.debug("$header: $message") }

    var progressWindow = TimeUnit.SECONDS.toMillis(1)

    private fun create(): BaseLogger {
        val serviceFactory = invoke(project, "getServices")
        val baseFactoryClass: Class<*> = ProgressLoggerFactory::class.java
        val baseFactory = invoke(serviceFactory, "get", baseFactoryClass)

        return invoke(baseFactory, "newOperation", javaClass) as BaseLogger
    }

    private operator fun invoke(obj: Any, method: String, vararg args: Any): Any {
        val argumentTypes = arrayOfNulls<Class<*>>(args.size)
        for (i in args.indices) {
            argumentTypes[i] = args[i].javaClass
        }
        val m = obj.javaClass.getMethod(method, *argumentTypes)
        m.isAccessible = true

        return m.invoke(obj, *args)
    }

    fun launch(block: ProgressLogger.() -> Unit) {
        if (running.get()) {
            apply(block)
        } else {
            try {
                if (running.compareAndSet(false, true)) {
                    stopWatch = StopWatch()
                    stopWatch.start()

                    base = create()
                    base.description = header
                    base.started()
                }

                apply(block)
            } finally {
                if (running.compareAndSet(true, false)) {
                    base.completed()
                    stopWatch.stop()
                }
            }
        }
    }

    fun progress(message: String) {
        base.progress(message)

        if (stopWatch.time >= progressWindow) {
            stopWatch.run { reset(); start() }
            progressEach(message)
        }
    }

    companion object {
        fun of(project: Project): ProgressLogger {
            return BuildScope.of(project).getOrPut(ProgressLogger::class.java.canonicalName, {
                ProgressLogger(project)
            })
        }
    }
}