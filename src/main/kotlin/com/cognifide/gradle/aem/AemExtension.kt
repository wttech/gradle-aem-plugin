package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.common.build.*
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.file.FileWatcher
import com.cognifide.gradle.aem.common.file.transfer.FileTransferManager
import com.cognifide.gradle.aem.common.file.transfer.http.HttpFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.sftp.SftpFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.smb.SmbFileTransfer
import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.common.instance.*
import com.cognifide.gradle.aem.common.notifier.NotifierFacade
import com.cognifide.gradle.aem.common.pkg.PackageDefinition
import com.cognifide.gradle.aem.common.pkg.PackageFile
import com.cognifide.gradle.aem.common.pkg.PackageOptions
import com.cognifide.gradle.aem.common.pkg.vlt.FilterFile
import com.cognifide.gradle.aem.common.utils.Formats
import com.cognifide.gradle.aem.common.utils.LineSeparator
import com.cognifide.gradle.aem.common.utils.Patterns
import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentPlugin
import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import com.cognifide.gradle.aem.tooling.ToolingPlugin
import com.cognifide.gradle.aem.tooling.vlt.VltException
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.io.Serializable
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Internal

/**
 * Core of library, facade for implementing tasks, configuration aggregator.
 */
@Suppress("TooManyFunctions")
class AemExtension(@JsonIgnore val project: Project) : Serializable {

    @JsonIgnore
    val logger = project.logger

    /**
     * Allows to read project property specified in command line and system property as a fallback.
     */
    @JsonIgnore
    val props = PropertyParser(this)

    /**
     * Project under which common configuration files are stored.
     * Usually it is also a project which is building full assembly CRX package.
     *
     * Convention assumes in case of:
     * - multi-project build - subproject with path ':aem'
     * - single-project build - root project
     */
    @JsonIgnore
    val projectMain: Project = project.findProject(props.string("projectMainPath") ?: ":aem") ?: project.rootProject

    /**
     * Project name convention prefixes used to determine default:
     *
     * - bundle install subdirectory
     * - CRX package base name
     * - OSGi bundle JAR base name
     *
     * in case of multi-project build and assembly packages.
     */
    val projectPrefixes: List<String> = props.list("projectPrefixes") ?: listOf("aem.", "aem-", "aem_")

    /**
     * Project name with skipped convention prefixes.
     */
    val projectName: String
        get() = project.name.run {
            var n = this; projectPrefixes.forEach { n = n.removePrefix(it) }; n
        }

    /**
     * Base name used as default for CRX packages being created by compose or collect task
     * and also for OSGi bundle JARs.
     */
    val baseName: String
        get() = Formats.normalizeSeparators(if (project == project.rootProject) {
            project.rootProject.name
        } else {
            "${project.rootProject.name}.$projectName"
        }, ".")

    /**
     * Allows to disable features that are using running instances.
     *
     * Gradle's offline mode does much more. It will not use any Maven repository so that CI build
     * will fail which is not expected in integration tests.
     */
    val offline = props.boolean("offline") ?: project.gradle.startParameter.isOffline

    /**
     * Determines current environment name to be used in e.g package deployment.
     */
    val env: String = props.string("env") ?: run { System.getenv("ENV") ?: "local" }

    /**
     * Specify characters to be used as line endings when cleaning up checked out JCR content.
     */
    var lineSeparator: String = props.string("lineSeparator") ?: "LF"

    @JsonIgnore
    val lineSeparatorString: String = LineSeparator.string(lineSeparator)

    /**
     * Directory for storing project specific files used by plugin e.g:
     * - Groovy Scripts to be launched by Groovy Console instance service in tasks defined in project.
     */
    val configDir: File
        get() = project.file(props.string("configDir") ?: "gradle")

