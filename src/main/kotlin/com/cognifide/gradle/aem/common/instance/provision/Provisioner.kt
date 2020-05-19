package com.cognifide.gradle.aem.common.instance.provision

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceManager
import com.cognifide.gradle.common.file.resolver.FileResolver
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
     * Enables using step conditions based on counter e.g [Condition.repeatEvery].
     */
    val countable = aem.obj.boolean {
        convention(false)
        aem.prop.boolean("instance.provision.countable")?.let { set(it) }
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

        val actions = provisionActions(instances)
        if (actions.none { it.status != Status.SKIPPED }) {
            logger.lifecycle("No steps to perform / all instances provisioned.")
        }

        return actions
    }

    private fun provisionActions(instances: Collection<Instance>): List<Action> {
        val stepsFiltered = stepsFiltered()
        if (stepsFiltered.isEmpty()) {
            return listOf()
        }

        val actions = mutableListOf<Action>()
        val steps = stepsFiltered.map { step -> step to instances.map { InstanceStep(it, step) } }.toMap()

        common.progress {
            total = steps.size.toLong()
            step = "Initializing"
            steps.forEach { (definition, instanceSteps) ->
                increment("Step '${definition.label}'") {
                    if (instanceSteps.any { it.performable }) {
                        definition.init()
                    }
                }
            }

            count = 0
            step = "Running"
            steps.forEach { (definition, instanceSteps) ->
                increment("Step '${definition.label}'") {
                    var intro = "Provision step '${definition.id}'"
                    if (!definition.description.isNullOrBlank()) {
                        intro += " / ${definition.description}"
                    }
                    logger.info(intro)
                    common.parallel.each(instanceSteps) { instanceStep ->
                        actions.add(instanceStep.perform())
                    }
                }
            }
        }
        return actions
    }

    private fun stepsFiltered() = steps.filter { Patterns.wildcard(it.id, stepName.get()) }

    fun init() {
        val stepsFiltered = stepsFiltered()
        if (stepsFiltered.isEmpty()) {
            return
        }

        common.progress {
            total = stepsFiltered.size.toLong()
            step = "Initializing"
            stepsFiltered.forEach { definition ->
                increment("Step '${definition.label}'") {
                    definition.init()
                }
            }
        }
    }

    val fileResolver = FileResolver(aem.common).apply {
        downloadDir.apply {
            convention(aem.obj.buildDir("instance/provision/files"))
            aem.prop.file("instance.provision.filesDir")?.let { set(it) }
        }
    }

    // Predefined steps

    fun enableCrxDe(options: Step.() -> Unit = {}) = step("enableCrxDe") {
        description = "Enabling CRX DE"
        condition { once() && instance.env != "prod" }
        sync {
            osgi.configure("org.apache.sling.jcr.davex.impl.servlets.SlingDavExServlet", mapOf(
                    "alias" to "/crx/server"
            ))
        }
        options()
    }

    fun deployPackage(url: String, options: Step.() -> Unit = {}) = deployPackage(FilenameUtils.getBaseName(url), url, options)

    fun deployPackage(name: String, url: Any, options: Step.() -> Unit = {}) = step("deployPackage/${slug(name)}") {
        description = "Deploying package '$name'"
        version = name

        val file by lazy { aem.packageOptions.wrapper.wrap(fileResolver.get(url).file) }

        init {
            logger.info("Resolved package '$name' to be deployed is file '$file'")
        }
        sync {
            logger.info("Deploying package '$name' to $instance")
            if (packageManager.deploy(file)) {
                instance.awaitUp()
            }
        }
        options()
    }

    private fun slug(name: String) = name.replace(".", "-").replace(":", "_")

    init {
        aem.project.afterEvaluate {
            aem.prop.list("instance.provision.packageUrls")?.forEach { deployPackage(it) }
        }
    }
}
