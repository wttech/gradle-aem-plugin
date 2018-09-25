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

    val tasks = AemTaskFactory(project)

    fun config(configurer: AemConfig.() -> Unit) {
        config.apply(configurer)
    }

    fun config(closure: Closure<*>) {
        config { ConfigureUtil.configure(closure, this) }
    }

    fun bundle(configurer: AemBundle.() -> Unit) {
        bundle.apply(configurer)
    }

    fun bundle(closure: Closure<*>) {
        bundle { ConfigureUtil.configure(closure, this) }
    }

    fun notifier(configurer: AemNotifier.() -> Unit) {
        notifier.apply(configurer)
    }

    fun notifier(closure: Closure<*>) {
        notifier { ConfigureUtil.configure(closure, this) }
    }

    fun tasks(configurer: AemTaskFactory.() -> Unit) {
        tasks.apply(configurer)
    }

    fun tasks(closure: Closure<*>) {
        tasks { ConfigureUtil.configure(closure, this) }
    }

}