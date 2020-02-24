package com.cognifide.gradle.aem.common.instance

interface InstanceAction {

    fun perform(instances: Collection<Instance>)
}
