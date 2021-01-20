package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.bundle.tasks.BundleJar
import com.cognifide.gradle.aem.bundle.tasks.bundle
import com.cognifide.gradle.aem.common.CommonOptions
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.common.asset.AssetManager
import com.cognifide.gradle.aem.common.instance.*
import com.cognifide.gradle.aem.common.instance.service.groovy.GroovyEvaluator
import com.cognifide.gradle.aem.common.instance.service.groovy.GroovyEvalSummary
import com.cognifide.gradle.aem.common.pkg.PackageDefinition
import com.cognifide.gradle.aem.common.pkg.PackageFile
import com.cognifide.gradle.aem.common.pkg.PackageOptions
import com.cognifide.gradle.aem.common.pkg.PackageValidator
import com.cognifide.gradle.aem.common.pkg.vault.FilterFile
import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import com.cognifide.gradle.aem.common.instance.rcp.RcpClient
import com.cognifide.gradle.aem.common.pkg.vault.VaultClient
import com.cognifide.gradle.aem.common.pkg.vault.VaultSummary
import com.cognifide.gradle.aem.common.utils.ProcessKiller
import com.cognifide.gradle.aem.common.utils.WebBrowser
import com.cognifide.gradle.aem.pkg.PackageSyncPlugin
import com.cognifide.gradle.common.CommonExtension
import com.cognifide.gradle.common.common
import com.cognifide.gradle.common.pluginProjects
import com.cognifide.gradle.common.utils.Patterns
import java.io.File
import java.io.Serializable
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar

/**
 * Core of library, facade for implementing tasks.
 */
@Suppress("TooManyFunctions")
class AemExtension(val project: Project) : Serializable {

    // Shorthands

    val common = CommonExtension.of(project)

    val logger = common.logger

    val prop = common.prop

    val obj = common.obj

    // ===

    val commonOptions by lazy { CommonOptions(this) }

    val assetManager by lazy { AssetManager(this) }

    val webBrowser by lazy { WebBrowser(this) }

    val processKiller by lazy { ProcessKiller(this) }

    /**
     * Defines common settings like environment name, line endings when generating files etc
     */
    fun common(options: CommonOptions.() -> Unit) {
        commonOptions.apply(options)
    }

    val packageOptions by lazy { PackageOptions(this) }

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

    /**
     * Defines instances to work with.
     */
    fun instance(options: InstanceManager.() -> Unit) {
        instanceManager.apply(options)
    }

    val instanceManager by lazy { InstanceManager(this) }

    /**
     * Define common settings valid only for instances created at local file system.
     */
    fun localInstance(options: LocalInstanceManager.() -> Unit) = localInstanceManager.apply(options)

    val localInstanceManager by lazy { LocalInstanceManager(this) }

    /**
     * Collection of all java packages from all projects applying bundle plugin.
     *
     * Use with caution as of this property is eagerly configuring all tasks building bundles.
     */
    val bundlesBuilt: List<Jar> get() = project.pluginProjects(BundlePlugin.ID)
            .flatMap { p -> p.common.tasks.getAll<Jar>() }
            .filter { jar -> jar.convention.plugins.containsKey(BundlePlugin.CONVENTION_PLUGIN) }

    /**
     * Collection of Vault definitions from all packages from all projects applying package plugin.
     *
     * Use with caution as of this property is eagerly configuring all tasks building packages.
     */
    val packagesBuilt: List<PackageCompose> get() = project.pluginProjects(PackagePlugin.ID)
            .flatMap { p -> p.common.tasks.getAll<PackageCompose>() }

    /**
     * Java package of built bundle (if project is applying bundle plugin).
     */
    val javaPackage: String? get() = when {
        project.plugins.hasPlugin(BundlePlugin.ID) -> common.tasks.get<Jar>(JavaPlugin.JAR_TASK_NAME).bundle.javaPackage.orNull
        else -> null
    }

    /**
     * Collection of all java packages from all projects applying bundle plugin.
     */
    val javaPackages: List<String> get() = bundlesBuilt.mapNotNull { it.bundle.javaPackage.orNull }

    /**
     * All instances matching default filtering.
     *
     * @see <https://github.com/Cognifide/gradle-aem-plugin#filter-instances-to-work-with>
     */
    val instances get() = filterInstances()

    /**
     * Work in parallel with instances matching default filtering.
     */
    fun instances(consumer: (Instance) -> Unit) = common.parallel.with(instances, consumer)

    /**
     * Work in parallel with instances which name is matching specified wildcard filter.
     */
    fun instances(filter: String, consumer: (Instance) -> Unit) = common.parallel.with(filterInstances(filter), consumer)

    /**
     * Shorthand method for getting defined instance or creating temporary instance by URL.
     *
     * Note that this method intentionally allows to read details of instance which is even not enabled.
     */
    fun instance(urlOrName: String): Instance = instanceManager.find(urlOrName) ?: instanceManager.parse(urlOrName)

    /**
     * Shorthand method for getting defined instances or creating temporary instances by URLs.
     */
    fun instances(urlsOrNames: Iterable<String>): List<Instance> = urlsOrNames.map { instance(it) }