    /**
     * Directory for storing common files used by plugin e.g:
     * - CRX package thumbnail
     * - environment configuration files (HTTPD virtual hosts, Dispatcher)
     * - instance overrides files
     * - tail incident filter
     */
    val configCommonDir: File
        get() = projectMain.file(props.string("configCommonDir") ?: "gradle")

    @get:Internal
    val fileTransfer = FileTransferManager(this)

    /**
     * Define settings for file transfer facade which allows to perform basic file operations on remote servers
     * like uploading and downloading files.
     *
     * Supports multiple protocols: HTTP, SFTP, SMB and other supported by JVM.
     */
    fun fileTransfer(options: FileTransferManager.() -> Unit) {
        fileTransfer.apply(options)
    }

    val packageOptions = PackageOptions(this)

    /**
     * Defines common settings for built packages and deployment related behavior.
     */
    fun `package`(options: PackageOptions.() -> Unit) {
        packageOptions.apply(options)
    }

    /**
     * Defines common settings for built packages and deployment related behavior.
     */
    fun pkg(options: PackageOptions.() -> Unit) = `package`(options)

    /**
     * Read CRX package properties of specified ZIP file.
     */
    fun `package`(file: File) = PackageFile(file)

    /**
     * Read CRX package properties of specified ZIP file.
     */
    fun pkg(file: File) = `package`(file)

    val instanceOptions = InstanceOptions(this)

    /**
     * Defines instances to work with.
     */
    fun instance(options: InstanceOptions.() -> Unit) {
        instanceOptions.apply(options)
    }

    val localInstanceManager = LocalInstanceManager(this)

    /**
     * Define common settings valid only for instances created at local file system.
     */
    fun localInstance(options: LocalInstanceManager.() -> Unit) = localInstanceManager.apply(options)

    /**
     * Provides API for controlling virtualized AEM environment with HTTPD and dispatcher module.
     */
    val environment = Environment(this)

    fun environment(configurer: Environment.() -> Unit) {
        environment.apply(configurer)
    }

    /**
     * Provides API for displaying interactive notification during running build tasks.
     */
    val notifier = NotifierFacade.of(this)

    fun notifier(configurer: NotifierFacade.() -> Unit) {
        notifier.apply(configurer)
    }

    /**
     * Provides API for easier creation of tasks (e.g in sequence) in the matter of Gradle task configuration avoidance.
     */
    val tasks = AemTaskFacade(this)

    fun tasks(configurer: AemTaskFacade.() -> Unit) {
        tasks.apply(configurer)
    }

    /**
     * Provides API for performing actions affecting multiple instances at once.
     */
    @JsonIgnore
    val instanceActions = InstanceActionPerformer(this)

    /**
     * Collection of all java packages from all projects applying bundle plugin.
     */
    val javaPackages: List<String>
        get() = AemPlugin.withId(project, BundlePlugin.ID).flatMap { subproject ->
            of(subproject).tasks.bundles.mapNotNull { it.javaPackage }
        }

    /**
     * All instances matching default filtering.
     *
     * @see <https://github.com/Cognifide/gradle-aem-plugin#filter-instances-to-work-with>
     */
    @get:JsonIgnore
    val instances: List<Instance>
        get() = filterInstances()

    /**
     * Work in parallel with instances matching default filtering.
     */
    fun instances(consumer: (Instance) -> Unit) = parallel.with(instances, consumer)

    /**
     * Work in parallel with instances which name is matching specified wildcard filter.
     */
    fun instances(filter: String, consumer: (Instance) -> Unit) = parallel.with(filterInstances(filter), consumer)

    /**
     * Shorthand method for getting defined instance or creating temporary instance by URL.
     */
    fun instance(urlOrName: String): Instance = instanceOptions.parse(urlOrName)

    /**
     * Shorthand method for getting defined instances or creating temporary instances by URLs.
     */
    fun instances(urlsOrNames: Iterable<String>): List<Instance> = urlsOrNames.map { instance(it) }

