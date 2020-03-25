package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.file.ZipFile
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.utils.shortenClass
import com.cognifide.gradle.common.utils.Patterns
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
    fun sync() = instance.get().sync {
        common.progress {
            step = "Preparing"

            val configPidPattern = pid.orNull ?: aem.javaPackages.joinToString(",") { "$it.*" }
            val configPids = osgi.determineConfigurationState().pids.map { it.id }.filter { Patterns.wildcard(it, configPidPattern) }
            total = configPids.size.toLong()

            step = "Processing"

            val rootNode = repository.node(rootPath.get())
            val configNodes = mutableListOf<Node>()

            common.parallel.poolEach(configPids) { configPid ->
                increment("Configuration '${configPid.shortenClass(PID_LENGTH)}'") {
                    val config = osgi.findConfiguration(configPid)
                    if (config != null) {
                        rootNode.import(config.properties + mapOf("jcr:primaryType" to "sling:OsgiConfig"), config.pid, true)
                        configNodes.add(rootNode.child(config.pid))
                    }
                }
            }

            step = "Downloading"

            val configPkg = packageManager.download {
                filters(configNodes.map { it.path })
                archiveFileName.convention("config.zip")
            }

            step = "Extracting"

            val configZip = ZipFile(configPkg)
            configNodes.forEach { configNode ->
                val configFile = saveDir.get().asFile.resolve("${configNode.name.replace("~", "-")}.xml")
                configZip.unpackFile("${Package.JCR_ROOT}${configNode.path}.xml", configFile)
            }

            step = "Cleaning"

            rootNode.delete()
            configPkg.delete()

            logger.lifecycle("Saved ${configPids.size} OSGi configuration XML file(s) to directory: ${saveDir.get().asFile}")
        }
    }

    init {
        description = "Saves current OSGi configuration of given PID as XML file being a part of JCR content."
    }

    companion object {
        const val NAME = "packageConfig"

        const val PID_LENGTH = 64
    }
}
