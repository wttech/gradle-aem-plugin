package com.cognifide.gradle.aem.common.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import java.io.File
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal

open class PackageTask : AemDefaultTask() {

    @Input
    var instances: List<Instance> = listOf()

    @InputFiles
    var packages: List<File> = listOf()

    /**
     * Hook for preparing instance before deploying packages
     */
    @Internal
    var initializer: InstanceSync.() -> Unit = {}

    /**
     * Hook for cleaning instance after deploying packages
     */
    @Internal
    var finalizer: InstanceSync.() -> Unit = {}

    /**
     * Hook after deploying all packages to all instances.
     */
    @Internal
    var completer: () -> Unit = { awaitUp() }

    /**
     * Check instance(s) condition after performing action related with package(s).
     */
    @Internal
    var awaited: Boolean = false

    private var awaitUpOptions: AwaitUpAction.() -> Unit = {}

    /**
     * Controls await up action.
     */
    fun awaitUp(options: AwaitUpAction.() -> Unit) {
        this.awaitUpOptions = options
    }

    fun awaitUp() {
        if (awaited) {
            aem.instanceActions.awaitUp {
                instances = this@PackageTask.instances
                awaitUpOptions()
            }
        }
    }

    override fun projectsEvaluated() {
        if (instances.isEmpty()) {
            instances = aem.instances
        }

        if (packages.isEmpty()) {
            packages = aem.dependentPackages(this)
        }
    }

    fun instance(urlOrName: String) {
        instances += aem.instance(urlOrName)
    }

    fun `package`(path: String) {
        packages += project.file(path)
    }

    fun pkg(path: String) = `package`(path)

    fun sync(action: InstanceSync.(File) -> Unit) {
        common.progress(instances.size * packages.size) {
            aem.syncFiles(instances, packages) { file ->
                increment("${file.name} -> ${instance.name}") {
                    initializer()
                    action(file)
                    finalizer()
                }
            }
        }

        completer()
    }
}
