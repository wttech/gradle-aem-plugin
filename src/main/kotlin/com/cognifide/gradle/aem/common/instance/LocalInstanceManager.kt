package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemVersion
import com.cognifide.gradle.aem.common.instance.action.AwaitDownAction
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.local.BackupManager
import com.cognifide.gradle.aem.common.instance.local.InstallResolver
import com.cognifide.gradle.aem.common.instance.local.JavaAgentResolver
import com.cognifide.gradle.aem.common.instance.local.OpenMode
import com.cognifide.gradle.aem.common.instance.local.QuickstartResolver
import com.cognifide.gradle.aem.common.instance.local.Source
import com.cognifide.gradle.aem.common.utils.FileUtil
import com.cognifide.gradle.aem.instance.LocalInstancePlugin
import com.cognifide.gradle.common.CommonException
import com.cognifide.gradle.common.pluginProject
import com.cognifide.gradle.common.utils.onEachApply
import com.cognifide.gradle.common.utils.using
import org.buildobjects.process.ProcBuilder
import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLauncher
import java.io.File
import java.io.Serializable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions", "MagicNumber")
class LocalInstanceManager(internal val aem: AemExtension) : Serializable {

    private val project = aem.project

    private val common = aem.common

    private val logger = aem.logger

    val base by lazy { aem.instanceManager }

    /**
     * Using local AEM instances is acceptable in any project so that lookup for project applying local instance plugin is required
     * Needed to determine e.g directory in which AEM quickstart will be stored and override files.
     */
    val projectDir = aem.obj.dir {
        convention(
            aem.obj.provider {
                project.pluginProject(LocalInstancePlugin.ID)?.layout?.projectDirectory ?: throw LocalInstanceException(
                    "Using local AEM instances requires having at least one project applying plugin '${LocalInstancePlugin.ID}'" +
                        " or setting property 'localInstance.projectDir'!"
                )
            }
        )
        aem.prop.string("localInstance.projectDir")?.let { set(project.rootProject.file(it)) }
    }

    /**
     * Path in which local AEM instance related files will be stored.
     */
    val rootDir = aem.obj.dir {
        convention(projectDir.dir(".gradle/aem/localInstance"))
        aem.prop.file("localInstance.rootDir")?.let { set(it) }
    }

    /**
     * Path in which local AEM instances will be stored.
     */
    val instanceDir = aem.obj.dir {
        convention(rootDir.dir("instance"))
        aem.prop.file("localInstance.instanceDir")?.let { set(it) }
    }

    /**
     * Path for storing local AEM instances related resources.
     */
    val configDir = aem.obj.dir {
        convention(projectDir.dir(aem.prop.string("localInstance.configPath") ?: "src/aem/localInstance"))
        aem.prop.file("localInstance.configDir")?.let { set(it) }
    }

    /**
     * Path from which e.g extra files for local AEM instances will be copied.
     * Useful for overriding default startup scripts ('start.bat' or 'start.sh') or providing some files inside 'crx-quickstart'.
     */
    val overrideDir = aem.obj.dir {
        convention(configDir.dir("override"))
        aem.prop.file("localInstance.overrideDir")?.let { set(it) }
    }

    /**
     * Determines how instances will be created (from backup or quickstart built from the scratch).
     */
    val source = aem.obj.typed<Source> {
        convention(Source.AUTO)
        aem.prop.string("localInstance.source")?.let { set(Source.of(it)) }
    }

    fun source(name: String) {
        source.set(Source.of(name))
    }

    val javaLauncher = aem.obj.typed<JavaLauncher> {
        convention(aem.common.javaSupport.launcher)
    }

    val javaExecutablePath get() = javaLauncher.get().executablePath.asFile.absolutePath

