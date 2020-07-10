package com.cognifide.gradle.sling.common.pkg

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.instance.service.pkg.Package
import com.cognifide.gradle.common.utils.using
import java.io.Serializable

class PackageOptions(private val sling: SlingExtension) : Serializable {

    /**
     * Package specific configuration
     */
    val commonDir = sling.obj.projectDir(sling.prop.string("package.commonPath") ?: "src/sling/package")

    /**
     * Package root directory containing 'jcr_root' and 'META-INF' directories.
     */
    val contentDir = sling.obj.projectDir(sling.prop.string("package.contentPath") ?: "src/main/content")

    /**
     * JCR root directory.
     */
    val jcrRootDir = sling.obj.relativeDir(contentDir, Package.JCR_ROOT)

    /**
     * Vault metadata files directory (package definition).
     */
    val vltDir = sling.obj.relativeDir(contentDir, Package.VLT_PATH)

    /**
     * Custom path to Vault files that will be used to build CRX package.
     * Useful to share same files for all packages, like package thumbnail.
     */
    val metaCommonDir = sling.obj.relativeDir(commonDir, "defaults/${Package.META_PATH}")

    /**
     * Content path for Sling application placed in CRX package.
     */
    val appPath = sling.obj.string {
        convention(sling.obj.provider {
            when (sling.project) {
                sling.project.rootProject -> "/apps/${sling.project.rootProject.name}"
                else -> "/apps/${sling.project.rootProject.name}/${sling.project.name}"
            }
        })
        sling.prop.string("package.appPath")?.let { set(it) }
    }

    /**
     * Repository path for OSGi bundles (JAR files) placed in CRX package.
     */
    val installPath = sling.obj.string { convention(appPath.map { "$it/install" }) }

    /**
     * Source directory for OSGi bundles (JAR files) placed in CRX package.
     */
    val installDir = sling.obj.dir { convention(jcrRootDir.dir(installPath.map { it.removePrefix("/") })) }

    /**
     * Repository path for OSGi configurations placed in CRX package.
     */
    val configPath = sling.obj.string { convention(appPath.map { "$it/config" }) }

    /**
     * Source directory for XML files holding OSGi configurations placed in CRX package.
     */
    val configDir = sling.obj.dir { convention(jcrRootDir.dir(configPath.map { it.removePrefix("/") })) }

    /**
     * Content path at which CRX Package Manager is storing uploaded packages.
     */
    val storagePath = sling.obj.string {
        convention("/etc/packages")
        sling.prop.string("package.storagePath")?.let { set(it) }
    }

    /**
     * Configures a local repository from which unreleased JARs could be added as 'compileOnly' dependency
     * and be deployed within CRX package deployment.
     */
    val installRepository = sling.obj.boolean {
        convention(true)
        sling.prop.boolean("package.installRepository")?.let { set(it) }
    }

    /**
     * Customize default validation options.
     */
    fun validator(options: PackageValidator.() -> Unit) {
        this.validatorOptions = options
    }

    internal var validatorOptions: PackageValidator.() -> Unit = {}

    val wrapper by lazy { PackageWrapper(sling) }

    fun wrapper(options: PackageWrapper.() -> Unit) = wrapper.using(options)
}
