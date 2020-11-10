package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import java.io.File

class JavaOptions(aem: AemExtension) {

    val homeDir = aem.obj.dir {
        set(File(System.getProperty("java.home")))
    }

    val homePath get() = homeDir.get().asFile.absolutePath

    val executableFile = aem.obj.file {
        set(homeDir.file("bin/java"))
    }

    val executablePath get() = executableFile.get().asFile.absolutePath
}
