package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vault.NodeTypesSync
import java.io.Serializable

class PackageOptions(private val aem: AemExtension) : Serializable {

    /**
     * Package specific configuration
     */
    val configDir = aem.obj.projectDir("src/aem/package")

    /**
     * Package root directory containing 'jcr_root' and 'META-INF' directories.
     */
    val contentDir = aem.obj.projectDir("src/main/content")

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
     * Default convention assumes that subprojects have separate bundle paths, because of potential re-installation of subpackages.
     * When all subprojects will have same bundle path, reinstalling one subpackage may end with deletion of other bundles coming from another subpackage.
     *
     * Beware that more nested bundle install directories are not supported by AEM by default (up to 4th depth level).
     * That's the reason of using dots in subproject names to avoid that limitation.
     */
    val installPath = aem.obj.string {
        convention(aem.obj.provider {
            when (aem.project) {
                aem.project.rootProject -> "/apps/${aem.project.rootProject.name}/install"
                else -> "/apps/${aem.project.rootProject.name}/${aem.project.name}/install"
            }
        })
    }

    /**
     * Content path at which CRX Package Manager is storing uploaded packages.
     */
    val storagePath = aem.obj.string { convention("/etc/packages") }

    /**
     * Calculate directory under storage path for each CRX package.
     */
    val storageDir: PackageFile.() -> String = { group }

    /**
     * Configures a local repository from which unreleased JARs could be added as 'compileOnly' dependency
     * and be deployed within CRX package deployment.
     */
    val installRepository = aem.obj.boolean { convention(true) }

    /**
     * Customize default validation options.
     */
    fun validator(options: PackageValidator.() -> Unit) {
        this.validatorOptions = options
    }

    internal var validatorOptions: PackageValidator.() -> Unit = {}

    /**
     * Controls automatic node types exporting from available instance to be later used in package validation.
     */
    val nodeTypesSync = aem.obj.typed<NodeTypesSync> {
        convention(aem.obj.provider {
            when {
                aem.commonOptions.offline.get() -> NodeTypesSync.PRESERVE_FALLBACK
                else -> NodeTypesSync.PRESERVE_AUTO
            }
        })
        aem.prop.string("package.nodeTypesSync")?.let { set(NodeTypesSync.of(it)) }
    }

    fun nodeTypesSync(name: String) {
        nodeTypesSync.set(NodeTypesSync.of(name))
    }

    /**
     * Determines location on which synchronized node types will be saved.
     */
    val nodeTypesSyncFile = aem.obj.file {
        convention(configDir.file(Package.NODE_TYPES_SYNC_FILE))
        aem.prop.file("package.nodeTypesSyncFile")?.let { fileValue(it) }
    }
}
