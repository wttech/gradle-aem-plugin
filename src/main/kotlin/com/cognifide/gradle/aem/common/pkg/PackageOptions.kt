package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import java.io.Serializable

class PackageOptions(private val aem: AemExtension) : Serializable {

    /**
     * Package specific configuration
     */
    val configDir = aem.obj.projectDir(aem.prop.string("package.configPath") ?: "src/aem/package")

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
    val metaCommonDir = aem.obj.relativeDir(configDir, Package.META_PATH)

    /**
     * Content path for OSGi bundle jars being placed in CRX package.
     *
     * Default convention assumes that sub-projects have separate bundle paths, because of potential re-installation of sub-packages.
     * When all sub-projects will have same bundle path, reinstalling one sub-package may end with deletion of other bundles coming from another sub-package.
     * As a consequence, generally it is recommended to avoid providing value by putting it into properties file.
     *
     * Beware that more nested bundle install directories are not supported by AEM by default (up to 4th depth level).
     * That's the reason of using dots in sub-project names to avoid that limitation.
     */
    val installPath = aem.obj.string {
        convention(aem.obj.provider {
            when (aem.project) {
                aem.project.rootProject -> "/apps/${aem.project.rootProject.name}/install"
                else -> "/apps/${aem.project.rootProject.name}/${aem.project.name}/install"
            }
        })
        aem.prop.string("package.installPath")?.let { set(it) }
    }

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
}
