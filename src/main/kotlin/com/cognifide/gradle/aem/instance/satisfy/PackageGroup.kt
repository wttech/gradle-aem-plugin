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
import com.cognifide.gradle.aem.internal.file.resolver.FileResolver
import groovy.lang.Closure
import java.io.File

class PackageGroup(resolver: FileResolver, name: String) : FileGroup(resolver, name) {

    private val project = resolver.project

    private val config = AemConfig.of(project)

    /**
     * Instances involved in packages deployment.
     */
    val instances by lazy {
        if (config.deployDistributed) {
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

    private fun performAction(action: AbstractAction, closure: Closure<*>) {
        action.configure(closure).perform()
    }

    fun await(closure: Closure<*>) {
        AwaitAction(project, instances).configure(closure).perform()
    }

    fun await() {
        AwaitAction(project, instances).perform()
    }

    fun reload(closure: Closure<*>) {
        ReloadAction(project, instances).configure(closure).perform()
    }

    fun reload() {
        ReloadAction(project, instances).perform()
    }

    override fun createResolution(id: String, resolver: (FileResolution) -> File): FileResolution {
        return PackageResolution(this, id, resolver)
    }

}
