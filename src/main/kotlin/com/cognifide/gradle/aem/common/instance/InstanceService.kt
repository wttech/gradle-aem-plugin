package com.cognifide.gradle.aem.common.instance

open class InstanceService(val sync: InstanceSync) {

    // Shorthands

    val aem = sync.aem

    val instance = sync.instance

    val project = sync.aem.project

    val common = aem.common

    val logger = aem.logger
}
