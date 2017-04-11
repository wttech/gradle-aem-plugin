package com.cognifide.gradle.aem

class AemConfig {

    val instances = mutableListOf(
            AemInstance("http://localhost:4502", "admin", "admin", "author"),
            AemInstance("http://localhost:4503", "admin", "admin", "publish")
    )

    fun instance(url: String, user: String = "admin", password: String = "admin", type: String = "default") {
        instances.add(AemInstance(url, user, password, type))
    }

    var connectionTimeout = 5000

    /**
     * Force upload or install packages.
     */
    var deployForce = true

    var recursiveInstall = true

    var acHandling = "merge_preserve"

    var contentPath = "src/main/aem"

    var bundlePath = ""

    val fileIgnores = mutableListOf(
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

    val fileExpands = mutableListOf(
            "**/filter.xml",
            "**/properties.xml"
    )

    val expandProperties = mutableMapOf<String, String>()

    var vaultCommonPath = "src/main/vault/common"

    var vaultProfilePath = "src/main/vault/profile"

}


