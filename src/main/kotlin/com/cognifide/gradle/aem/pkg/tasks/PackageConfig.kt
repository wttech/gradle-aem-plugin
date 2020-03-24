package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.file.ZipFile
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.PackageException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class PackageConfig : AemDefaultTask() {

    /**
     * Source instance from which OSGi config will be read.
     */
    @Internal
    val instance = aem.obj.typed<Instance> { convention(aem.obj.provider { aem.anyInstance }) }

    /**
     * Root repository path used to store temporarily OSGi configs for a serialization time only.
     */
    @Internal
    val rootPath = aem.obj.string { convention("/var/gap/package/config") }

    /**
     * Target directory in which OSGi configs read will be saved.
     */
    @Internal
    val saveDir = aem.obj.dir { convention(aem.packageOptions.configDir) }

    /**
     * Unique ID of OSGi config to be saved.
     */
    @Internal
    val pid = aem.obj.string { convention(aem.prop.string("package.config.pid")) }

    @TaskAction
    fun saveOsgiConfig() = instance.get().sync {
        val pid = pid.orNull ?: throw PackageException("OSGi config PID is not specified!")
        val config = osgi.getConfiguration(pid)

        val rootNode = repository.node(rootPath.get())
        rootNode.import(config.properties + mapOf("jcr:primaryType" to "sling:OsgiConfig"), pid, true)

        val configNode = rootNode.child(pid)
        val configPkg = configNode.downloadPackage()
        val configZip  = ZipFile(configPkg)
        configZip.unpackFileTo("${Package.JCR_ROOT}${configNode.path}.xml", saveDir.get().asFile)
        configPkg.delete()
    }

    init {
        description = "Saves current OSGi configuration of given PID as XML file being a part of JCR content."
    }

    companion object {
        const val NAME = "packageConfig"
    }
}
