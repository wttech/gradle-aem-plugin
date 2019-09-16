
package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.utils.Formats
import com.cognifide.gradle.aem.common.utils.Patterns

/**
 * Configures AEM instances only in concrete circumstances (only once, after some time etc).
 */
class Provisioner(val aem: AemExtension) {

    /**
     * Instances to perform provisioning.
     */
    var instances = aem.instances

    /**
     * Forces to perform all steps regardless their state on instances (already performed).
     */
    var greedy = aem.props.flag("instance.provision.greedy")

    /**
     * Determines which steps should be performed selectively.
     */
    var stepName = aem.props.string("instance.provision.step") ?: "*"

    /**
     * Determines a path in JCR repository in which provisioning metadata and step markers will be stored.
     */
    var path: String = aem.props.string("instance.provision.path") ?: "/var/gap/provision"

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

        val stepsFiltered = steps.filter { Patterns.wildcard(it.id, stepName) }
        if (stepsFiltered.isNotEmpty()) {
            stepsFiltered.forEach { definition ->
                var intro = "Provision step '${definition.id}'"
                if (!definition.description.isNullOrBlank()) {
                    intro += " / ${definition.description}"
                }
                aem.logger.info(intro)
                aem.parallel.each(instances) { actions.add(InstanceStep(it, definition).run { provisionStep() }) }
            }
        }

        return actions
    }

    private fun InstanceStep.provisionStep(): Action {
        if (!greedy && !isPerformable()) {
            update()
            aem.logger.info("Provision step '${definition.id}' skipped for $instance")
            return Action(this, Status.SKIPPED)
        }

        val startTime = System.currentTimeMillis()
        aem.logger.info("Provision step '${definition.id}' started at $instance")

        return try {
            perform()
            aem.logger.info("Provision step '${definition.id}' ended at $instance. Duration: ${Formats.durationSince(startTime)}")
            Action(this, Status.ENDED)
        } catch (e: ProvisionException) {
            if (!definition.continueOnFail) {
                throw e
            } else {
                aem.logger.error("Provision step '${definition.id} failed at $instance. Duration: ${Formats.durationSince(startTime)}. Cause: ${e.message}")
                aem.logger.debug("Actual error", e)
                Action(this, Status.FAILED)
            }
        }
    }
}