    /**
     * Get or create instance using command line parameter named 'instance' which holds instance name or URL.
     * If it is not specified, then first instance matching default filtering fill be returned.
     *
     * Purpose of this method is to easily get any instance to work with (no matter how it will be defined).
     *
     * @see <https://github.com/Cognifide/gradle-aem-plugin#filter-instances-to-work-with>
     */
    @get:JsonIgnore
    val anyInstance: Instance
        get() {
            val cmdInstanceArg = props.string("instance")
            if (!cmdInstanceArg.isNullOrBlank()) {
                return instance(cmdInstanceArg)
            }

            return namedInstance(Instance.FILTER_ANY)
        }

    /**
     * Get all instances which names are matching wildcard filter specified via command line parameter 'instance.name'.
     * By default, this method respects current environment which is used to work only with instances running locally.
     *
     * If none instances will be found, throws exception.
     */
    fun namedInstance(desiredName: String? = props.string("instance.name"), defaultName: String = "$env-*"): Instance {
        val nameMatcher: String = desiredName ?: defaultName

        val namedInstance = filterInstances(nameMatcher).firstOrNull()
        if (namedInstance != null) {
            return namedInstance
        }

        throw InstanceException("Instance named '$nameMatcher' is not defined.")
    }

    /**
     * Find all instances which names are matching wildcard filter specified via command line parameter 'instance.name'.
     */
    fun filterInstances(nameMatcher: String = props.string("instance.name") ?: "$env-*"): List<Instance> {
        val all = instanceOptions.defined.values

        // Specified by command line should not be filtered
        val cmd = all.filter { it.environment == Instance.ENVIRONMENT_CMD }
        if (cmd.isNotEmpty()) {
            return cmd
        }

        // Defined by build script, via properties or defaults are filterable by name
        return all.filter { instance ->
            when {
                props.flag("instance.authors") -> {
                    Patterns.wildcard(instance.name, "$env-${IdType.AUTHOR}*")
                }
                props.flag("instance.publishes") || props.flag("instance.publishers") -> {
                    Patterns.wildcard(instance.name, "$env-${IdType.PUBLISH}*")
                }
                else -> Patterns.wildcard(instance.name, nameMatcher)
            }
        }
    }

    /**
     * Get all author instances running on current environment.
     */
    @get:JsonIgnore
    val authorInstances: List<Instance>
        get() = filterInstances().filter { it.type == IdType.AUTHOR }

    /**
     * Work in parallel with all author instances running on current environment.
     */
    fun authorInstances(consumer: (Instance) -> Unit) = parallel.with(authorInstances, consumer)

    /**
     * Get all publish instances running on current environment.
     */
    @get:JsonIgnore
    val publishInstances: List<Instance>
        get() = filterInstances().filter { it.type == IdType.PUBLISH }

    /**
     * Work in parallel with all publish instances running on current environment.
     */
    fun publishInstances(consumer: Instance.() -> Unit) = parallel.with(publishInstances, consumer)

    /**
     * Get all local instances.
     */
    @get:JsonIgnore
    val localInstances: List<LocalInstance>
        get() = instances.filterIsInstance(LocalInstance::class.java)

    /**
     * Work in parallel with all local instances.
     */
    fun localInstances(consumer: LocalInstance.() -> Unit) = parallel.with(localInstances, consumer)

    /**
     * Get all remote instances.
     */
    @get:JsonIgnore
    val remoteInstances: List<RemoteInstance>
        get() = instances.filterIsInstance(RemoteInstance::class.java)

    /**
     * Work in parallel with all remote instances.
     */
    fun remoteInstances(consumer: RemoteInstance.() -> Unit) = parallel.with(remoteInstances, consumer)

    /**
     * Get CRX package defined to be built (could not yet exist).
     */
    @Suppress("VariableNaming")
    @get:JsonIgnore
    val `package`: File
        get() = tasks.get(PackageCompose.NAME, PackageCompose::class.java).archiveFile.get().asFile

