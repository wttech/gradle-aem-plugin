package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.utils.Formats
import java.util.*

class InstanceStep(val instance: Instance, val definition: Step) {

    private val provisioner = definition.provisioner

    private val aem = provisioner.aem

    private val repository = instance.sync.repository

    private val marker = repository.node("${provisioner.stepPath}/${definition.id}")

    val startedAt: Date
        get() = marker.properties.date(STARTED_AT_PROP)
                ?: throw InstanceException("Step '${definition.id}' not yet started on $instance!")

    val started: Boolean
        get() = marker.exists && marker.hasProperty(STARTED_AT_PROP)

    val ended: Boolean
        get() = marker.exists && marker.hasProperty(ENDED_AT_PROP)

    val endedAt: Date
        get() = marker.properties.date(ENDED_AT_PROP)
                ?: throw InstanceException("Step '${definition.id}' not yet ended on $instance!")

    val done: Boolean
        get() = ended

    val failed: Boolean
        get() = marker.exists && marker.hasProperty(FAILED_AT_PROP)

    val failedAt: Date
        get() = marker.properties.date(FAILED_AT_PROP)
                ?: throw InstanceException("Step '${definition.id}' not failed on $instance!")

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
            aem.logger.error("Step '${definition.id}' failed ")
            marker.saveProperty(FAILED_AT_PROP, Date())
        }
    }

    companion object {
        const val STARTED_AT_PROP = "startedAt"

        const val ENDED_AT_PROP = "endedAt"

        const val FAILED_AT_PROP = "failedAt"

        val MARKER_TYPE = "jcr:primaryType" to "nt:unstructured"
    }
}