    /**
     * Get instance from command line parameter named 'instance' which holds instance name or URL.
     * If it is not specified, then first instance matching default filtering fill be returned.
     *
     * Purpose of this method is to easily get any instance to work with (no matter how it will be defined).
     *
     * @see <https://github.com/Cognifide/gradle-aem-plugin#filter-instances-to-work-with>
     */
    val anyInstance: Instance
        get() {
            val cmdInstanceArg = prop.string("instance")
            if (!cmdInstanceArg.isNullOrBlank()) {
                return instance(cmdInstanceArg)
            }

            return findInstance(Instance.FILTER_ANY) ?: Instance.defaultAuthor(this)
        }

    /**
     * Get available instance of any type (most often first defined).
     */
    val availableInstance: Instance? get() = instances.asSequence().firstOrNull { it.available }

    /**
     * Find instance which name is matching wildcard filter specified via command line parameter 'instance.name'.
     * By default, this method respects current environment which is used to work only with instances running locally.
     */
    fun findInstance(desiredName: String? = prop.string("instance.name"), defaultName: String = "${commonOptions.env.get()}-*"): Instance? {
        return filterInstances(desiredName ?: defaultName).firstOrNull()
    }

    /**
     * Get instance which name is matching wildcard filter specified via command line parameter 'instance.name'.
     * By default, this method respects current environment which is used to work only with instances running locally.
     *
     * If instance not found, throws exception.
     */
    fun namedInstance(desiredName: String? = prop.string("instance.name"), defaultName: String = "${commonOptions.env.get()}-*"): Instance {
        return findInstance(desiredName, defaultName)
                ?: throw AemException("Instance named '${desiredName ?: defaultName}' is not defined.")
    }

    /**
     * Find all instances which names are matching wildcard filter specified via command line parameter 'instance.name'.
     */
    fun filterInstances(nameMatcher: String = prop.string("instance.name") ?: "${commonOptions.env.get()}-*"): List<Instance> {
        val all = instanceManager.defined.get().filter { it.enabled }

        // Specified by command line should not be filtered
        val cmd = all.filter { it.env == Instance.ENV_CMD }
        if (cmd.isNotEmpty()) {
            return cmd
        }

        // Defined by build script, via properties or defaults are filterable by name
        return all.filter { instance ->
            when {
                prop.flag("instance.author", "instance.authors") -> instance.author
                prop.flag("instance.publish", "instance.publishes", "instance.publishers") -> instance.publish
                else -> Patterns.wildcard(instance.name, nameMatcher)
            }
        }
    }

    /**
     * Get all author instances running on current environment.
     */
    val authorInstances: List<Instance> get() = filterInstances().filter { it.author }

    val authorInstance: Instance get() = authorInstances.firstOrNull()
            ?: throw AemException("No author instances defined!")

    /**
     * Work in parallel with all author instances running on current environment.
     */
    fun authorInstances(consumer: (Instance) -> Unit) = common.parallel.with(authorInstances, consumer)

    /**
     * Get all publish instances running on current environment.
     */
    val publishInstances: List<Instance> get() = filterInstances().filter { it.publish }

    val publishInstance: Instance get() = publishInstances.firstOrNull()
            ?: throw AemException("No publish instances defined!")

    /**
     * Work in parallel with all publish instances running on current environment.
     */
    fun publishInstances(consumer: Instance.() -> Unit) = common.parallel.with(publishInstances, consumer)

    /**
     * Get all local instances.
     */
    val localInstances: List<LocalInstance> get() = instances.filterIsInstance(LocalInstance::class.java)

    /**
     * Work in parallel with all local instances.
     */
    fun localInstances(consumer: LocalInstance.() -> Unit) = common.parallel.with(localInstances, consumer)

    /**
     * Get all remote instances.
     */
    val remoteInstances: List<Instance> get() = instances - localInstances

    /**
     * Work in parallel with all remote instances.
     */
    fun remoteInstances(consumer: Instance.() -> Unit) = common.parallel.with(remoteInstances, consumer)

    /**
     * Get CRX package defined to be built (could not yet exist).
     */
    @Suppress("VariableNaming")
    val `package`: File get() = common.tasks.get<PackageCompose>(PackageCompose.NAME).archiveFile.get().asFile

    val pkg: File get() = `package`

    /**
     * Get all CRX packages defined to be built.
     */
    val packages: List<File> get() = common.tasks.getAll<PackageCompose>().map { it.archiveFile.get().asFile }

    /**
     * Get OSGi bundle defined to be built (could not yet exist).
     */
    val bundle: File get() = common.tasks.get<Jar>(JavaPlugin.JAR_TASK_NAME).archiveFile.get().asFile

    /**
     * Get all OSGi bundles defined to be built.
     */
    val bundles: List<File> get() = common.tasks.getAll<Jar>().map { it.archiveFile.get().asFile }

