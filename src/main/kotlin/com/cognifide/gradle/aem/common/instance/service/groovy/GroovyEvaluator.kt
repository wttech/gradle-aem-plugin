package com.cognifide.gradle.aem.common.instance.service.groovy

import com.cognifide.gradle.aem.AemExtension
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.StopWatch

class GroovyEvaluator(private val aem: AemExtension) {

    private val logger = aem.logger

    var scriptPattern: String = ""

    var scriptSuffix: String = ".groovy"

    var instances = aem.instances

    var data: Map<String, Any?> = mapOf()

    var faulty = true

    private var consoleOptions: GroovyConsole.() -> Unit = {}

    fun console(options: GroovyConsole.() -> Unit) {
        this.consoleOptions = options
    }

    @Suppress("ComplexMethod", "LongMethod")
    fun eval(): GroovyEvalSummary {
        if (scriptPattern.isBlank()) {
            throw GroovyConsoleException("Groovy script to be evaluated is not specified!")
        }
        if (instances.isEmpty()) {
            throw GroovyConsoleException("No instances defined for Groovy script evaluation!")
        }

        val scripts = instances.first().sync.groovyConsole.getScripts(StringUtils.appendIfMissing(scriptPattern, scriptSuffix))
        val statuses = mutableListOf<GroovyEvalStatus>()

        val stopWatch = StopWatch().apply { start() }
        aem.progress(instances.size * scripts.size) {
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

        val summary = GroovyEvalSummary(statuses, stopWatch.time)
        if (summary.failed > 0) {
            val failedStatuses = statuses.filter { it.fail }
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
}
