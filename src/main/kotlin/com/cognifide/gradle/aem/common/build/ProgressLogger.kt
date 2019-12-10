package com.cognifide.gradle.aem.common.build

import java.util.*
import java.util.concurrent.TimeUnit
import org.apache.commons.lang3.time.StopWatch
import org.gradle.api.Project
import org.gradle.internal.logging.progress.ProgressLogger as BaseLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory

@Suppress("SpreadOperator")
open class ProgressLogger private constructor(private val project: Project) {

    var header: String = "Operation in progress"

    var progressEach: (String) -> Unit = { message -> project.logger.debug("$header: $message") }

    var progressWindow = TimeUnit.SECONDS.toMillis(1)

    private val baseParents: Queue<BaseLogger>
        get() = parents(project)

    private lateinit var base: BaseLogger

    private lateinit var stopWatch: StopWatch

    private fun create(): BaseLogger = InternalApi(project)
            .service(ProgressLoggerFactory::class)
            .newOperation(javaClass, baseParents.peek())

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
            return BuildScope.of(project).getOrPut("${ProgressLogger::class.java.canonicalName}_${project.path}") {
                LinkedList()
            }
        }
    }
}