    @get:JsonIgnore
    val pkg: File
        get() = `package`

    /**
     * Get all CRX packages defined to be built.
     */
    @get:JsonIgnore
    val packages: List<File>
        get() = project.tasks.withType(PackageCompose::class.java)
                .map { it.archiveFile.get().asFile }

    /**
     * Get all CRX packages built before running particular task.
     */
    fun dependentPackages(task: Task): List<File> {
        return task.taskDependencies.getDependencies(task)
                .filterIsInstance(PackageCompose::class.java)
                .map { it.archiveFile.get().asFile }
    }

    /**
     * In parallel, work with services of all instances matching default filtering.
     */
    fun sync(synchronizer: InstanceSync.() -> Unit) = sync(instances, synchronizer)

    /**
     * Work with instance services of specified instance.
     */
    fun <T> sync(instance: Instance, synchronizer: InstanceSync.() -> T) = instance.sync(synchronizer)

    /**
     * In parallel, work with services of all specified instances.
     */
    fun sync(instances: Iterable<Instance>, synchronizer: InstanceSync.() -> Unit) {
        parallel.with(instances) { this.sync.apply(synchronizer) }
    }

    /**
     * In parallel, work with built packages and services of instances matching default filtering.
     */
    fun syncPackages(synchronizer: InstanceSync.(File) -> Unit) = syncPackages(instances, packages, synchronizer)

    /**
     * In parallel, work with built packages and services of specified instances.
     */
    fun syncPackages(instances: Iterable<Instance>, packages: Iterable<File>, synchronizer: InstanceSync.(File) -> Unit) {
        packages.forEach { pkg -> // single AEM instance dislikes parallel package installation
            parallel.with(instances) { // but same package could be in parallel deployed on different AEM instances
                sync.apply { synchronizer(pkg) }
            }
        }
    }

    /**
     * Build minimal CRX package in-place / only via code.
     * All details like Vault properties, archive destination directory, file name are customizable.
     */
    fun composePackage(definition: PackageDefinition.() -> Unit): File {
        return PackageDefinition(this).compose(definition)
    }

    /**
     * Show asynchronous progress indicator with percentage while performing some action.
     */
    fun <T> progress(total: Int, action: ProgressIndicator.() -> T): T = progress(total.toLong(), action)

    /**
     * Show asynchronous progress indicator with percentage while performing some action.
     */
    fun <T> progress(total: Long, action: ProgressIndicator.() -> T): T {
        return ProgressIndicator(project).apply { this.total = total }.launch(action)
    }

    fun <T> progress(action: ProgressIndicator.() -> T) = progressIndicator(action)

    /**
     * Show asynchronous progress indicator while performing some action.
     */
    fun <T> progressIndicator(action: ProgressIndicator.() -> T): T = ProgressIndicator(project).launch(action)

    /**
     * Show synchronous progress logger while performing some action.
     */
    fun <T> progressLogger(action: ProgressLogger.() -> T): T = ProgressLogger.of(project).launch(action)

    /**
     * Show synchronous progress countdown / time to wait after performing asynchronous operation.
     */
    fun progressCountdown(time: Long) = progressCountdown { this.time = time }

    /**
     * Show synchronous progress countdown / time to wait after performing asynchronous operation.
     */
    fun progressCountdown(options: ProgressCountdown.() -> Unit) = ProgressCountdown(project).apply(options).run()

