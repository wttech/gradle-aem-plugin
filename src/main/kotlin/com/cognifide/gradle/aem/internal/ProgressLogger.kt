package com.cognifide.gradle.aem.internal

import java.util.concurrent.TimeUnit
import org.apache.commons.lang3.time.StopWatch
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.internal.logging.progress.ProgressLoggerFactory

open class ProgressLogger(val project: Project, val header: String) {

    private val logger: Logger = project.logger

    private val progressLogger = create(header)

    private var stopWatch = StopWatch()

    var progressEach: (String) -> Unit = { message -> logger.debug("$header: $message") }

    var progressWindow = TimeUnit.SECONDS.toMillis(1)

    private fun create(header: String): Any {
        val progressLoggerFactoryClass: Class<*> = ProgressLoggerFactory::class.java
        val serviceFactory = invoke(project, "getServices")
        val progressLoggerFactory = invoke(serviceFactory, "get", progressLoggerFactoryClass)

        val result = invoke(progressLoggerFactory, "newOperation", javaClass)
        invoke(result, "setDescription", header)
        invoke(result, "setLoggingHeader", header)

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

    private fun invokeQuietly(obj: Any, method: String, vararg args: Any) {
        try {
            invoke(obj, method, *args)
        } catch (e: Exception) {
            logger.trace("Unable to log progress", e)
        }
    }

    fun started() {
        stopWatch.start()
        invokeQuietly(progressLogger, "started")
    }

    fun progress(message: String) {
        invokeQuietly(progressLogger, "progress", message)

        if (stopWatch.time >= progressWindow) {
            stopWatch.run { reset(); start() }
            progressEach(message)
        }
    }

    fun completed() {
        invokeQuietly(progressLogger, "completed")
        stopWatch.stop()
    }
}