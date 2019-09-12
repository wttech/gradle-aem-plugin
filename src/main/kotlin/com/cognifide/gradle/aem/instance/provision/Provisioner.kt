
package com.cognifide.gradle.aem.instance.provision

import com.cognifide.gradle.aem.AemExtension
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
            val metadatas = instances.map { it to InstanceMetadata(this, it) }.toMap()

            stepsFiltered.forEach { definition ->
                var intro = "Provision step '${definition.id}'"
                if (!definition.description.isNullOrBlank()) {
                    intro += " / ${definition.description}"
                }
                aem.logger.info(intro)

                aem.parallel.each(instances) { instance ->
                    actions.add(InstanceStep(metadatas[instance]!!, definition).run { provisionStep() })
                }
            }

            aem.parallel.each(metadatas.values) { it.update() }
        }

        return actions
    }

    private fun InstanceStep.provisionStep(): Action {
        if (!greedy && !isPerformable()) {
            aem.logger.info("Provision step '${definition.id}' skipped for $instance")
            return Action(this, Status.SKIPPED)
        }

        aem.logger.info("Provision step '${definition.id}' started at $instance")

        return try {
            perform()
            aem.logger.info("Provision step '${definition.id}' ended at $instance. Duration: $durationString")
            Action(this, Status.ENDED)
        } catch (e: ProvisionException) {
            if (definition.failOnError) {
                throw e
            } else {
                aem.logger.error("Provision step '${definition.id} failed at $instance. Duration: $durationString. Cause: ${e.message}", e)
                Action(this, Status.FAILED)
            }
        }
    }
}
