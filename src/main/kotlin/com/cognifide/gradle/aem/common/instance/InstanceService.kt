package com.cognifide.gradle.aem.common.instance

open class InstanceService(val sync: InstanceSync) {

    val aem = sync.aem

    val instance = sync.instance

    val project = sync.aem.project
}