    /**
     * Automatically delete Quickstart JAR after unpacking.
     */
    val cleanJar = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("localInstance.cleanJar")?.let { set(it) }
    }

    /**
     * Automatically open a web browser when instances are up.
     */
    val openMode = aem.obj.typed<OpenMode> {
        convention(OpenMode.NEVER)
        aem.prop.string("localInstance.openMode")?.let { set(OpenMode.of(it)) }
    }

    fun openMode(name: String) {
        openMode.set(OpenMode.of(name))
    }

    /**
     * Maximum time to wait for browser open command response.
     */
    val openTimeout = aem.obj.long {
        convention(TimeUnit.SECONDS.toMillis(30))
        aem.prop.long("localInstance.openTimeout")?.let { set(it) }
    }

    /**
     * System service related options.
     */
    val serviceComposer by lazy { ServiceComposer(this) }

    fun service(options: ServiceComposer.() -> Unit) = serviceComposer.using(options)

    fun resolveFiles() {
        logger.info("Resolving local instance files")
        logger.info("Resolved local instance files:\n${sourceFiles.joinToString("\n")}")
    }

    /**
     * Maximum time to wait for start script response.
     */
    val startTimeout = aem.obj.long {
        convention(TimeUnit.SECONDS.toMillis(30))
        aem.prop.long("localInstance.startTimeout")?.let { set(it) }
    }

    /**
     * Maximum time to wait for stop script response.
     */
    val stopTimeout = aem.obj.long {
        convention(TimeUnit.SECONDS.toMillis(30))
        aem.prop.long("localInstance.stopTimeout")?.let { set(it) }
    }

    /**
     * Maximum time to wait for status script response.
     */
    val statusTimeout = aem.obj.long {
        convention(TimeUnit.SECONDS.toMillis(15))
        aem.prop.long("localInstance.statusTimeout")?.let { set(it) }
    }

    val controlTrigger by lazy { ControlTrigger(aem) }

    /**
     * Configure behavior of triggering instance up/down.
     */
    fun controlTrigger(options: ControlTrigger.() -> Unit) = controlTrigger.using(options)

    /**
     * Collection of files potentially needed to create instance
     */
    val sourceFiles = aem.obj.files {
        from(
            aem.obj.provider {
                listOfNotNull(backupZip) + quickstart.files + install.files
            }
        )
    }

    /**
     * Determines files in which properties can be injected.
     */
    val expandFiles = aem.obj.strings {
        set(
            listOf(
                "control/*.sh",
                "control/*.bat",
                "service/*.sh",
                "service/*.conf",
                "crx-quickstart/bin/*.sh",
                "crx-quickstart/bin/*.bat",
                "crx-quickstart/bin/start",
                "crx-quickstart/bin/status",
                "crx-quickstart/bin/stop"
            )
        )
    }

    /**
     * Determines files which executable rights will be applied.
     */
    val executableFiles = aem.obj.strings {
        set(
            listOf(
                "control/*.sh",
                "control/*.bat",
                "service/*.sh",
                "crx-quickstart/bin/*.sh",
                "crx-quickstart/bin/*.bat",
                "crx-quickstart/bin/start",
                "crx-quickstart/bin/status",
                "crx-quickstart/bin/stop"
            )
        )
    }

    /**
     * Custom properties that can be injected into instance files.
     */
    val expandProperties = aem.obj.map<String, Any> { convention(mapOf()) }

    val quickstart by lazy { QuickstartResolver(this) }

    /**
     * Configure AEM source files when creating instances from the scratch.
     */
    fun quickstart(options: QuickstartResolver.() -> Unit) = quickstart.using(options)

    /**
     * Configure AEM backup sources.
     */
    val backup by lazy { BackupManager(this) }

    fun backup(options: BackupManager.() -> Unit) = backup.using(options)

    val backupZip: File?
        get() {
            return when (source.get()) {
                Source.AUTO, Source.BACKUP_ANY -> backup.any
                Source.BACKUP_LOCAL -> backup.local
                Source.BACKUP_REMOTE -> backup.remote
                else -> null
            }
        }

    val install by lazy { InstallResolver(aem) }

    /**
     * Configure CRX packages, bundles to be pre-installed on instance(s).
     */
    fun install(options: InstallResolver.() -> Unit) = install.using(options)

    internal var initOptions: LocalInstance.() -> Unit = {}

    /**
     * Configure action to be performed only once when instance is up first time.
     */
    fun init(options: LocalInstance.() -> Unit) {
        this.initOptions = options
    }

    fun create(instance: LocalInstance) = create(listOf(instance))

    fun create(instances: Collection<LocalInstance> = aem.localInstances): List<LocalInstance> {
        val uncreatedInstances = instances.filter { !it.created }
        if (uncreatedInstances.isEmpty()) {
            logger.lifecycle("No instances to create.")
            return listOf()
        }

        logger.info("Creating instances: ${uncreatedInstances.names}")
        createBySource(uncreatedInstances)

        logger.info("Composing system service configuration")
        serviceComposer.compose()

        return uncreatedInstances.filter { it.created }
    }

    @Suppress("ComplexMethod")
    fun createBySource(instances: Collection<LocalInstance> = aem.localInstances) = when (source.get()) {
        Source.AUTO -> {
            val backupZip = backup.any
            when {
                backupZip != null -> createFromBackup(instances, backupZip)
                else -> createFromScratch(instances)
            }
        }
        Source.BACKUP_ANY -> {
            val backupZip = backup.any
            when {
                backupZip != null -> createFromBackup(instances, backupZip)
                else -> throw LocalInstanceException("Cannot create instance(s) because no backups available!")
            }
        }
        Source.BACKUP_LOCAL -> {
            val backupZip = backup.local
                ?: throw LocalInstanceException("Cannot create instance(s) because no local backups available!")
            createFromBackup(instances, backupZip)
        }
        Source.BACKUP_REMOTE -> {
            val backupZip = backup.remote
                ?: throw LocalInstanceException("Cannot create instance(s) because no remote backups available!")
            createFromBackup(instances, backupZip)
        }
        Source.SCRATCH -> createFromScratch(instances)
        null -> {}
    }

    fun createFromBackup(instances: Collection<LocalInstance> = aem.localInstances, backupZip: File) {
        backup.restore(backupZip, instanceDir.get().asFile, instances)

        val missingInstances = instances.filter { !it.created }
        if (missingInstances.isNotEmpty()) {
            logger.info("Backup ZIP '$backupZip' does not contain all instances. Creating from scratch: ${missingInstances.names}")

            createFromScratch(missingInstances)
        }

        common.progress(instances.size) {
            instances.onEachApply {
                increment("Customizing instance '$name'") {
                    logger.info("Customizing: $this")
                    customizeWhenDown()
                    logger.info("Customized: $this")
                }
            }
        }
    }

    fun createFromScratch(instances: Collection<LocalInstance> = aem.localInstances) {
        if (quickstart.distJar == null) {
            throw LocalInstanceException(
                "Cannot create instances due to lacking source files. " +
                    "Ensure having specified AEM SDK or Quickstart JAR url."
            )
        }
        if (quickstart.license == null) {
            throw LocalInstanceException(
                "Cannot create instances due to lacking source files. " +
                    "Ensure having specified AEM Quickstart license url."
            )
        }

        common.progress(instances.size) {
            instances.onEachApply {
                increment("Creating instance '$name'") {
                    if (created) {
                        logger.info(("Instance already created: $this"))
                        return@increment
                    }

                    logger.info("Creating: $this")
                    prepare()
                    logger.info("Created: $this")
                }
            }
        }
    }

    fun destroy(instance: LocalInstance): Boolean = destroy(listOf(instance)).isNotEmpty()

    fun destroy(instances: Collection<LocalInstance> = aem.localInstances): List<LocalInstance> {
        val createdInstances = instances.filter { it.touched }
        if (createdInstances.isEmpty()) {
            logger.lifecycle("No instances to destroy.")
            return listOf()
        }

        logger.info("Destroying instance(s): ${createdInstances.names}")

        common.progress(createdInstances.size) {
            createdInstances.onEachApply {
                increment("Destroying '$name'") {
                    logger.info("Destroying: $this")
                    delete()
                    logger.info("Destroyed: $this")
                }
            }
        }

        return createdInstances
    }

    fun up(instance: LocalInstance, awaitUpOptions: AwaitUpAction.() -> Unit = {}) = up(listOf(instance), awaitUpOptions).isNotEmpty()

    @Suppress("LongMethod")
    fun up(instances: Collection<LocalInstance> = aem.localInstances, awaitUpOptions: AwaitUpAction.() -> Unit = {}): List<LocalInstance> {
        val downInstances = instances.filter { it.runnable }
        if (downInstances.isEmpty()) {
            logger.lifecycle("No instances to turn on.")
            return listOf()
        }

        common.progress(downInstances.size) {
            downInstances.onEachApply {
                increment("Customizing instance '$name'") {
                    logger.info("Customizing: $this")
                    customizeWhenDown()
                    logger.info("Customized: $this")
                }
            }
        }

        common.progress(downInstances.size) {
            downInstances.onEachApply {
                increment("Starting instance '$name'") {
                    if (!created) {
                        logger.info("Instance not created, so it could not be up: $this")
                        return@increment
                    }

                    val status = checkStatus()
                    if (status.running) {
                        logger.info("Instance already running. No need to start: $this")
                        return@increment
                    }

                    sync {
                        if (!initialized) {
                            http.basicCredentials = Instance.CREDENTIALS_DEFAULT
                        }
                        controlTrigger.trigger(
                            action = { triggerUp() },
                            verify = { this@sync.status.reachable },
                            fail = {
                                val message = listOf(
                                    "Instance cannot be triggered up: $instance!",
                                    "Consider troubleshooting using file: '$stdoutLog'",
                                    "Last lines (30):"
                                )
                                val stdoutLines = FileUtil.readLastLines(stdoutLog, 30)
                                throw LocalInstanceException((message + stdoutLines).joinToString("\n"))
                            }
                        )
                    }
                }
            }
        }

        base.awaitUp(downInstances, awaitUpOptions)

        val uninitializedInstances = downInstances.filter { !it.initialized }
        common.progress(uninitializedInstances.size) {
            common.parallel.with(uninitializedInstances) {
                increment("Initializing instance '$name'") {
                    logger.info("Initializing: $this")
                    init(initOptions)
                }
            }
        }

        when {
            openMode.get() == OpenMode.ALWAYS -> open(downInstances)
            openMode.get() == OpenMode.ONCE -> open(uninitializedInstances)
        }

        return downInstances
    }

    private fun LocalInstance.triggerUp() {
        executeStartScript()
    }

    fun customize(instances: Collection<LocalInstance> = aem.localInstances) {
        common.progress(instances.size) {
            common.parallel.with(instances) {
                increment("Customizing instance '$name'") {
                    logger.info("Customizing: $this")
                    customizeWhenUp()
                }
            }
        }
    }

    fun down(instance: LocalInstance, awaitDownOptions: AwaitDownAction.() -> Unit = {}) = down(listOf(instance), awaitDownOptions).isNotEmpty()

    fun down(instances: Collection<LocalInstance> = aem.localInstances, awaitDownOptions: AwaitDownAction.() -> Unit = {}): List<LocalInstance> {
        val upInstances = instances.filter { it.running || it.available }
        if (upInstances.isEmpty()) {
            logger.lifecycle("No instances to turn off.")
            return listOf()
        }

        common.progress(upInstances.size) {
            upInstances.onEachApply {
                increment("Stopping instance '$name'") {
                    sync {
                        val initReachableStatus = status.checkReachableStatus()
                        controlTrigger.trigger(
                            action = { triggerDown() },
                            verify = { initReachableStatus != status.checkReachableStatus() },
                            fail = { throw LocalInstanceException("Instance cannot be triggered down: $instance!") }
                        )
                    }
                }
            }
        }

        base.awaitDown(upInstances, awaitDownOptions)

        return upInstances
    }

    private fun LocalInstance.triggerDown() {
        if (created) {
            val status = checkStatus()
            if (status.running) {
                executeStopScript()
            } else if (available) {
                logger.warn("Instance not running (reports status '$status'), but available. Stopping OSGi on: $this")
                sync.osgi.stop()
            }
        } else if (available) {
            sync.osgi.stop()
            logger.warn("Instance not created, but available. Stopping OSGi on: $this")
        }
    }

    fun open(instance: LocalInstance) = open(listOf(instance))

    fun open(instances: Collection<LocalInstance> = aem.localInstances): List<LocalInstance> {
        val upInstances = instances.filter { it.running }
        if (upInstances.isEmpty()) {
            logger.lifecycle("No instances to open.")
            return listOf()
        }

        val openedInstances = CopyOnWriteArrayList<LocalInstance>()
        common.progress(upInstances.size) {
            common.parallel.with(upInstances) {
                increment("Opening instance '$name'") {
                    try {
                        aem.webBrowser.open(httpOpenUrl) { withTimeoutMillis(openTimeout.get()) }
                        openedInstances += this@with
                    } catch (e: AemException) {
                        logger.debug("Instance '$name' open error", e)
                        logger.warn("Cannot open instance '$name'! Cause: ${e.message}")
                    }
                }
            }
        }
        if (openedInstances.isNotEmpty()) {
            logger.lifecycle(
                "Opened instances (${openedInstances.size}) in web browser (tabs):\n" +
                    openedInstances.joinToString("\n") { "Instance '${it.name}' at URL '${it.httpOpenUrl}'" }
            )
        }

        return openedInstances
    }

    fun kill(instance: LocalInstance) = kill(listOf(instance))

    fun kill(instances: Collection<LocalInstance> = aem.localInstances): List<LocalInstance> {
        val killableInstances = instances.filter { it.pid > 0 }
        if (killableInstances.isEmpty()) {
            logger.lifecycle("No instances to kill.")
            return listOf()
        }

        val killedInstances = CopyOnWriteArrayList<LocalInstance>()
        common.progress(killableInstances.size) {
            common.parallel.with(killableInstances) {
                increment("Killing instance '$name'") {
                    try {
                        aem.processKiller.kill(pid)
                        killedInstances += this@with
                    } catch (e: AemException) {
                        logger.debug("Instance '$name' kill error", e)
                        logger.warn("Cannot kill instance '$name'! Cause: ${e.message}")
                    }
                }
            }
        }

        if (killedInstances.isEmpty()) {
            logger.lifecycle("No instances killed!")
        }

        return killedInstances
    }

    val examineEnabled = aem.obj.boolean {
        convention(base.examineEnabled)
        aem.prop.boolean("localInstance.examination.enabled")?.let { set(it) }
    }

    fun examine(instances: Collection<LocalInstance> = aem.localInstances) {
        if (!examineEnabled.get()) {
            return
        }

        base.examine(instances)
    }

    val examinePrerequisites = aem.obj.boolean {
        convention(base.examineEnabled)
        aem.prop.boolean("localInstance.examination.prerequisites")?.let { set(it) }
    }

    fun examinePrerequisites(instances: Collection<LocalInstance> = aem.localInstances) {
        if (!examinePrerequisites.get()) {
            return
        }

        examineJavaAvailable()
        examineJavaCompatibility(instances)
        examineStatusUncorecognized(instances)
        examineRunningOther(instances)
    }

    /**
     * Pre-conditionally check if 'java' is available in shell scripts.
     *
     * Gradle is intelligently looking by its own for installed Java, but AEM control scripts
     * are just requiring 'java' command available in 'PATH' environment variable.
     */
    @Suppress("TooGenericExceptionCaught", "MagicNumber")
    fun examineJavaAvailable() {
        try {
            logger.debug("Examining Java properly installed")

            val result = ProcBuilder(javaExecutablePath, "-version")
                .withWorkingDirectory(rootDir.get().asFile.apply { mkdirs() })
                .withTimeoutMillis(statusTimeout.get())
                .withExpectedExitStatuses(0)
                .run()
            logger.debug("Installed Java:\n${result.outputString.ifBlank { result.errorString }}")
        } catch (e: Exception) {
            throw LocalInstanceException(
                "Local instances support requires Java properly installed! Cause: '${e.message}'\n" +
                    "Ensure having directory with 'java' executable listed in 'PATH' environment variable.",
                e
            )
        }
    }

    /**
     * Defines compatibility related to AEM versions and Java versions.
     *
     * AEM version is definable as range inclusive at a start, exclusive at an end.
     * Java Version is definable as list of supported versions pipe delimited.
     */
    val javaCompatibility = aem.obj.map<String, String> {
        convention(
            mapOf(
                "6.0.0-6.5.0" to "1.7|1.8",
                "6.5.0-6.6.0" to "1.8|11",
                "*.*.*.*T*Z" to "1.8|11" // cloud service
            )
        )
        aem.prop.map("localInstance.javaCompatibility")?.let { set(it) }
    }

    fun determineJavaCompatibleVersions(): List<JavaVersion> {
        val aemVersion = try {
            quickstart.distJar?.let { AemVersion.fromJar(it) }
        } catch (e: CommonException) {
            logger.info("Determining Java compatible versions for specified AEM quickstart JAR is not possible.")
            logger.debug("Cannot determine Java compatible versions basing on AEM quickstart JAR!", e)
            null
        }
        return aemVersion?.javaCompatibleVersions(javaCompatibility.get()) ?: listOf()
    }

    fun examineJavaCompatibility(instances: Collection<LocalInstance> = aem.localInstances) {
        if (javaCompatibility.get().isEmpty()) {
            logger.debug("Examining Java compatibility skipped as configuration not provided!")
            return
        }

        logger.debug("Examining Java compatibility for configuration: ${javaCompatibility.get()}")

        val javaVersionCurrent = JavaVersion.toVersion(javaLauncher.get().metadata.languageVersion)
        val errors = instances.fold(mutableListOf<String>()) { result, instance ->
            val aemVersion = instance.version
            if (aemVersion == AemVersion.UNKNOWN) {
                logger.info("Cannot examine Java compatibility because AEM version is unknown for $instance")
            } else {
                val javaVersionCompatibles = aemVersion.javaCompatibleVersions(javaCompatibility.get())
                if (javaVersionCurrent !in javaVersionCompatibles) {
                    result.add(
                        "Instance '${instance.name}' using URL '${instance.httpUrl.get()}' is AEM $aemVersion" +
                            " and requires Java ${javaVersionCompatibles.joinToString("|")}!"
                    )
                }
            }
            result
        }
        if (errors.isNotEmpty()) {
            throw LocalInstanceException(
                "Some instances (${errors.size}) require different Java version than current $javaVersionCurrent:\n" +
                    errors.joinToString("\n")
            )
        }
    }

    fun examineStatusUncorecognized(instances: Collection<LocalInstance>) {
        val unrecognized = instances.filter { it.created }
            .map { it to it.checkStatus() }
            .filter { it.second.unrecognized }
        if (unrecognized.isNotEmpty()) {
            throw LocalInstanceException(
                "Some instances are created but their status is unrecognized:\n" +
                    unrecognized.joinToString("\n") { (i, s) ->
                        "Instance '${i.name}' located at path '${i.dir}' reports status exit code ${s.exitValue}"
                    } + "\n\n" +
                    "Ensure that shell scripts have an ability to execute 'java' process or try rebooting machine."
            )
        }
    }

    fun examineRunningOther(instances: Collection<LocalInstance> = aem.localInstances) {
        val running = instances.filter { it.runningOther }
        if (running.isNotEmpty()) {
            throw LocalInstanceException(
                "Other instances (${running.size}) are running:\n" +
                    running.joinToString("\n") { "Instance '${it.name}' using URL '${it.httpUrl.get()}' located at path '${it.runningDir}'" } + "\n\n" +
                    "Ensure having these instances down."
            )
        }
    }

    val javaAgent by lazy { JavaAgentResolver(aem) }

    /**
     * Configure Java agents for instrumenting AEM instances.
     */
    fun javaAgent(options: JavaAgentResolver.() -> Unit) = javaAgent.using(options)

    /**
     * Hook for additional configuration for defined instances.
     */
    fun defined(options: LocalInstance.() -> Unit) {
        base.defined { whenLocal(options) }
    }

    // Null-safe accessors for easy DSL scripting

    val jar get() = quickstart.distJar?.takeIf { it.exists() }
        ?: quickstart.sdkJar?.takeIf { it.exists() }
        ?: throw LocalInstanceException("Instance JAR file not found! Is instance AEM SDK or Quickstart JAR URL configured?")

    val license get() = quickstart.license?.takeIf { it.exists() }
        ?: throw LocalInstanceException("Instance license file not found! Is instance license URL configured?")

    val sdkDir get() = quickstart.sdk?.parentFile?.takeIf { it.exists() }
        ?: throw LocalInstanceException("SDK dir not found! Is SDK URL configured?")

    val dispatcherImage get() = quickstart.sdkDispatcherImage?.takeIf { it.exists() }
        ?: throw LocalInstanceException("Dispatcher image not found! Is SDK URL configured?")

    val dispatcherDir get() = quickstart.sdkDispatcherDir?.takeIf { it.exists() }
        ?: throw LocalInstanceException("Dispatcher dir not found! Is SDK URL configured?")
}
