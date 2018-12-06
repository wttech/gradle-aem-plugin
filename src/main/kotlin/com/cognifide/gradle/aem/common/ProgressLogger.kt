package com.cognifide.gradle.aem.common

import java.util.concurrent.TimeUnit
import org.apache.commons.lang3.time.StopWatch
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.internal.logging.progress.ProgressLogger as BaseLogger

@Suppress("SpreadOperator")
open class ProgressLogger(val project: Project, val header: String) {

    private val logger: Logger = project.logger

    private val base: BaseLogger = create(header)

    private var stopWatch = StopWatch()

    var progressEach: (String) -> Unit = { message -> logger.debug("$header: $message") }

    var progressWindow = TimeUnit.SECONDS.toMillis(1)

    private fun create(header: String): BaseLogger {
        val progressLoggerFactoryClass: Class<*> = ProgressLoggerFactory::class.java
        val serviceFactory = invoke(project, "getServices")
        val progressLoggerFactory = invoke(serviceFactory, "get", progressLoggerFactoryClass)

        val result = invoke(progressLoggerFactory, "newOperation", javaClass) as BaseLogger
        result.description = header

        return result
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
        stopWatch.start()
        base.started()
    }

    fun progress(message: String) {
        base.progress(message)

        if (stopWatch.time >= progressWindow) {
            stopWatch.run { reset(); start() }
            progressEach(message)
        }
    }

    fun completed() {
        base.completed()
        stopWatch.stop()
    }

    fun launch(block: ProgressLogger.() -> Unit) {
        started()
        block()
        completed()
    }
}