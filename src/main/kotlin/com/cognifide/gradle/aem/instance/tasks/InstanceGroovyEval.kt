package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.service.groovy.GroovyEvaluator
import org.gradle.api.tasks.TaskAction

open class InstanceGroovyEval : AemDefaultTask() {

    init {
        description = "Evaluate Groovy script(s) on instance(s)."
    }

    private var options: GroovyEvaluator.() -> Unit = {}

    fun options(options: GroovyEvaluator.() -> Unit) {
        this.options = options
    }

    @TaskAction
    fun eval() = aem.groovyEval {
        aem.prop.string("instance.groovyEval.script")?.let { scriptPattern = it }
        aem.prop.string("instance.groovyEval.scriptSuffix")?.let { scriptSuffix = it }
        aem.prop.map("instance.groovyEval.data")?.let { data = it }
        aem.prop.boolean("instance.groovyEval.faulty")?.let { faulty = it }

        options()

        val scripts = findScripts()
        if (scripts.isEmpty()) {
            logger.lifecycle("Lack of Groovy script(s) to evaluate matching pattern '$scriptPattern'")
        } else {
            val summary = evalScripts(scripts)
            aem.notifier.lifecycle(
                    "Evaluated Groovy script(s)",
                    "Succeeded: ${summary.succeededPercent}. Elapsed time: ${summary.durationString}"
            )
        }
    }

    companion object {
        const val NAME = "instanceGroovyEval"
    }
}
