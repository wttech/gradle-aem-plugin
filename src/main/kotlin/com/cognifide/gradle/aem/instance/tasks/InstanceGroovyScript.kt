package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.service.groovy.GroovyConsoleEvaluator
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class InstanceGroovyScript : AemDefaultTask() {

    init {
        description = "Evaluate Groovy script(s) on instance(s)."
    }

    @Internal
    var evaluator = GroovyConsoleEvaluator(aem)

    fun options(options: GroovyConsoleEvaluator.() -> Unit) {
        evaluator.apply(options)
    }

    @TaskAction
    fun eval() {
        evaluator.eval()
    }

    companion object {
        const val NAME = "instanceGroovyScript"
    }
}
