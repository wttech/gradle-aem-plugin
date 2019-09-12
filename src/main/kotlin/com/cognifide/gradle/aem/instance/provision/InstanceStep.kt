package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.common.utils.Formats
import java.util.*

class InstanceStep(val metadata: InstanceMetadata, val definition: Step) {

    val instance = metadata.instance

    private val provisioner = definition.provisioner

    private val marker = instance.sync.repository.node("${provisioner.path}/step/${definition.id}")

    private val logger = provisioner.aem.logger

    val startedAt: Date
        get() = marker.properties.date(STARTED_AT_PROP)
                ?: throw ProvisionException("Provision step '${definition.id}' not yet started on $instance!")

    val started: Boolean
        get() = marker.exists && marker.hasProperty(STARTED_AT_PROP)

    val ended: Boolean
        get() = marker.exists && marker.hasProperty(ENDED_AT_PROP)

    val endedAt: Date
        get() = marker.properties.date(ENDED_AT_PROP)
                ?: throw ProvisionException("Provision step '${definition.id}' not yet ended on $instance!")

    val done: Boolean
        get() = ended

    val failed: Boolean
        get() = marker.exists && marker.hasProperty(FAILED_AT_PROP)

    val failedAt: Date
        get() = marker.properties.date(FAILED_AT_PROP)
                ?: throw ProvisionException("Provision step '${definition.id}' not failed on $instance!")

    val duration: Long
        get() = endedAt.time - startedAt.time

    val durationString: String
        get() = Formats.duration(duration)

    fun isPerformable(): Boolean {
        return definition.conditionCallback(Condition(this))
    }

    @Suppress("TooGenericExceptionCaught")
    fun perform() {
        marker.save(mapOf(
                MARKER_TYPE,
                STARTED_AT_PROP to Date()
        ))

        try {
            definition.actionCallback(instance)
            marker.saveProperty(ENDED_AT_PROP, Date())
        } catch (e: Exception) {
            marker.saveProperty(FAILED_AT_PROP, Date())
            throw ProvisionException("Cannot perform provision step '${definition.id}' on $instance! Cause: ${e.message}")
        }
    }

    companion object {
        const val STARTED_AT_PROP = "startedAt"

        const val ENDED_AT_PROP = "endedAt"

        const val FAILED_AT_PROP = "failedAt"

        val MARKER_TYPE = "jcr:primaryType" to "nt:unstructured"
    }
}
