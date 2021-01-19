package com.cognifide.gradle.aem.common.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.FileSync as Base
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.common.utils.using
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

open class InstanceFileSync : AemDefaultTask() {

    @get:Internal
    val sync by lazy { Base(aem.instanceManager) }

    fun sync(options: Base.() -> Unit) = sync.using(options)

    @TaskAction
    protected fun doSync() {
        sync.sync()
    }

    @get:Internal
    val files: Set<File> get() = sync.files.files

    fun files(vararg paths: Any) {
        sync.files.from(paths)
    }

    @get:Internal
    val instances: List<Instance> get() = sync.instances.get()

    fun instances(vararg instances: Instance) {
        sync.instances.set(instances.asIterable())
    }
}
