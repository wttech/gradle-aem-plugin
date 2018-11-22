package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.Instance
import java.io.File
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles

open class Sync : AemDefaultTask() {

    @Input
    var instances: List<Instance> = mutableListOf()

    @InputFiles
    var packages: List<File> = mutableListOf()

    override fun projectsEvaluated() {
        if (instances.isEmpty()) {
            instances = aem.instances
        }

        if (packages.isEmpty()) {
            packages = aem.packages(this)
        }
    }
}