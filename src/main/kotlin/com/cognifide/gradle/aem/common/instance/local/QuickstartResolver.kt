package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.common.instance.LocalInstanceException
import com.cognifide.gradle.aem.common.instance.LocalInstanceManager
import com.cognifide.gradle.common.os.OSUtil
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.Patterns
import org.gradle.internal.os.OperatingSystem
import java.io.File

class QuickstartResolver(private val manager: LocalInstanceManager) {

    private val aem = manager.aem

    private val common = aem.common

    /**
     * Directory storing downloaded AEM Quickstart source files (JAR & license).
     */
    val downloadDir = aem.obj.dir {
        convention(aem.project.layout.buildDirectory.dir("localInstance/quickstart"))
        aem.prop.file("localInstance.quickstart.downloadDir")?.let { set(it) }
    }

    val distJar: File? get() = sdkJar ?: jar

    /**
     * URI pointing to AEM distribution (AEM SDK zip or AEM jar)
     */
    val distUrl = aem.obj.string {
        aem.prop.string("localInstance.quickstart.distUrl")?.let { set(it) }
    }

    /**
     * URI pointing to AEM self-extractable JAR containing 'crx-quickstart'.
     */
    val jarUrl = aem.obj.string {
        convention(distUrl.map { it.takeIf { it.endsWith(".jar") } })
        aem.prop.string("localInstance.quickstart.jarUrl")?.let { set(it) }
    }

    val jar: File? get() = jarUrl.orNull?.let { common.fileTransfer.downloadTo(it, downloadDir.get().asFile) }

    /**
     * URI pointing to AEM quickstart license file.
     */
    val licenseUrl = aem.obj.string {
        aem.prop.string("localInstance.quickstart.licenseUrl")?.let { set(it) }
    }

    val license: File? get() = licenseUrl.orNull?.let { common.fileTransfer.downloadTo(it, downloadDir.get().asFile) }

    /**
     * URI pointing to AEM SDK ZIP containing AEM instance JAR and Dispatcher Docker image.
     */
    val sdkUrl = aem.obj.string {
        convention(distUrl.map { it.takeIf { it.endsWith(".zip") } })
        aem.prop.string("localInstance.quickstart.sdkUrl")?.let { set(it) }
    }

    val sdk: File? get() = sdkUrl.orNull?.let { common.fileTransfer.downloadTo(it, downloadDir.get().asFile) }

    val sdkDir = aem.obj.dir {
        convention(manager.rootDir.dir("sdk"))
        aem.prop.file("localInstance.quickstart.sdkDir")?.let { set(it) }
    }

    private val sdkWorkDir: File get() = sdkDir.get().asFile

    val sdkJar: File? get() = sdk?.also { unpackSdkZip(it) }
        .let {
            sdkWorkDir.listFiles { name -> Patterns.wildcard(name, "*.jar") }?.firstOrNull()
        }

    val sdkDispatcherImage: File? get() = sdk
        ?.also { unpackSdkZip(it) }
        ?.also { unpackSdkDispatcher() }
        ?.let { sdkWorkDir.resolve(sdkDispatcherImagePath.get()).takeIf { it.exists() } }

    val sdkDispatcherImagePath = aem.obj.string {
        convention(aem.obj.provider { "$DISPATCHER_DIR/lib/dispatcher-publish-${OSUtil.archOfHost()}.tar.gz" })
        aem.prop.string("localInstance.quickstart.sdkDispatcherImagePath")?.let { set(it) }
    }

    private fun unpackSdkZip(zip: File) {
        if (!sdkWorkDir.exists()) {
            common.progress {
                step = "Unpacking AEM SDK: ${zip.name} (${Formats.fileSize(zip)})"
                common.zip(zip).unpackAll(sdkWorkDir)
            }
        }
    }

    private fun unpackSdkDispatcher() {
        when {
            OperatingSystem.current().isWindows -> findAndUnpackSdkDispatcherZip()
            else -> findAndRunSdkDispatcherScript()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun findAndRunSdkDispatcherScript() {
        sdkWorkDir.listFiles { _, name -> Patterns.wildcard(name, "*-dispatcher-*-unix.sh") }
            ?.firstOrNull()
            ?.let { script ->
                val dispatcherDir = sdkWorkDir.resolve(DISPATCHER_DIR)
                if (!dispatcherDir.exists()) {
                    common.progress {
                        step = "Unpacking AEM SDK Dispatcher Tools: ${script.name} (${Formats.fileSize(script)})"
                        try {
                            aem.project.exec { spec ->
                                spec.workingDir(dispatcherDir.parentFile)
                                spec.commandLine("sh", script.absolutePath, "--target", dispatcherDir.absolutePath)
                            }.assertNormalExitValue()
                        } catch (e: Exception) {
                            throw LocalInstanceException("Cannot run AEM SDK Dispatcher unpacking script '${script.absolutePath}'!", e)
                        }
                    }
                }
            }
    }

    private fun findAndUnpackSdkDispatcherZip() {
        sdkWorkDir.listFiles { _, name -> Patterns.wildcard(name, "*-dispatcher-*-windows.zip") }
            ?.firstOrNull()
            ?.let { zip ->
                val dispatcherDir = sdkWorkDir.resolve(DISPATCHER_DIR)
                if (!dispatcherDir.exists()) {
                    common.progress {
                        step = "Unpacking AEM SDK Dispatcher Tools: ${zip.name} (${Formats.fileSize(zip)})"
                        common.zip(zip).unpackAll(dispatcherDir)
                    }
                }
            }
    }

    val files: List<File> get() = listOfNotNull(sdkJar, sdkDispatcherImage, jar, license)

    companion object {
        const val DISPATCHER_DIR = "dispatcher"
    }
}
