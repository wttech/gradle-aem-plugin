package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.pkg.ComposeTask
import org.gradle.api.Incubating
import org.gradle.api.Project
import java.io.Serializable

data class AemConfig(

    /**
     * List of AEM instances on which packages could be deployed.
     */
    var instances: MutableList<AemInstance> = mutableListOf(),

    /**
     * Defines maximum time after which initializing connection to AEM will be aborted (e.g on upload, install).
     */
    var deployConnectionTimeout: Int = 5000,

    /**
     * Perform deploy action (upload, install or activate) in parallel to multiple instances at once.
     */
    @Incubating
    var deployParallel: Boolean = false,

    /**
     * Force upload CRX package regardless if it was previously uploaded.
     */
    var uploadForce: Boolean = true,

    /**
     * Determines if when on package install, sub-packages included in CRX package content should be also installed.
     */
    var recursiveInstall: Boolean = true,

    /**
     * Defines behavior for access control handling included in rep:policy nodes being a part of CRX package content.
     *
     * @see <https://jackrabbit.apache.org/filevault/apidocs/org/apache/jackrabbit/vault/fs/io/AccessControlHandling.html>
     */
    var acHandling: String = "merge_preserve",

    /**
     * Relative to project, path to JCR content to be included in CRX package.
     */
    var contentPath: String = "src/main/content",

    /**
     * Content path to bundle jars being placed in CRX package.
     * Default: "/apps/${project.rootProject.name}/install".
     */
    var bundlePath: String = "",

    /**
     * Exclude files being a part of CRX package.
     */
    var contentFileIgnores: MutableList<String> = mutableListOf(
            "**/.git",
            "**/.git/**",
            "**/.gitattributes",
            "**/.gitignore",
            "**/.gitmodules",
            "**/.vlt",
            "**/package.json",
            "**/clientlibs/Gruntfile.js",
            "**/node_modules/**",
            "jcr_root/.vlt-sync-config.properties"
    ),

    /**
     * Ensures that for directory 'META-INF/vault' default files will be generated when missing: 'config.xml', 'filter.xml', 'properties.xml' and 'settings.xml'.
     */
    var vaultCopyMissingFiles : Boolean = true,

    /**
     * Define here custom properties that can be used in Vault files like 'properties.xml'.
     */
    var vaultExpandProperties: MutableMap<String, String> = mutableMapOf(),

    /**
     * Custom path to Vault files that will be used to build CRX package.
     * Useful to share same files for all packages, like package thumbnail.
     */
    var vaultFilesPath: String = "",

    /**
     * Wildcard file name filter expression that is used to filter in which files properties can be injected.
     */
    var vaultFilesExpanded: MutableList<String> = mutableListOf("*.xml"),

    /**
     * Points to Vault files for all package profiles.
     */
    var vaultCommonPath: String = "src/main/vault/common",

    /**
     * Points to Vault files from specific profile (e.g filters with only configuration to be installed).
     */
    var vaultProfilePath: String = "src/main/vault/profile",

    /**
     * Define here properties that will be skipped when pulling JCR content from AEM instance.
     */
    var vaultSkipProperties : MutableList<String> = mutableListOf(
            "jcr:lastModified",
            "jcr:created",
            "cq:lastModified",
            "cq:lastReplicat*",
            "jcr:uuid"
    ),

    /**
     * Filter file used when Vault files are being checked out from AEM instance.
     */
    var vaultFilterPath: String = "src/main/content/${AemPlugin.VLT_PATH}/filter.xml",

    /**
     * Extra parameters passed to VLT application while executing 'checkout' command.
     */
    var vaultCheckoutArgs : MutableList<String> = mutableListOf("--force"),

    /**
     * Specify characters to be used as line endings when cleaning up checked out JCR content.
     */
    var vaultLineSeparator : String = System.lineSeparator(),

    /**
     * Custom path to composed CRX package being uploaded.
     * Default: "${project.buildDir.path}/distributions/${project.name}-${project.version}.zip"
     */
    var localPackagePath: String = "",

    /**
     * Custom path to CRX package that is uploaded on AEM instance.
     * Default: [automatically determined]
     */
    var remotePackagePath: String = "",

    /**
     * Controls support of SCR Annotations which are used often in AEM development in Java sources.
     *
     * @see <http://felix.apache.org/documentation/subprojects/apache-felix-service-component-runtime.html>
     * @see <http://felix.apache.org/documentation/subprojects/apache-felix-maven-scr-plugin/apache-felix-maven-scr-plugin-use.html>
     */
    var scrEnabled: Boolean = true,

    /**
     * Treat SCR warnings as errors
     */
    var scrStrictMode: Boolean = false,

    /**
     * Scan generated classes directory instead of sources directory
     */
    var scrScanClasses: Boolean = true,

    /**
     * Exclude source files being processed by SCR annotations scanner.
     */
    var scrExcludes: String = "",

    /**
     * Include source files being processed by SCR annotations scanner.
     */
    var scrIncludes: String = "",

    /**
     * Force specific declarative services version.
     */
    var scrSpecVersion: String = ""

) : Serializable {
    companion object {

        /**
         * Generally it is recommended to configure only extension config instead of config of concrete task, because
         * there are combined tasks like `aemDeploy` which are using multiple properties at once.
         *
         * Copying properties and considering them as separate config is intentional, just to ensure that specific task
         * configuration does not affect another.
         */
        fun extend(project: Project): AemConfig {
            val global = (project.extensions.getByName(AemExtension.NAME) as AemExtension).config
            val extended = global.copy()

            applyProjectDefaults(extended, project)

            return extended
        }

        private fun applyProjectDefaults(config: AemConfig, project: Project) {
            config.bundlePath = "/apps/" + project.rootProject.name + "/install"
        }
    }

    fun instance(url: String, user: String = "admin", password: String = "admin", type: String = "default") {
        instances.add(AemInstance(url, user, password, type))
    }

    /**
     * While including another project content, use path configured in that project.
     */
    fun determineContentPath(project: Project): String {
        val task = project.tasks.getByName(ComposeTask.NAME) as ComposeTask

        return project.projectDir.path + "/" + task.config.contentPath
    }

    /**
     * Following checks will be performed during configuration phase
     */
    fun validate() {
        if (bundlePath.isBlank()) {
            throw AemException("Bundle path cannot be blank")
        }

        if (contentPath.isBlank()) {
            throw AemException("Content path cannot be blank")
        }

        instances.forEach { it.validate() }
    }

}


