package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.aem.aem
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class MvnExec : DefaultTask() {

    private val aem = project.aem

    @Nested
    val invoker = MvnInvoker(aem, listOf("-N"))

    fun invoker(options: MvnInvoker.() -> Unit) {
        invoker.apply(options)
    }

    @TaskAction
    fun exec() {
        invoker.invoke()
    }
}
