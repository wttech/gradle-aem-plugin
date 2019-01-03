package com.cognifide.gradle.aem.common

import java.util.*
import java.util.concurrent.TimeUnit
import org.apache.commons.lang3.time.StopWatch
import org.gradle.api.Project
import org.gradle.internal.logging.progress.ProgressLogger as BaseLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory

@Suppress("SpreadOperator")
open class ProgressLogger private constructor(val project: Project) {

    var header: String = "Operation in progress"

    var progressEach: (String) -> Unit = { message -> project.logger.debug("$header: $message") }

    var progressWindow = TimeUnit.SECONDS.toMillis(1)

    private val baseParents: Queue<BaseLogger>
        get() = parents(project)

    private lateinit var base: BaseLogger

    private lateinit var stopWatch: StopWatch

    private fun create(): BaseLogger {
        val serviceFactory = invoke(project, "getServices")
        val baseFactoryClass: Class<*> = ProgressLoggerFactory::class.java
        val baseFactory = invoke(serviceFactory, "get", baseFactoryClass)

        return invoke(
                baseFactory,
                "newOperation",
                listOf(javaClass, baseParents.peek()),
                listOf(javaClass.javaClass, BaseLogger::class.java)
        ) as BaseLogger
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

    private fun invoke(obj: Any, method: String, args: List<Any?>, argTypes: List<Class<out Any>>): Any {
        val m = obj.javaClass.getMethod(method, *argTypes.toTypedArray())
        m.isAccessible = true

        return m.invoke(obj, *args.toTypedArray())
    }

    fun <T> launch(block: ProgressLogger.() -> T): T {
        stopWatch = StopWatch()
        base = create()
        baseParents.add(base)

        try {
            stopWatch.start()
            base.description = header
            base.started()

            return run(block)
        } finally {
            base.completed()
            baseParents.remove(base)
            stopWatch.stop()
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
            return ProgressLogger(project)
        }

        fun parents(project: Project): Queue<BaseLogger> {
            return BuildScope.of(project).getOrPut("${ProgressLogger::class.java.canonicalName}_${project.path}", {
                LinkedList()
            })
        }
    }
}