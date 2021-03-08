package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.utils.shortenClass
import com.cognifide.gradle.common.utils.Patterns
import com.cognifide.gradle.common.zip.ZipFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

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
    val pid = aem.obj.string {
        aem.prop.string("package.config.pid")?.let { set(it) }
    }

    @TaskAction
    fun sync() {
        instance.get().examine()

        instance.get().sync {
            common.progress {
                step = "Preparing"

                val configPidPattern = pid.orNull ?: listOfNotNull(aem.javaPackage).ifEmpty { aem.javaPackages }.joinToString(",") { "$it.*" }
                val configPids = osgi.determineConfigurationState().pids.map { it.id }.filter {
                    Patterns.wildcard(it, configPidPattern)
                }

                if (configPids.isEmpty()) {
                    logger.lifecycle("None of OSGi configuration XML files synchronized matching PID '$configPidPattern'!")
                } else {
                    total = configPids.size.toLong()

                    step = "Processing"

                    val rootNode = repository.node(rootPath.get())
                    val configNodes = CopyOnWriteArrayList<Node>()

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
                    val configFiles = mutableListOf<File>()
                    configNodes.forEach { configNode ->
                        val configFile = saveDir.get().asFile.resolve("${configNode.name.replace("~", "-")}.xml")
                        configZip.unpackFile("${Package.JCR_ROOT}${configNode.path}.xml", configFile)
                        configFiles.add(configFile)
                    }

                    step = "Cleaning"

                    rootNode.delete()
                    configPkg.delete()

                    logger.lifecycle("Synchronized OSGi configuration XML file(s) (${configPids.size}) matching PID '$configPidPattern':\n" +
                            configFiles.joinToString("\n"))
                }
            }
        }
    }

    init {
        description = "Check out OSGi configuration then save as JCR content."
    }

    companion object {
        const val NAME = "packageConfig"

        const val PID_LENGTH = 64
    }
}
