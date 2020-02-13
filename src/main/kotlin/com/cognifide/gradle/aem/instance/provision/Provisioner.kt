
package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.Patterns

/**
 * Configures AEM instances only in concrete circumstances (only once, after some time etc).
 */
class Provisioner(val aem: AemExtension) {

    private val common = aem.common

    private val logger = aem.logger

    /**
     * Instances to perform provisioning.
     */
    val instances = aem.obj.list<Instance> {
        convention(aem.obj.provider { aem.instances })
    }

    /**
     * Forces to perform steps that supports greediness regardless their state on instances (already performed).
     */
    val greedy = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("instance.provision.greedy")?.let { set(it) }
    }

    /**
     * Determines which steps should be performed selectively.
     */
    val stepName = aem.obj.string {
        convention("*")
        aem.prop.string("instance.provision.step")?.let { set(it) }
    }

    /**
     * Determines a path in JCR repository in which provisioning metadata and step markers will be stored.
     */
    val path = aem.obj.string {
        convention("/var/gap/provision")
        aem.prop.string("instance.provision.path")?.let { set(it) }
    }

    private val steps = mutableListOf<Step>()

    /**
     * Define provision step.
     */
    fun step(id: String, options: Step.() -> Unit) {
        steps.add(Step(this, id).apply(options))
    }

    /**
     * Perform all provision steps.
     */
    fun provision(): List<Action> {
        steps.forEach { it.validate() }

        val actions = mutableListOf<Action>()

        val stepsFiltered = steps.filter { Patterns.wildcard(it.id, stepName.get()) }
        if (stepsFiltered.isNotEmpty()) {
            stepsFiltered.forEach { definition ->
                var intro = "Provision step '${definition.id}'"
                if (!definition.description.isNullOrBlank()) {
                    intro += " / ${definition.description}"
                }
                logger.info(intro)
                common.parallel.each(instances.get()) { actions.add(InstanceStep(it, definition).run { provisionStep() }) }
            }
        }

        return actions
    }

    private fun InstanceStep.provisionStep(): Action {
        if (!isPerformable()) {
            update()
            logger.info("Provision step '${definition.id}' skipped for $instance")
            return Action(this, Status.SKIPPED)
        }

        val startTime = System.currentTimeMillis()
        logger.info("Provision step '${definition.id}' started at $instance")

        return try {
            perform()
            logger.info("Provision step '${definition.id}' ended at $instance." +
                    " Duration: ${Formats.durationSince(startTime)}")
            Action(this, Status.ENDED)
        } catch (e: ProvisionException) {
            if (!definition.continueOnFail.get()) {
                throw e
            } else {
                logger.error("Provision step '${definition.id} failed at $instance." +
                        " Duration: ${Formats.durationSince(startTime)}. Cause: ${e.message}")
                logger.debug("Actual error", e)
                Action(this, Status.FAILED)
            }
        }
    }
}
