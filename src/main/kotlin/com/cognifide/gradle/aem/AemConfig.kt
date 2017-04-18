package com.cognifide.gradle.aem

import org.gradle.api.Project
import java.io.Serializable

data class AemConfig(

        var instances: MutableList<AemInstance> = mutableListOf<AemInstance>(),

        var deployConnectionTimeout: Int = 5000,

        var deployForce: Boolean = true,

        var recursiveInstall: Boolean = true,

        var acHandling: String = "merge_preserve",

        var contentPath: String = "src/main/content",

        var bundlePath: String = "",

        var fileIgnores: MutableList<String> = mutableListOf(
                "**/.git",
                "**/.git/**",
                "**/.gitattributes",
                "**/.gitignore",
                "**/.gitmodules", "**/.vlt",
                "**/package.json",
                "**/clientlibs/Gruntfile.js",
                "**/node_modules/**",
                "jcr_root/.vlt-sync-config.properties",
                "jcr_root/var/**",
                "SLING-INF/**"
        ),

        var vaultProperties: MutableMap<String, String> = mutableMapOf<String, String>(),

        var vaultFilesPath : String = "",

        var vaultFilesExpanded : MutableList<String> = mutableListOf("*.xml"),

        var vaultCommonPath: String = "src/main/vault/common",

        var vaultProfilePath: String = "src/main/vault/profile",

        var localPackagePath: String = "",

        var remotePackagePath: String = ""

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

            if (!config.vaultProperties.contains("assembly.name")) {
                config.vaultProperties.put("assembly.name", project.rootProject.name)
            }
        }
    }

    fun instance(url: String, user: String = "admin", password: String = "admin", type: String = "default") {
        instances.add(AemInstance(url, user, password, type))
    }

}


