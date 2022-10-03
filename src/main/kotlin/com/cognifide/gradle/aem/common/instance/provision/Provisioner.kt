package com.cognifide.gradle.aem.common.instance.provision

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceManager
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.provision.step.ConfigureCryptoStep
import com.cognifide.gradle.aem.common.instance.provision.step.ConfigureReplicationAgentStep
import com.cognifide.gradle.aem.common.instance.provision.step.ConfigureWorkflowLauncherStep
import com.cognifide.gradle.aem.common.instance.provision.step.CustomStep
import com.cognifide.gradle.aem.common.instance.provision.step.DeployPackageStep
import com.cognifide.gradle.aem.common.instance.provision.step.ImportMappingsStep
import com.cognifide.gradle.aem.common.instance.service.repository.ReplicationAgent
import com.cognifide.gradle.common.build.ProgressIndicator
import com.cognifide.gradle.common.file.resolver.FileResolver
import com.cognifide.gradle.common.utils.Patterns
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Configures AEM instances only in concrete circumstances (only once, after some time etc).
 */
@Suppress("TooManyFunctions")
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

    private var awaitUpOptions: AwaitUpAction.() -> Unit = {
        unchanged { enabled.set(false) }
    }

    /**
     * Allows to customize instance stability checking performed before provisioning.
     */
    fun awaitUp(options: AwaitUpAction.() -> Unit) {
        this.awaitUpOptions = options
    }

    /**
     * Define custom provision step.
     */
    fun step(id: String, options: CustomStep.() -> Unit) {
        steps.add(
            CustomStep(this).apply {
                this.id.set(id)
                options()
            }
        )
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
            message = definition.label

            var initializable = false
            instanceSteps.forEach { instanceStep ->
                increment("${definition.label} on '${instanceStep.instance.name}'") {
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
            message = definition.label

            if (definition.runInParallel.get()) {
                common.parallel.each(instanceSteps) { instanceStep ->
                    increment("${definition.label} on '${instanceStep.instance.name}'") {
                        actions.add(instanceStep.perform())
                    }
                }
            } else {
                instanceSteps.forEach { instanceStep ->
                    increment("${definition.label} on '${instanceStep.instance.name}'") {
                        actions.add(instanceStep.perform())
                    }
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

    val fileResolver = FileResolver(aem.common).apply {
        downloadDir.apply {
            convention(aem.project.layout.buildDirectory.dir("instance/provision/files"))
            aem.prop.file("instance.provision.filesDir")?.let { set(it) }
        }
    }

    private fun stepsFor(instances: Collection<Instance>) = steps
        .filter { Patterns.wildcard(it.id.get(), stepName.get()) }
        .map { step -> step to instances.map { InstanceStep(it, step) } }
        .toMap()

    // Predefined steps

    fun syncOnce(stepName: String, options: InstanceSync.() -> Unit) = step(stepName) { condition { once() }; sync(options) }
    fun syncOnceOnAuthor(stepName: String, options: InstanceSync.() -> Unit) = step(stepName) { condition { onAuthor() && once() }; sync(options) }
    fun syncOnceOnPublish(stepName: String, options: InstanceSync.() -> Unit) = step(stepName) { condition { onPublish() && once() }; sync(options) }

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

    fun deployPackage(options: DeployPackageStep.() -> Unit) {
        steps.add(DeployPackageStep(this).apply(options))
    }

    fun deployPackage(source: Any, options: DeployPackageStep.() -> Unit = {}) = deployPackage {
        this.source.set(source)
        options()
    }

    fun deployPackages(vararg sources: Any, options: DeployPackageStep.() -> Unit = {}) = deployPackages(sources.asIterable(), options)

    fun deployPackages(sources: Iterable<Any>, options: DeployPackageStep.() -> Unit = {}) = sources.forEach { deployPackage(it, options) }

    fun deployPackagesFrom(dir: File, options: DeployPackageStep.() -> Unit = {}) {
        checkPackagesDir(dir)
        dir.walkTopDown()
            .filter { it.isFile && it.extension == "zip" }
            .sortedBy { it.absolutePath }
            .forEach { deployPackage(it, options) }
    }

    fun deployPackagesNamed(parentDir: File, fileNames: Iterable<String>, options: DeployPackageStep.() -> Unit = {}) {
        checkPackagesDir(parentDir)
        fileNames.map { parentDir.resolve(it) }.forEach { deployPackage(it, options) }
    }

    private fun checkPackagesDir(dir: File) {
        if (!dir.exists()) {
            throw ProvisionException("Packages directory does not exist at path '$dir'!")
        }
    }

    fun evalGroovyScript(fileName: String, options: Step.() -> Unit = {}) = evalGroovyScript(fileName, mapOf(), options)

    fun evalGroovyScript(fileName: String, data: Map<String, Any?> = mapOf(), options: Step.() -> Unit = {}) = step("evalGroovyScript/$fileName") {
        description.set("Evaluating Groovy Script '$fileName'")
        sync { groovyConsole.evalScript(fileName, data) }
        options()
    }

    fun deleteReplicationAgents(options: Step.() -> Unit = {}) = step("deleteReplicationAgents") {
        description.set("Deleting replication agents")
        sync { repository.replicationAgents().forEach { it.delete() } }
        options()
    }

    fun configureReplicationAgent(location: String, name: String, options: ConfigureReplicationAgentStep.() -> Unit = {}) {
        steps.add(ConfigureReplicationAgentStep(this, location, name).apply(options))
    }

    fun configureReplicationAgentAuthor(name: String, options: ConfigureReplicationAgentStep.() -> Unit = {}) {
        configureReplicationAgent(ReplicationAgent.LOCATION_AUTHOR, name) {
            condition { onAuthor() && once() }
            options()
        }
    }

    fun configureReplicationAgentPublish(name: String, options: ConfigureReplicationAgentStep.() -> Unit = {}) {
        configureReplicationAgent(ReplicationAgent.LOCATION_PUBLISH, name) {
            condition { onPublish() && once() }
            options()
        }
    }

    fun configureCrypto(options: ConfigureCryptoStep.() -> Unit) {
        steps.add(
            ConfigureCryptoStep(this).apply {
                condition { onLocal() && once() }
                options()
            }
        )
    }

    fun configureCrypto(hmac: Any, master: Any, options: ConfigureCryptoStep.() -> Unit = {}) = configureCrypto {
        this.hmac.set(hmac)
        this.master.set(master)
        options()
    }

    fun configureCrypto(dirUrl: String, options: ConfigureCryptoStep.() -> Unit = {}) = configureCrypto("$dirUrl/hmac", "$dirUrl/master", options)

    fun configureCryptos(dirUrl: String, options: ConfigureCryptoStep.() -> Unit = {}) = configureCryptos("$dirUrl/author", "$dirUrl/publish", options)

    fun configureCryptos(authorDirUrl: String, publishDirUrl: String, options: ConfigureCryptoStep.() -> Unit = {}) {
        configureCrypto(authorDirUrl) {
            condition { onLocal() && onAuthor() && once() }
            options()
        }
        configureCrypto(publishDirUrl) {
            condition { onLocal() && onPublish() && once() }
            options()
        }
    }

    fun configureWorkflowLauncher(id: String, options: ConfigureWorkflowLauncherStep.() -> Unit) {
        steps.add(ConfigureWorkflowLauncherStep(this, id).apply(options))
    }

    fun configureOsgi(pid: String, properties: Map<String, Any?>, options: Step.() -> Unit = {}) = step("configureOsgi/$pid") {
        description.set("Setting OSGi config '$pid'")
        sync { osgi.configure(pid, properties) }
        version(properties)
        options()
    }

    fun importMappings(fileName: String, options: ImportMappingsStep.() -> Unit = {}) {
        steps.add(ImportMappingsStep(this, fileName).apply(options))
    }
}
