package com.cognifide.gradle.aem.api

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

/**
 * Extension holding collection of AEM configuration properties.
 * Provides also nice DSL for configuring OSGi bundles and configuring custom interactive
 * build notifications.
 */
open class AemExtension(@Transient private val project: Project) {

    companion object {
        val NAME = "aem"
    }

    val config = AemConfig(project)

    val bundle = AemBundle(project)

    val notifier = AemNotifier.of(project)

    fun config(closure: Closure<*>) {
        ConfigureUtil.configure(closure, config)
    }

    fun bundle(closure: Closure<*>) {
        ConfigureUtil.configure(closure, bundle)
    }

    fun notifier(closure: Closure<*>) {
        ConfigureUtil.configure(closure, notifier)
    }

}