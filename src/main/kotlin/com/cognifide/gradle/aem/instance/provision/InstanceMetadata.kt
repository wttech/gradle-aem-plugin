package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.common.instance.Instance
import java.util.*

class InstanceMetadata(provisioner: Provisioner, val instance: Instance) {

    private val node = instance.sync.repository.node("${provisioner.path}/metadata")

    val counter: Long
        get() = node.takeIf { it.exists }?.properties?.long(COUNTER_PROP) ?: 0L

    val performedAt: Date
        get() = node.properties.date(PERFORMED_AT_PROP)
                ?: throw ProvisionException("Instance is not yet provisioned: $instance!")

    val performed: Boolean
        get() = node.exists && node.hasProperty(PERFORMED_AT_PROP)

    fun update() {
        node.save(mapOf(
                NODE_TYPE,
                COUNTER_PROP to counter + 1,
                PERFORMED_AT_PROP to Date()
        ))
    }

    companion object {
        val NODE_TYPE = "jcr:primaryType" to "nt:unstructured"

        const val COUNTER_PROP = "counter"

        const val PERFORMED_AT_PROP = "performedAt"
    }
}
