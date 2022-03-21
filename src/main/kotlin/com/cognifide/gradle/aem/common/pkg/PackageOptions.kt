package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.common.utils.using
import java.io.Serializable

class PackageOptions(private val aem: AemExtension) : Serializable {

    /**
     * Package specific configuration
     */
    val commonDir = aem.obj.projectDir(aem.prop.string("package.commonPath") ?: "src/aem/package")

    /**
     * Package root directory containing 'jcr_root' and 'META-INF' directories.
     */
    val contentDir = aem.obj.projectDir(aem.prop.string("package.contentPath") ?: "src/main/content")

    /**
     * JCR root directory.
     */
    val jcrRootDir = aem.obj.relativeDir(contentDir, Package.JCR_ROOT)

    /**
     * Vault metadata files directory (package definition).
     */
    val vltDir = aem.obj.relativeDir(contentDir, Package.VLT_PATH)

    /**
     * Custom path to Vault files that will be used to build CRX package.
     * Useful to share same files for all packages, like package thumbnail.
     */
    val metaCommonDir = aem.obj.relativeDir(commonDir, "defaults/${Package.META_PATH}")

    /**
     * Content path for AEM application placed in CRX package.
     */
    val appPath = aem.obj.string {
        convention(
            aem.obj.provider {
                when (aem.project) {
                    aem.project.rootProject -> "/apps/${aem.project.rootProject.name}"
                    else -> "/apps/${aem.project.rootProject.name}/${aem.project.name}"
                }
            }
        )
        aem.prop.string("package.appPath")?.let { set(it) }
    }

    /**
     * Repository path for OSGi bundles (JAR files) placed in CRX package.
     */
    val installPath = aem.obj.string { convention(appPath.map { "$it/install" }) }

    /**
     * Source directory for OSGi bundles (JAR files) placed in CRX package.
     */
    val installDir = aem.obj.dir { convention(jcrRootDir.dir(installPath.map { it.removePrefix("/") })) }

    /**
     * Repository path for OSGi configurations placed in CRX package.
     */
    val configPath = aem.obj.string { convention(appPath.map { "$it/config" }) }

    /**
     * Source directory for XML files holding OSGi configurations placed in CRX package.
     */
    val configDir = aem.obj.dir { convention(jcrRootDir.dir(configPath.map { it.removePrefix("/") })) }

    /**
     * Content path at which CRX Package Manager is storing uploaded packages.
     */
    val storagePath = aem.obj.string {
        convention("/etc/packages")
        aem.prop.string("package.storagePath")?.let { set(it) }
    }

    /**
     * Configures a local repository from which unreleased JARs could be added as 'compileOnly' dependency
     * and be deployed within CRX package deployment.
     */
    val installRepository = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("package.installRepository")?.let { set(it) }
    }

    /**
     * Customize default validation options.
     */
    fun validator(options: PackageValidator.() -> Unit) {
        this.validatorOptions = options
    }

    internal var validatorOptions: PackageValidator.() -> Unit = {}

    val wrapper by lazy { PackageWrapper(aem) }

    fun wrapper(options: PackageWrapper.() -> Unit) = wrapper.using(options)
}
