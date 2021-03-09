package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.aem.aem
import com.cognifide.gradle.common.mvn.MvnInvoker
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class MvnExec : DefaultTask() {

    private val aem = project.aem

    @Nested
    val invoker = MvnInvoker(aem.common)

    fun invoker(options: MvnInvoker.() -> Unit) {
        invoker.apply(options)
    }

    @TaskAction
    fun exec() {
        invoker.invoke()
    }
}
