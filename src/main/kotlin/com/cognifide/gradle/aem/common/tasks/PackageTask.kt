package com.cognifide.gradle.aem.common.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.names
import java.io.File
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles

open class PackageTask : AemDefaultTask() {

    @Input
    var instances: List<Instance> = listOf()

    @InputFiles
    var packages: List<File> = listOf()

    override fun projectsEvaluated() {
        if (instances.isEmpty()) {
            instances = aem.instances
        }

        if (packages.isEmpty()) {
            packages = aem.dependentPackages(this)
        }
    }

    fun checkInstances() {
        val unavailableInstances = instances.filter { !it.available }
        if (unavailableInstances.isNotEmpty()) {
            throw InstanceException("Instances are unavailable: ${unavailableInstances.names}.\n" +
                    "Ensure having correct instance URLs defined, credentials correctly encoded " +
                    "and networking in correct state (internet accessible, VPN on/off).")
        }
    }

    fun instance(urlOrName: String) {
        instances += aem.instance(urlOrName)
    }

    fun pkg(path: String) {
        packages += project.file(path)
    }
}