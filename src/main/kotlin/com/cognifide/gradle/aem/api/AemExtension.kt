package com.cognifide.gradle.aem.api

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceSync
import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

/**
 * Main place for providing build script DSL capabilities in case of AEM.
 */
open class AemExtension(@Transient private val project: Project) {

    companion object {
        const val NAME = "aem"
    }

    val config = AemConfig(project)

    val bundle = AemBundle(project)

    val notifier = AemNotifier.of(project)

    val instances: List<Instance>
        get() = Instance.filter(project)

    fun sync(synchronizer: (InstanceSync) -> Unit) {
        instances.parallelStream().forEach { it.sync(synchronizer) }
    }

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