package com.cognifide.gradle.aem.common.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.Instance
import java.io.File
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles

open class BundleTask : AemDefaultTask() {

    @Input
    var instances: List<Instance> = listOf()

    @InputFiles
    var bundles: List<File> = listOf()

    override fun projectsEvaluated() {
        if (instances.isEmpty()) {
            instances = aem.instances
        }

        if (bundles.isEmpty()) {
            bundles = aem.dependentBundles(this)
        }
    }

    fun instance(urlOrName: String) {
        instances += aem.instance(urlOrName)
    }

    fun bundle(path: String) {
        bundles += project.file(path)
    }
}
