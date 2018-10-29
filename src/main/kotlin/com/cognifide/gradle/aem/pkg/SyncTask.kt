package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.Instance
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import java.io.File

open class SyncTask : AemDefaultTask() {

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