package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vlt.NodeTypesSync
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.io.Serializable

class PackageOptions(aem: AemExtension) : Serializable {

    /**
     * Package root directory containing 'jcr_root' and 'META-INF' directories.
     */
    var contentDir: File = aem.project.file("src/main/content")

    /**
     * JCR root directory.
     */
    @get:JsonIgnore
    val jcrRootDir: File
        get() = File(contentDir, Package.JCR_ROOT)

    /**
     * Vault metadata files directory (package definition).
     */
    @get:JsonIgnore
    val vltDir: File
        get() = File(contentDir, Package.VLT_PATH)

    /**
     * Custom path to Vault files that will be used to build CRX package.
     * Useful to share same files for all packages, like package thumbnail.
     */
    var metaCommonDir: File = File(aem.configCommonDir, Package.META_RESOURCES_PATH)

    /**
     * Content path for OSGi bundle jars being placed in CRX package.
     *
     * Default convention assumes that subprojects have separate bundle paths, because of potential re-installation of subpackages.
     * When all subprojects will have same bundle path, reinstalling one subpackage may end with deletion of other bundles coming from another subpackage.
     *
     * Beware that more nested bundle install directories are not supported by AEM by default (up to 4th depth level).
     * That's the reason of using dots in subproject names to avoid that limitation.
     */
    var installPath: String = if (aem.project == aem.project.rootProject) {
        "/apps/${aem.project.rootProject.name}/install"
    } else {
        "/apps/${aem.project.rootProject.name}/${aem.projectName}/install"
    }

    /**
     * Content path at which CRX Package Manager is storing uploaded packages.
     */
    var storagePath: String = "/etc/packages"

    /**
     * Calculate directory under storage path for each CRX package.
     */
    @get:JsonIgnore
    var storageDir: PackageFile.() -> String = { group }

    /**
     * Configures a local repository from which unreleased JARs could be added as 'compileOnly' dependency
     * and be deployed within CRX package deployment.
     */
    var installRepository: Boolean = true

    /**
     * Define patterns for known exceptions which could be thrown during package installation
     * making it impossible to succeed.
     *
     * When declared exception is encountered during package installation process, no more
     * retries will be applied.
     */
    var errors: List<String> = (aem.props.list("package.errors") ?: listOf(
            "javax.jcr.nodetype.*Exception",
            "org.apache.jackrabbit.oak.api.*Exception",
            "org.apache.jackrabbit.vault.packaging.*Exception",
            "org.xml.sax.*Exception"
    ))

    /**
     * CRX package name conventions (with wildcard) indicating that package can change over time
     * while having same version specified. Affects CRX packages composed and satisfied.
     */
    var snapshots: List<String> = aem.props.list("package.snapshots") ?: listOf()

    /**
     * Determines number of lines to process at once during reading Package Manager HTML responses.
     *
     * The higher the value, the bigger consumption of memory but shorter execution time.
     * It is a protection against exceeding max Java heap size.
     */
    var responseBuffer = aem.props.int("package.responseBuffer") ?: 4096

    /**
     * Customize default validation options.
     */
    fun validator(options: PackageValidator.() -> Unit) {
        this.validatorOptions = options
    }

    @get:JsonIgnore
    internal var validatorOptions: PackageValidator.() -> Unit = {}

    /**
     * Controls automatic node types exporting from available instance to be later used in package validation.
     */
    var nodeTypesSync = aem.props.string("package.nodeTypesSync")
            ?.let { NodeTypesSync.of(it) } ?: when {
                aem.offline -> NodeTypesSync.USE_FALLBACK
                else -> NodeTypesSync.WHEN_AVAILABLE
            }

    /**
     * Determines location on which synchronized node types will be saved.
     */
    var nodeTypesSyncFile = File(aem.configCommonDir, Package.NODE_TYPES_SYNC_PATH)
}
