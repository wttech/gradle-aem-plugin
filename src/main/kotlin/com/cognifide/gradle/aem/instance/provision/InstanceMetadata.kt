package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.common.instance.Instance

class InstanceMetadata(provisioner: Provisioner, val instance: Instance) {

    private val node = instance.sync.repository.node("${provisioner.path}/metadata")

    val counter: Long
        get() = node.takeIf { it.exists }?.properties?.long(COUNTER_PROP) ?: 0L

    fun incrementCounter() {
        node.save(mapOf(
                NODE_TYPE,
                COUNTER_PROP to counter + 1
        ))
    }

    companion object {
        const val COUNTER_PROP = "counter"

        val NODE_TYPE = "jcr:primaryType" to "nt:unstructured"
    }
}
