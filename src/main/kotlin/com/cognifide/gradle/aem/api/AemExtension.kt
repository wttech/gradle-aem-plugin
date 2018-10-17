package com.cognifide.gradle.aem.api

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.gradle.api.Action
import org.gradle.api.Project

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

    fun config(configurer: Action<AemConfig>) {
        configurer.execute(config)
    }

    fun bundle(configurer: Action<AemBundle>) {
        configurer.execute(bundle)
    }

    fun notifier(configurer: Action<AemNotifier>) {
        configurer.execute(notifier)
    }

    fun tasks(configurer: Action<AemTaskFactory>) {
        configurer.execute(tasks)
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