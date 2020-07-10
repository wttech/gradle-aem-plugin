package com.cognifide.gradle.sling.common.instance

open class InstanceService(val sync: InstanceSync) {

    // Shorthands

    val sling = sync.sling

    val instance = sync.instance

    val project = sync.sling.project

    val common = sling.common

    val logger = sling.logger
}