    @get:JsonIgnore
    val filter: FilterFile
        get() {
            val cmdFilterRoots = props.list("filter.roots") ?: listOf()
            if (cmdFilterRoots.isNotEmpty()) {
                logger.debug("Using Vault filter roots specified as command line property: $cmdFilterRoots")
                return FilterFile.temporary(project, cmdFilterRoots)
            }

            val cmdFilterPath = props.string("filter.path") ?: ""
            if (cmdFilterPath.isNotEmpty()) {
                val cmdFilter = FileOperations.find(project, packageOptions.vltDir.toString(), cmdFilterPath)
                        ?: throw VltException("Vault check out filter file does not exist at path: $cmdFilterPath" +
                                " (or under directory: ${packageOptions.vltDir}).")
                logger.debug("Using Vault filter file specified as command line property: $cmdFilterPath")
                return FilterFile(cmdFilter)
            }

            val conventionFilterFiles = listOf(
                    "${packageOptions.vltDir}/${FilterFile.SYNC_NAME}",
                    "${packageOptions.vltDir}/${FilterFile.BUILD_NAME}"
            )
            val conventionFilterFile = FileOperations.find(project, packageOptions.vltDir.toString(), conventionFilterFiles)
            if (conventionFilterFile != null) {
                logger.debug("Using Vault filter file found by convention: $conventionFilterFile")
                return FilterFile(conventionFilterFile)
            }

            logger.debug("None of Vault filter files found by CMD properties or convention.")

            return FilterFile.temporary(project, listOf())
        }

    /**
     * Get Vault filter object for specified file.
     */
    fun filter(file: File) = FilterFile(file)

    /**
     * Get Vault filter object for specified path.
     */
    fun filter(path: String) = filter(project.file(path))

    /**
     * Determine temporary directory for particular task.
     */
    fun temporaryDir(task: Task) = temporaryDir(task.name)

    /**
     * Determine temporary directory for particular service (any name).
     */
    fun temporaryDir(name: String) = AemTask.temporaryDir(project, name)

    /**
     * Determine temporary file for particular service (any name).
     */
    fun temporaryFile(name: String) = AemTask.temporaryFile(project, TEMPORARY_DIR, name)

    /**
     * Predefined temporary directory.
     */
    @get:JsonIgnore
    val temporaryDir: File
        get() = temporaryDir(TEMPORARY_DIR)

    /**
     * Factory method for configuration object determining how operation should be retried.
     */
    fun retry(configurer: Retry.() -> Unit): Retry {
        return retry().apply(configurer)
    }

    /**
     * Factory method for configuration object determining that operation should not be retried.
     */
    fun retry(): Retry = Retry.none(this)

    /**
     * React on file changes under configured directory.
     */
    fun fileWatcher(options: FileWatcher.() -> Unit) {
        FileWatcher(this).apply(options).start()
    }

    /**
     * Perform any HTTP requests to external endpoints.
     */
    fun <T> http(consumer: HttpClient.() -> T) = HttpClient(this).run(consumer)

    /**
     * Download files using HTTP protocol using custom settings.
     */
    fun <T> httpFile(consumer: HttpFileTransfer.() -> T) = fileTransfer.factory.http(consumer)

    /**
     * Transfer files using over SFTP protocol using custom settings.
     */
    fun <T> sftpFile(consumer: SftpFileTransfer.() -> T) = fileTransfer.factory.sftp(consumer)

    /**
     * Transfer files using over SMB protocol using custom settings.
     */
    fun <T> smbFile(consumer: SmbFileTransfer.() -> T) = fileTransfer.factory.smb(consumer)

    // Utilities (to use without imports)

    @JsonIgnore
    val parallel = Parallel

    @JsonIgnore
    val formats = Formats

    @JsonIgnore
    val buildScope = BuildScope.of(project)

    companion object {

        const val NAME = "aem"

        const val TEMPORARY_DIR = "tmp"

        private val PLUGIN_IDS = listOf(
                PackagePlugin.ID,
                BundlePlugin.ID,
                InstancePlugin.ID,
                EnvironmentPlugin.ID,
                ToolingPlugin.ID,
                CommonPlugin.ID
        )

        fun of(project: Project): AemExtension {
            return project.extensions.findByType(AemExtension::class.java)
                    ?: throw AemException("${project.displayName.capitalize()} must have at least one of following plugins applied: $PLUGIN_IDS")
        }
    }
}