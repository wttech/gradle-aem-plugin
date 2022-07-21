package com.cognifide.gradle.aem.common.instance.provision

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.common.utils.Formats
import java.util.*

/**
 * Represents provision step to be performed on concrete AEM instance.
 */
class InstanceStep(val instance: Instance, val definition: Step) {

    private val logger = definition.provisioner.aem.logger

    private val provisioner = definition.provisioner

    private val marker get() = instance.sync.repository.node("${provisioner.path.get()}/step/${definition.id.get().replace(".", "-")}")

    val greedy: Boolean get() = provisioner.greedy.get() || provisioner.aem.prop.flag("instance.provision.${definition.id.get()}.greedy")

    val startedAt: Date get() = marker.properties.date(STARTED_AT_PROP)
        ?: throw ProvisionException("Provision step '${definition.id.get()}' not yet started on $instance!")

    val started: Boolean get() = marker.exists && marker.hasProperty(STARTED_AT_PROP)

    val ended: Boolean get() = marker.exists && marker.hasProperty(ENDED_AT_PROP)

    val version: String get() = marker.takeIf { it.exists }?.properties?.string(VERSION_PROP) ?: VERSION_DEFAULT

    val changed: Boolean get() = version != definition.version.get()

    val endedAt: Date get() = marker.properties.date(ENDED_AT_PROP)
        ?: throw ProvisionException("Provision step '${definition.id.get()}' not yet ended on $instance!")

    val failed: Boolean get() = marker.exists && marker.properties.boolean(FAILED_PROP) ?: false

    val duration: Long get() = endedAt.time - startedAt.time

    val durationString: String get() = Formats.duration(duration)

    val counter: Long get() {
        if (!provisioner.countable.get()) {
            throw ProvisionException(
                "Provision step counting is disabled!\n" +
                    "Consider enabling it by setting property 'instance.provision.countable=true'"
            )
        }

        return marker.takeIf { it.exists }?.properties?.long(COUNTER_PROP) ?: 0L
    }

    val performable by lazy { definition.isPerformable(Condition(this)) }

    fun perform(): Action {
        if (!performable) {
            update()
            logger.info("Provision step '${definition.id.get()}' skipped for $instance")
            return Action(this, Status.SKIPPED)
        }

        val startTime = System.currentTimeMillis()
        logger.info("Provision step '${definition.id.get()}' started at $instance")

        return try {
            action()
            logger.info(
                "Provision step '${definition.id.get()}' ended at $instance." +
                    " Duration: ${Formats.durationSince(startTime)}"
            )
            Action(this, Status.ENDED)
        } catch (e: ProvisionException) {
            if (!definition.continueOnFail.get()) {
                throw e
            } else {
                logger.error(
                    "Provision step '${definition.id.get()} failed at $instance." +
                        " Duration: ${Formats.durationSince(startTime)}. Cause: ${e.message}"
                )
                logger.debug("Actual error", e)
                Action(this, Status.FAILED)
            }
        }
    }

    /**
     * Update provision step metadata on AEM instance.
     *
     * Condition 'every()' is basing on counter and allows to perform step every(n) times,
     * so that counting is needed even for step that is actually not performed.
     */
    private fun update() {
        if (provisioner.countable.get()) {
            marker.save(
                mapOf(
                    Node.TYPE_UNSTRUCTURED,
                    COUNTER_PROP to counter + 1
                )
            )
        }
    }

    /**
     * Perform provision step on AEM instance.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun action() {
        if (provisioner.countable.get()) {
            marker.save(
                mapOf(
                    Node.TYPE_UNSTRUCTURED,
                    STARTED_AT_PROP to Date(),
                    COUNTER_PROP to counter + 1
                )
            )
        } else {
            marker.save(
                mapOf(
                    Node.TYPE_UNSTRUCTURED,
                    STARTED_AT_PROP to Date()
                )
            )
        }

        try {
            with(definition) {
                val operation = "perform provision step '${id.get()}' for '${instance.name}'"
                actionRetry.withCountdown<Unit, Exception>(operation) { definition.action(instance) }
            }
            marker.save(
                mapOf(
                    VERSION_PROP to definition.version.get(),
                    ENDED_AT_PROP to Date(),
                    FAILED_PROP to false
                )
            )
        } catch (e: Exception) {
            marker.save(
                mapOf(
                    VERSION_PROP to definition.version.get(),
                    ENDED_AT_PROP to Date(),
                    FAILED_PROP to true
                )
            )
            throw ProvisionException("Cannot perform provision step '${definition.id.get()}' on $instance! Cause: ${e.message}", e)
        } finally {
            marker.reload()
        }
    }

    override fun toString() = "InstanceStep(instance=$instance, definition=$definition, marker=$marker)"

    companion object {
        const val STARTED_AT_PROP = "startedAt"

        const val ENDED_AT_PROP = "endedAt"

        const val FAILED_PROP = "failed"

        const val COUNTER_PROP = "counter"

        const val VERSION_PROP = "version"

        const val VERSION_DEFAULT = "default"
    }
}
