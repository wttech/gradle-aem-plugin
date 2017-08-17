package com.cognifide.gradle.aem.internal

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.internal.logging.progress.ProgressLoggerFactory

class ProgressLogger(val project: Project, header: String) {

    val logger: Logger = project.logger

    val progressLogger = create(header)

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
        invokeQuietly(progressLogger, "started")
    }

    fun progress(msg: String) {
        invokeQuietly(progressLogger, "progress", msg)
    }

    fun completed() {
        invokeQuietly(progressLogger, "completed")
    }

}