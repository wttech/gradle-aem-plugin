package com.cognifide.gradle.aem.common.instance.service.groovy

import com.cognifide.gradle.aem.AemExtension
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.StopWatch
import java.io.File

class GroovyEvaluator(private val aem: AemExtension) {

    private val common = aem.common

    private val logger = aem.logger

    val scriptDirDefault
        get() = aem.project.version.toString().removeSuffix("-SNAPSHOT")

    var scriptPattern: String = "$scriptDirDefault/**/*"

    var scriptSuffix: String = ".groovy"

    var instances = aem.instances

    var data: Map<String, Any?> = mapOf()

    var faulty = true

    private var consoleOptions: GroovyConsole.() -> Unit = {}

    fun console(options: GroovyConsole.() -> Unit) {
        this.consoleOptions = options
    }

    fun eval() = evalScripts(findScripts())

    fun findScripts(): List<File> {
        if (scriptPattern.isBlank()) {
            throw GroovyConsoleException("Groovy script to be evaluated is not specified!")
        }
        if (instances.isEmpty()) {
            throw GroovyConsoleException("No instances defined for Groovy script evaluation!")
        }

        return instances.first().sync.groovyConsole.findScripts(StringUtils.appendIfMissing(scriptPattern, scriptSuffix))
    }

    fun evalScripts(scripts: List<File>): GroovyEvalSummary {
        val summary = evalScriptsInternal(scripts)
        if (summary.failed > 0) {
            val failedStatuses = summary.statuses.filter { it.fail }
            val failMessages = failedStatuses.mapIndexed { no, status ->
                "${no + 1}) '${status.script}' on ${status.instance}:\n${status.error}"
            }
            val failMessage = "Groovy script evaluation errors (${failedStatuses.size}):\n${failMessages.joinToString("\n")}"

            if (faulty) {
                throw GroovyConsoleException(failMessage)
            } else {
                logger.error(failMessage)
            }
        }

        return summary
    }

    private fun evalScriptsInternal(scripts: List<File>): GroovyEvalSummary {
        if (scripts.isEmpty()) {
            return GroovyEvalSummary.empty()
        }

        val statuses = mutableListOf<GroovyEvalStatus>()

        val stopWatch = StopWatch().apply { start() }
        common.progress(instances.size * scripts.size) {
            step = "Validating"
            aem.sync(instances) {
                groovyConsole.requireAvailable()
            }

            step = "Evaluating"
            aem.sync(instances) {
                groovyConsole.apply(consoleOptions)

                scripts.forEach { script ->
                    increment("Script '${script.name}' on '${instance.name}'")

                    groovyConsole.evalScript(script, data).apply {
                        val message = mutableListOf<String>().apply {
                            if (success) {
                                add("Groovy script '$script' evaluated with success in '$runningTime' on $instance")
                            } else {
                                add("Groovy script '$script' evaluated with error on $instance")
                            }

                            if (logger.isInfoEnabled) {
                                result.orEmpty().trim().takeIf { it.isNotEmpty() }?.let {
                                    add("Groovy script '$script' result:\n$it")
                                }
                                exceptionStackTrace.trim().takeIf { it.isNotEmpty() }?.let {
                                    add("Groovy script '$script' exception:\n$it")
                                }
                                output.trim().takeIf { it.isNotEmpty() }?.let {
                                    add("Groovy script '$script' output:\n$it")
                                }
                            }
                        }.joinToString("\n")

                        when {
                            success -> logger.info(message)
                            else -> logger.error(message)
                        }

                        val error = exceptionStackTrace.trim().lineSequence().firstOrNull().orEmpty()
                        statuses.add(GroovyEvalStatus(script, instance, success, error))
                    }
                }
            }
        }
        stopWatch.stop()

        return GroovyEvalSummary(statuses, stopWatch.time)
    }
}
