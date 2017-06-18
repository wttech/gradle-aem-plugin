package com.cognifide.gradle.aem

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

open class AemExtension(val project: Project) {

    companion object {
        val NAME = "aem"
    }

    val config = AemConfig(project)

    fun config(closure: Closure<*>) {
        ConfigureUtil.configure(closure, config)
    }

}