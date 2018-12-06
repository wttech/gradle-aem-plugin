package com.cognifide.gradle.aem.common

import java.util.concurrent.TimeUnit
import org.apache.commons.lang3.time.StopWatch
import org.gradle.api.Project
import org.gradle.internal.logging.progress.ProgressLogger as BaseLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory

@Suppress("SpreadOperator")
open class ProgressLogger(val project: Project) {

    private lateinit var base: BaseLogger

    private lateinit var stopWatch: StopWatch

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

    fun started() {
        stopWatch = StopWatch()
        stopWatch.start()

        base = create()
        base.description = header
        base.started()
    }

    val started: Boolean
        get() = stopWatch.isStarted

    fun progress(message: String) {
        if (!started) {
            return
        }

        base.progress(message)

        if (stopWatch.time >= progressWindow) {
            stopWatch.run { reset(); start() }
            progressEach(message)
        }
    }

    fun completed() {
        if (!started) {
            return
        }

        base.completed()
        stopWatch.stop()
    }

    fun launch(block: ProgressLogger.() -> Unit) {
        try {
            started()
            block()
        } finally {
            completed()
        }
    }

    fun hold(block: ProgressLogger.() -> Unit) {
        try {
            completed()
            block()
        } finally {
            started()
        }
    }
}