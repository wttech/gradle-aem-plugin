package com.cognifide.gradle.aem.internal

import com.cognifide.gradle.aem.instance.AemInstance
import org.gradle.api.Project

class BundleSynchronizer(val project: Project, val instance: AemInstance) {

    companion object {
        val URL_JSON = "system/console/bundles.json"
    }

    val all: Set<BundleDescriptor>
        get() {
            return setOf()
        }

}