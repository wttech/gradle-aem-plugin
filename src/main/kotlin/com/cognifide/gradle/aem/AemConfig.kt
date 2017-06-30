package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.pkg.ComposeTask
import org.gradle.api.DefaultTask
import org.gradle.api.Incubating
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File
import java.io.Serializable

/**
 * Aggregated collection of AEM related configuration.
 *
 * Notice that this whole object is serializable and marked as input of tasks, so there is no need to mark
 * each property as input.
 *
 * What is more, content paths which are used to compose a CRX package are being processed by copy task,
 * which automatically mark them as inputs so package is being rebuild on any content change.
 */
data class AemConfig(

    /**
     * List of AEM instances on which packages could be deployed.
     */
    @Input
    var instances: MutableList<AemInstance> = mutableListOf(),

    /**
     * Defines maximum time after which initializing connection to AEM will be aborted (e.g on upload, install).
     */
    @Input
    var deployConnectionTimeout: Int = 5000,

    /**
     * Perform deploy action (upload, install or activate) in parallel to multiple instances at once.
     */
    @Incubating
    @Input
    var deployParallel: Boolean = false,

    /**
     * Force upload CRX package regardless if it was previously uploaded.
     */
    @Input
    var uploadForce: Boolean = true,

    /**
     * Determines if when on package install, sub-packages included in CRX package content should be also installed.
     */
    @Input
    var recursiveInstall: Boolean = true,

    /**
     * Defines behavior for access control handling included in rep:policy nodes being a part of CRX package content.
     *
     * @see <https://jackrabbit.apache.org/filevault/apidocs/org/apache/jackrabbit/vault/fs/io/AccessControlHandling.html>
     */
    @Input
    var acHandling: String = "merge_preserve",

    /**
     * Absolute path to JCR content to be included in CRX package.
     * Must be absolute or relative to current working directory.
     *
     * Default: "${project.projectDir.path}/src/main/content"
     */
    @Input
    var contentPath: String = "",

    /**
     * Content path for bundle jars being placed in CRX package.
     * Default: "/apps/${project.rootProject.name}/install".
     */
    @Input
    var bundlePath: String = "",

    /**
     * Custom path to composed CRX package being uploaded.
     *
     * Default: [automatically determined]
     */
    @Input
    var localPackagePath: String = "",

    /**
     * Custom path to CRX package that is uploaded on AEM instance.
     *
     * Default: [automatically determined]
     */
    @Input
    var remotePackagePath: String = "",

    /**
     * Exclude files being a part of CRX package.
     */
    @Input
    var filesExcluded: MutableList<String> = mutableListOf(
            "**/.gradle",
            "**/.git",
            "**/.git/**",
            "**/.gitattributes",
            "**/.gitignore",
            "**/.gitmodules",
            "**/.vlt",
            "**/node_modules/**",
            "jcr_root/.vlt-sync-config.properties"
    ),

    /**
     * Wildcard file name filter expression that is used to filter in which Vault files properties can be injected.
     * This also could be done 'by fileFilter', but due to performance optimization it is done separately.
     */
    @Input
    var filesExpanded: MutableList<String> = mutableListOf("**/${AemPlugin.VLT_PATH}/*.xml"),

    /**
     * Define here custom properties that can be used in CRX package files like 'META-INF/vault/properties.xml'.
     * Could override predefined properties provided by plugin itself.
     */
    @Input
    var fileProperties: MutableMap<String, Any> = mutableMapOf(
            "requiresRoot" to "false"
    ),

    /**
     * Ensures that for directory 'META-INF/vault' default files will be generated when missing:
     * 'config.xml', 'filter.xml', 'properties.xml' and 'settings.xml'.
     */
    @Input
    var vaultCopyMissingFiles: Boolean = true,

    /**
     * Custom path to Vault files that will be used to build CRX package.
     * Useful to share same files for all packages, like package thumbnail.
     * Must be absolute or relative to current working directory.
     */
    @Input
    var vaultFilesPath: String = "",

    /**
     * Define here properties that will be skipped when pulling JCR content from AEM instance.
     */
    @Input
    var vaultSkipProperties: MutableList<String> = mutableListOf(
            "jcr:lastModified",
            "jcr:created",
            "cq:lastModified",
            "cq:lastReplicat*",
            "jcr:uuid"
    ),

    /**
     * Filter file used when Vault files are being checked out from running AEM instance.
     *
     * Default: "src/main/content/META-INF/vault/filter.xml"
     */
    @Input
    var vaultFilterPath: String = "",

    /**
     * Global options which are being applied to any Vault related command like 'aemVault' or 'aemCheckout'.
     */
    @Input
    var vaultGlobalOptions: String = "--credentials \${instance.credentials}",

    /**
     * Specify characters to be used as line endings when cleaning up checked out JCR content.
     */
    @Input
    var vaultLineSeparator: String = System.lineSeparator(),

    /**
     * Configure default task dependency assignments while including dependant project bundles.
     * Simplifies multi-module project configuration.
     */
    @Input
    var dependBundlesTaskNames: List<String> = mutableListOf(
            LifecycleBasePlugin.ASSEMBLE_TASK_NAME,
            LifecycleBasePlugin.CHECK_TASK_NAME
    ),

    /**
     * Configure default task dependency assignments while including dependant project content.
     * Simplifies multi-module project configuration.
     */
    @Input
    var dependContentTaskNames: List<String> = mutableListOf(
            ComposeTask.NAME + ComposeTask.DEPENDENCIES_SUFFIX
    )

) : Serializable {
    companion object {

        /**
         * Shorthand getter for configuration related with specified project.
         * Especially useful when including one project in another (composing assembly packages).
         */
        fun of(project: Project): AemConfig {
            return project.extensions.getByType(AemExtension::class.java).config
        }

        fun of(task: DefaultTask): AemConfig {
            return of(task.project)
        }

    }

    /**
     * Initialize defaults that depends on concrete type of project.
     */
    fun configure(task: DefaultTask) {
        val project = task.project

        if (project == project.rootProject) {
            bundlePath = "/apps/${project.name}/install"
        } else {
            bundlePath = "/apps/${project.rootProject.name}/${project.name}/install"
        }

        contentPath =  "${project.projectDir.path}/src/main/content"
        vaultFilesPath ="${project.rootProject.projectDir.path}/src/main/resources/${AemPlugin.VLT_PATH}"
        vaultFilterPath = "${project.projectDir.path}/src/main/content/${AemPlugin.VLT_PATH}/filter.xml"
    }

    fun attach(task: DefaultTask) {
        val inputs = task.inputs

        vaultFilesDirs.forEach { inputs.dir(it) }
    }

    /**
     * Declare new deployment target (AEM instance).
     */
    fun instance(url: String, user: String = "admin", password: String = "admin", type: String = "default") {
        instances.add(AemInstance(url, user, password, type))
    }

    @get:Internal
    val instancesByName: Map<String, AemInstance>
        get() = instances.fold(mutableMapOf<String, AemInstance>(), { map, instance ->
            map.put(instance.name, instance); map
        })

    /**
     * Following checks will be performed during configuration phase.
     */
    fun validate() {
        if (bundlePath.isBlank()) {
            throw AemException("Bundle path cannot be blank")
        }

        if (contentPath.isBlank()) {
            throw AemException("Content path cannot be blank")
        }

        if (instances.size != instancesByName.size) {
            throw AemException("Instance names must be unique")
        }

        instances.forEach { it.validate() }
    }

    /**
     * CRX package Vault files will be composed from given sources.
     * Missing files required by package within installation will be auto-generated if 'vaultCopyMissingFiles' is enabled.
     */
    @get:Internal
    val vaultFilesDirs: List<File>
        get() {
            val paths = listOf(
                    vaultFilesPath,
                    "$contentPath/${AemPlugin.VLT_PATH}"
            )

            return paths.filter { !it.isNullOrBlank() }.map { File(it) }
        }

}