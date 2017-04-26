package com.cognifide.gradle.aem

import org.gradle.api.Project
import java.io.Serializable

data class AemConfig(

    /**
     * List of AEM instances on which packages could be deployed.
     */
    var instances: MutableList<AemInstance> = mutableListOf<AemInstance>(),

    /**
     * Defines maximum time after which initializing connection to AEM will be aborted (e.g on upload, install).
     */
    var deployConnectionTimeout: Int = 5000,

    /**
     * Force upload CRX package regardless if it was previously uploaded.
     */
    var deployForce: Boolean = true,

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
            "jcr_root/.vlt-sync-config.properties",
            "jcr_root/var/**",
            "SLING-INF/**"
    ),

    /**
     * Define here custom properties that can be used in Vault files like 'properties.xml'.
     */
    var vaultProperties: MutableMap<String, String> = mutableMapOf<String, String>(),

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
        fun extendFromGlobal(project: Project): AemConfig {
            val global = (project.extensions.getByName(AemExtension.NAME) as AemExtension).config
            val extended = global.copy()

            applyProjectDefaults(extended, project)

            return extended
        }

        private fun applyProjectDefaults(config: AemConfig, project: Project) {
            if (config.bundlePath.isNullOrBlank()) {
                config.bundlePath = "/apps/" + project.rootProject.name + "/install"
            }
        }
    }

    fun instance(url: String, user: String = "admin", password: String = "admin", type: String = "default") {
        instances.add(AemInstance(url, user, password, type))
    }

}


