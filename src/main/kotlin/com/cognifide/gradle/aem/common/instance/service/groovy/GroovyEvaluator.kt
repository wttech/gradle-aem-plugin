package com.cognifide.gradle.aem.common.instance.service.groovy

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.system.measureTimeMillis

class GroovyEvaluator(private val aem: AemExtension) {

    private val common = aem.common

    private val logger = aem.logger

    val scriptDirDefault = aem.obj.string { convention(aem.obj.provider { aem.project.version.toString().removeSuffix("-SNAPSHOT") }) }

    val scriptPattern = aem.obj.string { convention(scriptDirDefault.map { "$it/**/*" }) }

    val scriptSuffix = aem.obj.string { convention(".groovy") }

    val instances = aem.obj.list<Instance> { convention(aem.obj.provider { aem.instances }) }

    val data = aem.obj.map<String, Any?> { convention(mapOf()) }

    val faulty = aem.obj.boolean { convention(true) }

    private var consoleOptions: GroovyConsole.() -> Unit = {}

    fun console(options: GroovyConsole.() -> Unit) {
        this.consoleOptions = options
    }

    fun eval() = evalScripts(findScripts())

    fun findScripts(): List<File> {
        if (scriptPattern.get().isBlank()) {
            throw GroovyConsoleException("Groovy script to be evaluated is not specified!")
        }
        if (instances.get().isEmpty()) {
            throw GroovyConsoleException("No instances defined for Groovy script evaluation!")
        }

        val groovyConsole = instances.get().first().sync.groovyConsole
        val pathPattern = StringUtils.appendIfMissing(scriptPattern.get(), scriptSuffix.get())

        return groovyConsole.findScripts(pathPattern)
    }

    fun evalScripts(scripts: List<File>): GroovyEvalSummary {
        val summary = evalScriptsInternal(scripts)
        if (summary.failed > 0) {
            val failedStatuses = summary.statuses.filter { it.fail }
            val failMessages = failedStatuses.mapIndexed { no, status ->
                "${no + 1}) '${status.script}' on ${status.instance}:\n${status.error}"
            }
            val failMessage = "Groovy script evaluation errors (${failedStatuses.size}):\n${failMessages.joinToString("\n")}"

            if (faulty.get()) {
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

        val statuses = CopyOnWriteArrayList<GroovyEvalStatus>()

        val elapsed = measureTimeMillis {
            common.progress(instances.get().size * scripts.size) {
                step = "Validating"
                aem.sync(instances.get()) {
                    groovyConsole.requireAvailable()
                }

                step = "Evaluating"
                aem.sync(instances.get()) {
                    groovyConsole.apply(consoleOptions)

                    scripts.forEach { script ->
                        increment("Script '${script.name}' on '${instance.name}'")

                        groovyConsole.evalScript(script, data.get()).apply {
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
        }

        return GroovyEvalSummary(statuses, elapsed)
    }
}
