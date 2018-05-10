package com.cognifide.gradle.aem.api

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

/**
 * Gradle extensions cannot be serialized so that config need to be wrapped.
 */
open class AemExtension(@Transient private val project: Project) {

    companion object {
        val NAME = "aem"
    }

    val config = AemConfig(project)

    val bundle = AemBundle(project)

    fun config(closure: Closure<*>) {
        ConfigureUtil.configure(closure, config)
    }

    fun bundle(closure: Closure<*>) {
        ConfigureUtil.configure(closure, bundle)
    }

}