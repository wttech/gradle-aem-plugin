package com.cognifide.gradle.aem.common.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import java.io.File

open class SyncFileTask : AemDefaultTask() {

    @Input
    val instances = aem.obj.list<Instance> { convention(aem.obj.provider { aem.instances }) }

    @InputFiles
    val files = aem.obj.files()

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
    val awaited = aem.obj.boolean { convention(true) }

    private var awaitUpOptions: AwaitUpAction.() -> Unit = {}

    /**
     * Controls await up action.
     */
    fun awaitUp(options: AwaitUpAction.() -> Unit) {
        this.awaitUpOptions = options
    }

    fun awaitUp() {
        if (awaited.get()) {
            aem.instanceActions.awaitUp {
                instances.convention(this@SyncFileTask.instances)
                awaitUpOptions()
            }
        }
    }

    fun sync(action: InstanceSync.(File) -> Unit) {
        common.progress(instances.get().size * files.files.size) {
            aem.syncFiles(instances.get(), files.files) { file ->
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
