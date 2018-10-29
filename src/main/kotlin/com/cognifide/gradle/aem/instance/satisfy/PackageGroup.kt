package com.cognifide.gradle.aem.instance.satisfy

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.instance.action.AbstractAction
import com.cognifide.gradle.aem.instance.action.AwaitAction
import com.cognifide.gradle.aem.instance.action.ReloadAction
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.file.resolver.FileGroup
import com.cognifide.gradle.aem.internal.file.resolver.FileResolution
import groovy.lang.Closure
import org.gradle.util.ConfigureUtil
import java.io.File

class PackageGroup(resolver: PackageResolver, name: String) : FileGroup(resolver, name) {

    private val project = resolver.project

    private val config = AemConfig.of(project)

    /**
     * Instances involved in packages deployment.
     */
    val instances by lazy {
        if (false) { // TODO config.deployDistributed (include in SatisfyAction)
            Instance.filter(project, config.instanceAuthorName)
        } else {
            Instance.filter(project)
        }.filter { Patterns.wildcard(it.name, instanceName) }
    }

    /**
     * Instance name filter for excluding group from deployment.
     */
    var instanceName = "*"

    /**
     * Hook for preparing instance before deploying packages
     */
    var initializer: (InstanceSync) -> Unit = {}

    /**
     * Hook for cleaning instance after deploying packages
     */
    var finalizer: (InstanceSync) -> Unit = {}

    /**
     * Hook after deploying all packages to all instances called only when
     * at least one package was deployed on any instance.
     */
    var completer: () -> Unit = { await() }

    fun await() {
        await {}
    }

    fun await(configurer: AwaitAction.() -> Unit) {
        action(AwaitAction(project), configurer)
    }

    fun reload() {
        reload {}
    }


    fun reload(configurer: ReloadAction.() -> Unit) {
        action(ReloadAction(project), configurer)
    }

    private fun <T : AbstractAction> action(action: T, configurer: T.() -> Unit) {
        action.apply {
            instances = this@PackageGroup.instances
            notify = false
        }.apply(configurer).perform()
    }

    override fun createResolution(id: String, resolver: (FileResolution) -> File): FileResolution {
        return PackageResolution(this, id, resolver)
    }

}
