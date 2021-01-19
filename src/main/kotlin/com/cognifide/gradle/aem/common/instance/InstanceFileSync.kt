package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import java.io.File

class InstanceFileSync(private val aem: AemExtension) {

    val common = aem.common

    val instanceManager by lazy { aem.instanceManager }

    val instances = aem.obj.list<Instance> { convention(aem.obj.provider { aem.instances }) }

    val files = aem.obj.files()

    val awaited = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instanceFileSync.awaited")?.let { set(it) }
    }

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

    fun action(action: InstanceSync.(File) -> Unit) {
        this.action = action
    }

    private var action: InstanceSync.(File) -> Unit = { throw AemException("Instance file sync action is not defined!") }

    fun actionAwaited(action: InstanceSync.(File) -> Boolean) = action { file ->
        awaitIf {
            action(file)
        }
    }

    fun sync() {
        instanceManager.examine(instances.get())

        val actions = instances.get().size * files.files.size
        if (actions > 0) {
            common.progress(actions) {
                aem.syncFiles(instances.get(), files.files) { file ->
                    increment("${file.name} -> ${instance.name}") {
                        action(file)
                    }
                }
            }
            awaitUp()
        }
    }

    // Predefined actions

    fun deployPackage(vararg paths: Any) {
        files.from(paths)
        actionAwaited { packageManager.deploy(it) }
    }

    fun installBundle(vararg paths: Any) {
        files.from(paths)
        actionAwaited { osgi.installBundle(it); true }
    }
}
