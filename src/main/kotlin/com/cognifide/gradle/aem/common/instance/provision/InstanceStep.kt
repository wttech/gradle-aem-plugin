package com.cognifide.gradle.aem.common.instance.provision

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.common.utils.Formats
import java.util.*

/**
 * Represents provision step to be performed on concrete AEM instance.
 */
class InstanceStep(val instance: Instance, val definition: Step) {

    private val provisioner = definition.provisioner

    private val marker = instance.sync.repository.node("${provisioner.path}/step/${definition.id}")

    val greedy: Boolean get() = provisioner.greedy.get() || provisioner.aem.prop.flag("instance.provision.${definition.id}.greedy")

    val startedAt: Date
        get() = marker.properties.date(STARTED_AT_PROP)
                ?: throw ProvisionException("Provision step '${definition.id}' not yet started on $instance!")

    val started: Boolean get() = marker.exists && marker.hasProperty(STARTED_AT_PROP)

    val ended: Boolean get() = marker.exists && marker.hasProperty(ENDED_AT_PROP)

    val endedAt: Date
        get() = marker.properties.date(ENDED_AT_PROP)
                ?: throw ProvisionException("Provision step '${definition.id}' not yet ended on $instance!")

    val failed: Boolean
        get() = marker.exists && marker.properties.boolean(FAILED_PROP) ?: false

    val duration: Long get() = endedAt.time - startedAt.time

    val durationString: String get() = Formats.duration(duration)

    val counter: Long get() = marker.takeIf { it.exists }?.properties?.long(COUNTER_PROP) ?: 0L

    fun isPerformable(): Boolean = definition.conditionCallback(Condition(this))

    /**
     * Update provision step metadata on AEM instance.
     *
     * Condition 'every()' is basing on counter and allows to perform step every(n) times,
     * so that counting is needed even for step that is actually not performed.
     */
    fun update() {
        marker.save(mapOf(
                Node.TYPE_UNSTRUCTURED,
                COUNTER_PROP to counter + 1
        ))
    }

    /**
     * Perform provision step on AEM instance.
     */
    @Suppress("TooGenericExceptionCaught")
    fun perform() {
        marker.save(mapOf(
                Node.TYPE_UNSTRUCTURED,
                STARTED_AT_PROP to Date(),
                COUNTER_PROP to counter + 1
        ))

        try {
            with(definition) {
                retry.withCountdown<Unit, Exception>("perform provision step '$id' for '${instance.name}'") {
                    actionCallback(instance)
                }
            }
            marker.save(mapOf(
                    ENDED_AT_PROP to Date(),
                    FAILED_PROP to false
            ))
        } catch (e: Exception) {
            marker.save(mapOf(
                    ENDED_AT_PROP to Date(),
                    FAILED_PROP to true
            ))
            throw ProvisionException("Cannot perform provision step '${definition.id}' on $instance! Cause: ${e.message}")
        } finally {
            marker.reload()
        }
    }

    override fun toString(): String {
        return "InstanceStep(instance=$instance, definition=$definition, marker=$marker)"
    }

    companion object {
        const val STARTED_AT_PROP = "startedAt"

        const val ENDED_AT_PROP = "endedAt"

        const val FAILED_PROP = "failed"

        const val COUNTER_PROP = "counter"
    }
}
