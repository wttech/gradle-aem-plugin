package com.cognifide.gradle.sling.common.instance.provision

import com.cognifide.gradle.sling.common.instance.Instance
import com.cognifide.gradle.sling.common.instance.InstanceManager
import com.cognifide.gradle.sling.common.instance.provision.step.CustomStep
import com.cognifide.gradle.sling.common.instance.provision.step.DeployPackageStep
import com.cognifide.gradle.common.build.ProgressIndicator
import com.cognifide.gradle.common.file.resolver.FileResolver
import com.cognifide.gradle.common.utils.Patterns
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Configures Sling instances only in concrete circumstances (only once, after some time etc).
 */
class Provisioner(val manager: InstanceManager) {

    internal val sling = manager.sling

    private val common = sling.common

    private val logger = sling.logger

    /**
     * Allows to disable service at all.
     */
    val enabled = sling.obj.boolean {
        convention(true)
        sling.prop.boolean("instance.provision.enabled")?.let { set(it) }
    }

    /**
     * Forces to perform steps that supports greediness regardless their state on instances (already performed).
     */
    val greedy = sling.obj.boolean {
        convention(false)
        sling.prop.boolean("instance.provision.greedy")?.let { set(it) }
    }

    /**
     * Determines which steps should be performed selectively.
     */
    val stepName = sling.obj.string {
        convention("*")
        sling.prop.string("instance.provision.step")?.let { set(it) }
    }

    /**
     * Enables using step conditions based on counter e.g [Condition.repeatEvery].
     *
     * By default such conditions are disabled, because of extra HTTP calls (performance overhead).
     */
    val countable = sling.obj.boolean {
        convention(false)
        sling.prop.boolean("instance.provision.countable")?.let { set(it) }
    }

    /**
     * Determines a path in JCR repository in which provisioning metadata and step markers will be stored.
     */
    val path = sling.obj.string {
        convention("/var/gap/provision")
        sling.prop.string("instance.provision.path")?.let { set(it) }
    }

    private val steps = mutableListOf<Step>()

    /**
     * Define custom provision step.
     */
    fun step(id: String, options: CustomStep.() -> Unit) {
        steps.add(CustomStep(this).apply {
            this.id.set(id)
            options()
        })
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

        val steps = stepsFor(instances)
        steps.keys.forEach { it.validate() }

        val actions = provisionActions(steps)
        if (actions.none { it.status != Status.SKIPPED }) {
            logger.lifecycle("No steps to perform / all instances provisioned.")
        }

        return actions
    }

    private fun provisionActions(steps: Map<Step, List<InstanceStep>>): Collection<Action> {
        if (steps.isEmpty()) {
            return listOf()
        }

        return common.progress {
            initSteps(steps)
            performSteps(steps)
        }
    }

    private fun ProgressIndicator.initSteps(steps: Map<Step, List<InstanceStep>>) {
        total = steps.flatMap { it.value }.count().toLong()
        step = "Initializing"

        steps.forEach { (definition, instanceSteps) ->
            message = "Step \"${definition.label}\""

            var initializable = false
            instanceSteps.forEach { instanceStep ->
                increment("Step \"${definition.label}\" on '${instanceStep.instance.name}'") {
                    if (instanceStep.performable) {
                        initializable = true
                    }
                }
            }
            if (initializable) {
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
            val instancesStepsPerformed = instanceSteps.filter { it.performable }.map { it.instance }
            if (instancesStepsPerformed.isNotEmpty()) {
                definition.awaitUp(instancesStepsPerformed)
            }
        }

        return actions
    }

    fun init(instances: Collection<Instance>) {
        val steps = stepsFor(instances).keys
        if (steps.isEmpty()) {
            return
        }

        common.progress {
            total = steps.count().toLong()
            step = "Initializing"

            steps.forEach { step ->
                increment("Step \"${step.label}\"") {
                    step.init()
                }
            }
        }
    }

    val fileResolver = FileResolver(sling.common).apply {
        downloadDir.apply {
            convention(sling.obj.buildDir("instance/provision/files"))
            sling.prop.file("instance.provision.filesDir")?.let { set(it) }
        }
    }

    private fun stepsFor(instances: Collection<Instance>) = steps
            .filter { Patterns.wildcard(it.id.get(), stepName.get()) }
            .map { step -> step to instances.map { InstanceStep(it, step) } }
            .toMap()

    // Predefined steps

    fun enableCrxDe(options: Step.() -> Unit = {}) = step("enableCrxDe") {
        description.set("Enabling CRX DE")
        condition { once() && instance.env != "prod" }
        sync { osgi.configure("org.apache.sling.jcr.davex.impl.servlets.SlingDavExServlet", "alias", "/crx/server") }
        options()
    }

    fun disableCrxDe(options: Step.() -> Unit = {}) = step("disableCrxDe") {
        description.set("Disabling CRX DE")
        condition { once() && instance.env != "local" }
        sync { osgi.configure("org.apache.sling.jcr.davex.impl.servlets.SlingDavExServlet", "alias", "/crx") }
        options()
    }

    fun deployPackage(source: Any) = deployPackage { this.source.set(source) }

    fun deployPackage(options: DeployPackageStep.() -> Unit) {
        steps.add(DeployPackageStep(this).apply(options))
    }

    fun deployPackages(vararg sources: Any) = deployPackages(sources.asIterable())

    fun deployPackages(sources: Iterable<Any>) = sources.forEach { deployPackage(it) }
}
