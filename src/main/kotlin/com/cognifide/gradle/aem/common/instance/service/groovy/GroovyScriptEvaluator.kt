package com.cognifide.gradle.aem.common.instance.service.groovy

import com.cognifide.gradle.aem.AemExtension
import org.apache.commons.lang3.time.StopWatch

class GroovyScriptEvaluator(private val aem: AemExtension) {

    private val logger = aem.logger

    var scriptPattern: String = aem.prop.string("instance.groovyScript") ?: ""

    var failable = aem.prop.boolean("instance.groovyScript.failable") ?: true

    var instances = aem.instances

    var data: Map<String, Any> = mapOf()

    @Suppress("ComplexMethod")
    fun eval(): GroovyScriptSummary {
        if (scriptPattern.isBlank()) {
            throw GroovyConsoleException("Groovy script to be evaluated is not specified!")
        }
        if (instances.isEmpty()) {
            throw GroovyConsoleException("No instances defined for Groovy script evaluation!")
        }

        val scripts = instances.first().sync.groovyConsole.getScripts(scriptPattern)
        val statuses = mutableListOf<GroovyScriptStatus>()

        val stopWatch = StopWatch().apply { start() }
        aem.progress(instances.size * scripts.size) {
            step = "Validating"
            aem.sync(instances) {
                groovyConsole.requireAvailable()
            }

            step = "Evaluating"
            aem.sync(instances) {
                scripts.forEach { script ->
                    increment("Script '${script.name}' on '${instance.name}'")

                    groovyConsole.evalScript(script, data).apply {
                        statuses.add(GroovyScriptStatus(script, instance, success))

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
                        }
                        logger.info(message.joinToString("\n"))
                    }
                }
            }
        }
        stopWatch.stop()

        val summary = GroovyScriptSummary(statuses, stopWatch.time)
        if (summary.failed > 0) {
            logger.error("Groovy script evaluation errors:\n${summary.statuses.joinToString("\n") { "Script '${it.script}' on ${it.instance}" }}")
            if (failable) {
                throw GroovyConsoleException("Groovy script evaluation ended with ${summary.failed} error(s)!")
            }
        }

        return summary
    }
}
