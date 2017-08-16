package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.LocalInstance
import com.cognifide.gradle.aem.instance.RemoteInstance
import com.cognifide.gradle.aem.pkg.ComposeTask
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.DefaultTask
import org.gradle.api.Incubating
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File
import java.io.Serializable
import java.util.*

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
        var instances: MutableList<Instance> = mutableListOf(),

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
         */
        @Input
        var filesExpanded: MutableList<String> = mutableListOf("**/${AemPackagePlugin.VLT_PATH}/*.xml"),

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
         *
         * Default: "${project.rootProject.projectDir.path}/src/main/resources/META-INF/vault"
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
        ),

        /**
         * Build date used as base for calculating 'created' and 'buildCount' package properties.
         */
        @Internal
        var buildDate: Date = Date(),

        /**
         * Path in which local AEM instances will be stored.
         *
         * Default: "${System.getProperty("user.home")}/.gradle/aem/${project.rootProject.name}"
         */
        @Input
        var instancesPath: String = "",

        /**
         * Path from which extra files for local AEM instances will be copied.
         * Useful for overriding default startup scripts ('start.bat' or 'start.sh') or providing some files inside 'crx-quickstart'.
         *
         * Default: "{rootProject}/src/main/resources/local-instance"
         */
        @Input
        var instanceFilesPath: String = "",

        /**
         * Wildcard file name filter expression that is used to filter in which instance files properties can be injected.
         */
        @Input
        var instanceFilesExpanded: MutableList<String> = mutableListOf("**/*.properties", "**/*.sh", "**/*.bat", "**/*.xml"),

        /**
         * Time in milliseconds to postpone instance stability checks to avoid race condition related with
         * actual operation being performed on AEM like starting JCR package installation or even creating launchpad.
         */
        @Input
        var instanceAwaitDelay: Int = 3000,

        /**
         * Time in milliseconds used as interval between next instance stability checks being performed.
         * Optimization could be necessary only when instance is heavily loaded.
         */
        @Input
        var instanceAwaitInterval: Int = 1000,

        /**
         * Time in milliseconds used as maximum after which instance stability checks will be skipped.
         */
        @Input
        var instanceAwaitTimeout: Int = 1000 * 60 * 15

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

        fun archiveName(project: Project): String {
            return (project.tasks.getByName(ComposeTask.NAME) as ComposeTask).archiveName
        }

    }

    /**
     * Initialize defaults that depends on concrete type of project.
     */
    fun configure(project: Project) {
        if (project == project.rootProject) {
            bundlePath = "/apps/${project.name}/install"
        } else {
            bundlePath = "/apps/${project.rootProject.name}/${project.name}/install"
        }

        contentPath = "${project.projectDir.path}/src/main/content"
        vaultFilesPath = "${project.rootProject.projectDir.path}/src/main/resources/${AemPackagePlugin.VLT_PATH}"
        instancesPath = "${System.getProperty("user.home")}/.gradle/aem/${project.rootProject.name}"
        instanceFilesPath = "${project.rootProject.projectDir.path}/src/main/resources/${AemInstancePlugin.FILES_PATH}"

        project.afterEvaluate { validate() }
    }

    /**
     * Declare new deployment target (AEM instance).
     */

    fun localInstance(httpUrl: String) {
        instances.add(LocalInstance(httpUrl))
    }

    fun localInstance(httpUrl: String, user: String, password: String) {
        instances.add(LocalInstance(httpUrl, user, password))
    }

    fun localInstance(httpUrl: String, user: String, password: String, type: String, debugPort: Int) {
        instances.add(LocalInstance(httpUrl, user, password, type, debugPort))
    }

    fun remoteInstance(httpUrl: String, environment: String) {
        instances.add(RemoteInstance(httpUrl, environment))
    }

    fun remoteInstance(httpUrl: String, user: String, password: String, type: String, environment: String) {
        instances.add(RemoteInstance(httpUrl, user, password, type, environment))
    }

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
    @get:JsonIgnore
    val vaultFilesDirs: List<File>
        get() {
            val paths = listOf(
                    vaultFilesPath,
                    "$contentPath/${AemPackagePlugin.VLT_PATH}"
            )

            return paths.filter { !it.isNullOrBlank() }.map { File(it) }
        }

    @get:Internal
    @get:JsonIgnore
    val instancesByName: Map<String, Instance>
        get() = instances.fold(mutableMapOf<String, Instance>(), { map, instance ->
            map.put(instance.name, instance); map
        })

    /**
     * CRX package Vault filter path.
     * Also used by VLT tool as default filter for files being checked out from running AEM instance.
     *
     * @see <http://jackrabbit.apache.org/filevault/filter.html>
     */
    @get:Internal
    val vaultFilterPath: String
        get() = "$contentPath/${AemPackagePlugin.VLT_PATH}/filter.xml"

}