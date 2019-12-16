package com.cognifide.gradle.aem.common.instance.service.groovy

import com.cognifide.gradle.aem.AemExtension

class GroovyConsoleEvaluator(private val aem: AemExtension) {

    private val logger = aem.logger

    var scriptPattern: String = aem.prop.string("instance.groovyScript") ?: ""

    var instances = aem.instances

    var data: Map<String, Any> = mapOf()

    fun eval() {
        if (scriptPattern.isBlank()) {
            throw GroovyConsoleException("Groovy script to be evaluated is not specified!")
        }
        if (instances.isEmpty()) {
            throw GroovyConsoleException("No instances defined for Groovy script evaluation!")
        }

        val scripts = instances.first().sync.groovyConsole.findScripts(scriptPattern)

        aem.progress(instances.size * scripts.size) {
            step = "Validating prerequisites"
            aem.sync(instances) {
                groovyConsole.requireAvailable()
            }

            step = "Evaluating scripts"
            aem.sync(instances) {
                scripts.forEach { script ->
                    increment("File '${script.name}' on '${instance.name}'")
                    groovyConsole.evalScript(script, data).apply {
                        logger.info("Groovy script '$script' evaluated in time $runningTime with result: $result\n${output}\n$exceptionStackTrace")
                    }
                }
            }
        }
    }
}
