package com.cognifide.gradle.aem.common.instance.provision

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceManager
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.Patterns
import org.apache.commons.io.FilenameUtils

/**
 * Configures AEM instances only in concrete circumstances (only once, after some time etc).
 */
class Provisioner(val manager: InstanceManager) {

    internal val aem = manager.aem

    private val common = aem.common

    private val logger = aem.logger

    /**
     * Allows to disable service at all.
     */
    val enabled = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.provision.enabled")?.let { set(it) }
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
     * Perform all provision steps for specified instance.
     */
    fun provision(instance: Instance) = provision(listOf(instance))

    /**
     * Perform all provision steps for all instances in parallel.
     */
    fun provision(instances: Collection<Instance>): List<Action> {
        if (!enabled.get()) {
            logger.lifecycle("No steps performed / instance provisioner is disabled.")
            return listOf()
        }

        steps.forEach { it.validate() }

        val actions = mutableListOf<Action>()

        val stepsFiltered = steps.filter { Patterns.wildcard(it.id, stepName.get()) }
        if (stepsFiltered.isNotEmpty()) {
            stepsFiltered.forEach { it.initCallback() }
            stepsFiltered.forEach { definition ->
                var intro = "Provision step '${definition.id}'"
                if (!definition.description.isNullOrBlank()) {
                    intro += " / ${definition.description}"
                }
                logger.info(intro)
                common.parallel.each(instances) { actions.add(InstanceStep(it, definition).run { provisionStep() }) }
            }
        }

        if (actions.none { it.status != Status.SKIPPED }) {
            logger.lifecycle("No steps to perform / all instances provisioned.")
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

    // Predefined steps

    fun enableCrxDe(options: Step.() -> Unit = {}) = step("enableCrxDe") {
        description = "Enables CRX DE"
        condition { once() && instance.env != "prod" }
        sync {
            osgi.configure("org.apache.sling.jcr.davex.impl.servlets.SlingDavExServlet", mapOf(
                    "alias" to "/crx/server"
            ))
        }
        options()
    }

    fun deployPackage(url: String, options: Step.() -> Unit = {}) = deployPackage(FilenameUtils.getBaseName(url), url, options)

    fun deployPackage(name: String, url: Any, options: Step.() -> Unit = {}) = step("deployPackage-${Formats.toHashCodeHex(url)}") {
        description = "Deploys package '$name'"
        version = name

        val file by lazy { aem.packageOptions.wrapper.wrap(common.resolveFile(url)) }

        init {
            logger.info("Deploying package '$name' - resolved file '$file'")
        }
        sync {
            logger.info("Deploying package '$name' to $instance")
            packageManager.deploy(file)
        }
        options()
    }
}
