package com.cognifide.gradle.aem.common.instance.provision

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceManager
import com.cognifide.gradle.aem.common.instance.provision.step.CustomStep
import com.cognifide.gradle.aem.common.instance.provision.step.DeployPackageStep
import com.cognifide.gradle.common.build.ProgressIndicator
import com.cognifide.gradle.common.file.resolver.FileResolver
import com.cognifide.gradle.common.utils.Patterns
import org.apache.commons.io.FilenameUtils
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

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
     *
     * By default such conditions are disabled, because of extra HTTP calls (performance overhead).
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
     * Define custom provision step.
     */
    fun step(id: String, options: CustomStep.() -> Unit) {
        steps.add(CustomStep(this, id).apply(options))
    }

    /**
     * Perform all provision steps for specified instance.
     */
    fun provision(instance: Instance) = provision(listOf(instance))

    /**
     * Perform all provision steps for all instances in parallel.
     */
    fun provision(instances: Collection<Instance>): Collection<Action> {
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

    private fun provisionActions(instances: Collection<Instance>): Collection<Action> {
        val stepsFiltered = stepsFor(instances)
        if (stepsFiltered.isEmpty()) {
            return listOf()
        }

        return common.progress {
            initSteps(stepsFiltered)
            performSteps(stepsFiltered)
        }
    }

    private fun ProgressIndicator.initSteps(steps: Map<Step, List<InstanceStep>>) {
        total = steps.flatMap { it.value }.count().toLong()
        step = "Initializing"

        steps.forEach { (definition, instanceSteps) ->
            message = "Step \"${definition.label}\""

            val initializable = AtomicBoolean(false)
            common.parallel.each(instanceSteps) { instanceStep ->
                increment("Step \"${definition.label}\" on '${instanceStep.instance.name}'") {
                    if (instanceStep.performable) {
                        initializable.getAndSet(true)
                    }
                }
            }
            if (initializable.get()) {
                definition.init()
            }
        }
    }

    private fun ProgressIndicator.performSteps(steps: Map<Step, List<InstanceStep>>): List<Action> {
        val actions = CopyOnWriteArrayList<Action>()

        total = steps.flatMap { it.value }.count().toLong()
        count = 0
        step = "Running"

        steps.forEach { (definition, instanceSteps) ->
            message = "Step \"${definition.label}\""

            common.parallel.each(instanceSteps) { instanceStep ->
                increment("Step \"${definition.label}\" on '${instanceStep.instance.name}'") {
                    actions.add(instanceStep.perform())
                }
            }
            definition.awaitUp(instanceSteps.filter { it.performable }.map { it.instance })
        }

        return actions
    }

    fun init(instances: Collection<Instance>) {
        val steps = stepsFor(instances)
        if (steps.isEmpty()) {
            return
        }

        common.progress {
            initSteps(steps)
        }
    }

    val fileResolver = FileResolver(aem.common).apply {
        downloadDir.apply {
            convention(aem.obj.buildDir("instance/provision/files"))
            aem.prop.file("instance.provision.filesDir")?.let { set(it) }
        }
    }

    private fun stepsFor(instances: Collection<Instance>) = steps
            .filter { Patterns.wildcard(it.id, stepName.get()) }
            .map { step -> step to instances.map { InstanceStep(it, step) } }
            .toMap()

    // Predefined steps

    fun enableCrxDe(options: Step.() -> Unit = {}) = step("enableCrxDe") {
        description.set("Enabling CRX DE")
        condition { once() && instance.env != "prod" }
        sync {
            osgi.configure("org.apache.sling.jcr.davex.impl.servlets.SlingDavExServlet", mapOf(
                    "alias" to "/crx/server"
            ))
        }
        options()
    }

    fun deployPackage(url: String, options: DeployPackageStep.() -> Unit = {}) = deployPackage(FilenameUtils.getBaseName(url), url, options)

    fun deployPackage(name: String, url: Any, options: DeployPackageStep.() -> Unit = {}) {
        steps.add(DeployPackageStep(this, name, url).apply(options))
    }

    init {
        aem.project.afterEvaluate {
            aem.prop.list("instance.provision.deployPackage.urls")?.forEach { deployPackage(it) }
        }
    }
}
