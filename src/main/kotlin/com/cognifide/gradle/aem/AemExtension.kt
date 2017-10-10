package com.cognifide.gradle.aem

import groovy.lang.Closure
import groovy.lang.GroovyObjectSupport
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

/**
 * Gradle extensions cannot be serialized so that config need to be wrapped.
 */
open class AemExtension(project: Project) : GroovyObjectSupport() {

    companion object {
        val NAME = "aem"
    }

    val config = AemConfig(project)

    fun config(closure: Closure<*>) {
        ConfigureUtil.configure(closure, config)
    }

}