    /**
     * Shorthand for embedding code inside OSGi bundle being composed when configuration on demand is enabled.
     * When this feature is not enabled, it is preferred to use [BundleJar.embedPackage].
     */
    fun bundleEmbed(dependencyNotation: Any, pkgs: Iterable<String>, export: Boolean = false) {
        project.dependencies.add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, dependencyNotation)
        common.tasks.named<Jar>(JavaPlugin.JAR_TASK_NAME) {
            bundle {
                when {
                    export -> exportPackage(pkgs)
                    else -> privatePackage(pkgs)
                }
            }
        }
    }

    /**
     * Shorthand for embedding code inside OSGi bundle being composed when configuration on demand is enabled.
     * When this feature is not enabled, it is preferred to use [BundleJar.embedPackage].
     */
    fun bundleEmbed(dependencyNotation: Any, vararg pkgs: String, export: Boolean = false) = bundleEmbed(dependencyNotation, pkgs.asIterable(), export)

    /**
     * In parallel, work with services of all instances matching default filtering.
     */
    fun sync(action: InstanceSync.() -> Unit) = sync(instances, action)

    /**
     * In parallel, work with services of all instances matching default filtering.
     */
    fun syncInstances(action: InstanceSync.() -> Unit) = sync(action)

    /**
     * Work with instance services of specified instance.
     */
    fun <T> sync(instance: Instance, action: InstanceSync.() -> T) = instance.sync(action)

    /**
     * Work with instance services of specified instance.
     */
    fun <T> syncInstance(instance: Instance, action: InstanceSync.() -> T) = sync(instance, action)

    /**
     * In parallel, work with services of all specified instances.
     */
    fun sync(instances: Iterable<Instance>, action: InstanceSync.() -> Unit) {
        common.parallel.with(instances) { this.sync.apply(action) }
    }

    /**
     * In parallel, work with services of all specified instances.
     */
    fun syncInstances(instances: Iterable<Instance>, action: InstanceSync.() -> Unit) = sync(instances, action)

    /**
     * In parallel, work with built packages and services of instances matching default filtering.
     */
    fun syncPackages(action: InstanceSync.(File) -> Unit) = syncFiles(instances, packages, action)

    /**
     * In parallel, work with built OSGi bundles and services of instances matching default filtering.
     */
    fun syncBundles(action: InstanceSync.(File) -> Unit) = syncFiles(instances, bundles, action)

    /**
     * In parallel, work with built packages and services of specified instances.
     */
    fun syncFiles(instances: Iterable<Instance>, packages: Iterable<File>, action: InstanceSync.(File) -> Unit) {
        packages.forEach { pkg -> // single AEM instance dislikes parallel CRX package / OSGi bundle installation
            common.parallel.with(instances) { // but same file could be in parallel deployed on different AEM instances
                sync.apply { action(pkg) }
            }
        }
    }

    /**
     * Build minimal CRX package in-place / only via code.
     * All details like Vault properties, archive destination directory, file name are customizable.
     */
    fun composePackage(definition: PackageDefinition.() -> Unit): File = PackageDefinition(this).apply(definition).compose()

    /**
     * Validate any CRX packages.
     */
    fun validatePackage(vararg packages: File, options: PackageValidator.() -> Unit = {}) = validatePackage(packages.asIterable(), options)

    /**
     * Validate any CRX packages.
     */
    fun validatePackage(packages: Iterable<File>, options: PackageValidator.() -> Unit = {}) = PackageValidator(this).apply(options).perform(packages)

    /**
     * Vault filter determined by convention and properties.
     */
    val filter: FilterFile get() = FilterFile.default(this)

    /**
     * Get Vault filter object for specified file.
     */
    fun filter(file: File) = FilterFile(file)

    /**
     * Get Vault filter object for specified path.
     */
    fun filter(path: String) = filter(project.file(path))

    /**
     * Execute any Vault command.
     */
    fun vlt(command: String): VaultSummary = vlt { this.command.set(command); run() }

    /**
     * Execute any Vault command with customized options like content directory.
     */
    fun <T> vlt(options: VaultClient.() -> T) = VaultClient(this).run(options)

    /**
     * Execute any Vault JCR content remote copying with customized options like content directory.
     */
    fun <T> rcp(options: RcpClient.() -> T) = RcpClient(this).run(options)

    /**
     * Execute Groovy script(s) using specified options.
     */
    fun <T> groovyEval(options: GroovyEvaluator.() -> T) = GroovyEvaluator(this).run(options)

    /**
     * Execute Groovy script(s) matching file pattern on AEM instances.
     */
    fun groovyEval(scriptPattern: String): GroovyEvalSummary = groovyEval { this.scriptPattern.set(scriptPattern); eval() }

    companion object {

        const val NAME = "aem"

        private val PLUGIN_IDS = listOf(
                CommonPlugin.ID,
                PackagePlugin.ID,
                PackageSyncPlugin.ID,
                BundlePlugin.ID,
                InstancePlugin.ID,
                LocalInstancePlugin.ID
        )

        fun of(project: Project): AemExtension {
            return project.extensions.findByType(AemExtension::class.java)
                    ?: throw AemException("${project.displayName.capitalize()} must have at least one of following plugins applied: $PLUGIN_IDS")
        }
    }
}

val Project.aem get() = AemExtension.of(this)
