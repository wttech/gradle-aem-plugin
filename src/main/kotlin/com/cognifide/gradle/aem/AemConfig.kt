package com.cognifide.gradle.aem

class AemConfig {

    var instances = mutableListOf<AemInstance>()

    fun instance(url: String, user: String = "admin", password: String = "admin", type: String = "default") {
        instances.add(AemInstance(url, user, password, type))
    }

    var deployConnectionTimeout = 5000

    /**
     * Force upload or install packages.
     */
    var deployForce = true

    var recursiveInstall = true

    var acHandling = "merge_preserve"

    var contentPath = "src/main/aem"

    var bundlePath = ""

    var fileIgnores = mutableListOf(
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
    )

    var fileExpands = mutableListOf(
            "**/filter.xml",
            "**/properties.xml"
    )

    var expandProperties = mutableMapOf<String, String>()

    var vaultCommonPath = "src/main/vault/common"

    var vaultProfilePath = "src/main/vault/profile"

    var localPackagePath = ""

    var remotePackagePath = ""

    var assemblyFilePattern = ""

}


