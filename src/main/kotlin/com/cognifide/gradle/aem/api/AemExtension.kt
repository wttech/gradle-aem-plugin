package com.cognifide.gradle.aem.api

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.pkg.PackagePlugin
import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

/**
 * Main place for providing build script DSL capabilities in case of AEM.
 */
open class AemExtension(@Transient private val project: Project) {

    val config = AemConfig(project)

    val bundle = AemBundle(project)

    val notifier = AemNotifier.of(project)

    val tasks = AemTaskFactory(project)

    val instances: List<Instance>
        get() = Instance.filter(project)

    fun instances(filter: String): List<Instance> {
        return Instance.filter(project, filter)
    }

    fun instance(urlOrName: String): Instance {
        return config.parseInstance(urlOrName)
    }

    fun sync(synchronizer: (InstanceSync) -> Unit) {
        instances.parallelStream().forEach { it.sync(synchronizer) }
    }

    fun config(configurer: Closure<AemConfig>) {
        ConfigureUtil.configure(configurer, config)
    }

    fun config(configurer: AemConfig.() -> Unit) {
        config.apply(configurer)
    }

    fun bundle(configurer: Closure<AemBundle>) {
        ConfigureUtil.configure(configurer, bundle)
    }

    fun bundle(configurer: AemBundle.() -> Unit) {
        bundle.apply(configurer)
    }

    fun notifier(configurer: Closure<AemNotifier>) {
        ConfigureUtil.configure(configurer, notifier)
    }

    fun notifier(configurer: AemNotifier.() -> Unit) {
        notifier.apply(configurer)
    }

    fun tasks(configurer: Closure<AemTaskFactory>) {
        ConfigureUtil.configure(configurer, tasks)
    }

    fun tasks(configurer: AemTaskFactory.() -> Unit) {
        tasks.apply(configurer)
    }

    companion object {
        const val NAME = "aem"

        fun of(project: Project): AemExtension {
            return project.extensions.findByType(AemExtension::class.java)
                    ?: throw AemException(project.displayName.capitalize()
                            + " has neither '${PackagePlugin.ID}' nor '${InstancePlugin.ID}' plugin applied.")
        }
    }

}