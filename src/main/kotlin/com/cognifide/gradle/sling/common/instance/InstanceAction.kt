package com.cognifide.gradle.sling.common.instance

interface InstanceAction {

    fun perform(instances: Collection<Instance>)
}
