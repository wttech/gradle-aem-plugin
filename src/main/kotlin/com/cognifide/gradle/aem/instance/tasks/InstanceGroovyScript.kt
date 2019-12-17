package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.service.groovy.GroovyScriptEvaluator
import org.gradle.api.tasks.TaskAction

open class InstanceGroovyScript : AemDefaultTask() {

    init {
        description = "Evaluate Groovy script(s) on instance(s)."
    }

    private var options: GroovyScriptEvaluator.() -> Unit = {}

    fun options(options: GroovyScriptEvaluator.() -> Unit) {
        this.options = options
    }

    @TaskAction
    fun eval() {
        val summary = aem.groovyScript { options(); eval() }

        aem.notifier.notify(
                "Evaluated Groovy script(s)",
                "Succeeded ${summary.succeededPercent}"
        )
    }

    companion object {
        const val NAME = "instanceGroovyScript"
    }
}
