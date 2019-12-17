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
    fun eval() {
        val summary = aem.groovyEval {
            scriptPattern = aem.prop.string("instance.groovyEval.script") ?: ""
            data = aem.prop.map("instance.groovyEval.data") ?: mapOf()
            faulty = aem.prop.boolean("instance.groovyEval.faulty") ?: true
            options()
            eval()
        }

        aem.notifier.notify(
                "Evaluated Groovy script(s)",
                "Succeeded: ${summary.succeededPercent}. Elapsed time: ${summary.durationString}"
        )
    }

    companion object {
        const val NAME = "instanceGroovyEval"
    }
}
