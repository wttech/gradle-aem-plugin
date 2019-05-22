package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.pkg.Package
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

class PackageOptions(private val aem: AemExtension) : Serializable {

    /**
     * CRX package name conventions (with wildcard) indicating that package can change over time
     * while having same version specified. Affects CRX packages composed and satisfied.
     */
    @Internal
    var packageSnapshots: List<String> = aem.props.list("packageSnapshots") ?: listOf()

    @Input
    var packageRoot: String = "${aem.project.file("src/main/content")}"

    @get:Internal
    @get:JsonIgnore
    val packageJcrRoot: String
        get() = "$packageRoot/${Package.JCR_ROOT}"

    @get:Internal
    @get:JsonIgnore
    val packageVltRoot: String
        get() = "$packageRoot/${Package.VLT_PATH}"

    /**
     * Custom path to Vault files that will be used to build CRX package.
     * Useful to share same files for all packages, like package thumbnail.
     */
    @Input
    var packageMetaCommonRoot: String = "${aem.configCommonDir}/${Package.META_RESOURCES_PATH}"

    /**
     * Content path for OSGi bundle jars being placed in CRX package.
     *
     * Default convention assumes that subprojects have separate bundle paths, because of potential re-installation of subpackages.
     * When all subprojects will have same bundle path, reinstalling one subpackage may end with deletion of other bundles coming from another subpackage.
     *
     * Beware that more nested bundle install directories are not supported by AEM by default (up to 4th depth level).
     * That's the reason of using dots in subproject names to avoid that limitation.
     */
    @Input
    var packageInstallPath: String = if (aem.project == aem.project.rootProject) {
        "/apps/${aem.project.rootProject.name}/install"
    } else {
        "/apps/${aem.project.rootProject.name}/${aem.projectName}/install"
    }

    /**
     * Configures a local repository from which unreleased JARs could be added as 'compileOnly' dependency
     * and be deployed within CRX package deployment.
     */
    @Input
    var packageInstallRepository: Boolean = true

    /**
     * Define patterns for known exceptions which could be thrown during package installation
     * making it impossible to succeed.
     *
     * When declared exception is encountered during package installation process, no more
     * retries will be applied.
     */
    @Internal
    var packageErrors: List<String> = (aem.props.list("packageErrors") ?: listOf(
            "javax.jcr.nodetype.*Exception",
            "org.apache.jackrabbit.oak.api.*Exception",
            "org.apache.jackrabbit.vault.packaging.*Exception",
            "org.xml.sax.*Exception"
    ))

    /**
     * Determines number of lines to process at once during reading html responses.
     *
     * The higher the value, the bigger consumption of memory but shorter execution time.
     * It is a protection against exceeding max Java heap size.
     */
    @Internal
    var packageResponseBuffer = aem.props.int("packageResponseBuffer") ?: 4096
}