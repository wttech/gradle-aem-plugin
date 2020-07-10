package com.cognifide.gradle.sling.common.instance.local

import com.cognifide.gradle.sling.SlingExtension
import java.io.File

class StarterResolver(private val sling: SlingExtension) {

    private val common = sling.common

    /**
     * Directory storing downloaded Sling Starter source files (JAR & license).
     */
    val downloadDir = sling.obj.dir {
        convention(sling.obj.buildDir(TEMPORARY_DIR))
        sling.prop.file("localInstance.starter.downloadDir")?.let { set(it) }
    }

    /**
     * URI pointing to Sling self-extractable JAR containing launchpad directory 'sling'.
     */
    val jarUrl = sling.obj.string {
        convention("org.apache.sling:org.apache.sling.starter:11")
        sling.prop.string("localInstance.starter.jarUrl")?.let { set(it) }
    }

    val jar: File? get() = jarUrl.orNull?.let { common.fileTransfer.downloadTo(it, downloadDir.get().asFile) }

    val files: List<File> get() = listOfNotNull(jar)

    companion object {

        const val TEMPORARY_DIR = "instance/sling"
    }
}
