package com.cognifide.gradle.aem.common.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import java.io.File

open class SyncFileTask : AemDefaultTask() {

    @get:Internal
    val instanceManager get() = aem.instanceManager

    @Internal
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
     * Check instance(s) condition after performing action related with synced file(s).
     */
    @Internal
    val awaited = aem.obj.boolean { convention(true) }

    private var awaitUpOptions: AwaitUpAction.() -> Unit = {}

    private var awaitOptionally = false

    private var awaitRequired = false

    fun awaitIf(callback: () -> Boolean) {
        awaitOptionally = true
        if (callback()) {
            awaitRequired = true
        }
    }

    /**
     * Controls await up action.
     */
    fun awaitUp(options: AwaitUpAction.() -> Unit) {
        this.awaitUpOptions = options
    }

    fun awaitUp() {
        if (awaited.get() && (!awaitOptionally || awaitRequired)) {
            aem.instanceManager.awaitUp(instances.get(), awaitUpOptions)
        }
    }

    fun sync(action: InstanceSync.(File) -> Unit) {
        instanceManager.examine(instances.get())

        val actions = instances.get().size * files.files.size
        if (actions > 0) {
            common.progress(actions) {
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

    fun syncFile(action: InstanceSync.(File) -> Unit) = doLast { sync(action) }
}
