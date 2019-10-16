package com.cognifide.gradle.aem.environment.docker.container

import com.cognifide.gradle.aem.environment.docker.Container
import java.io.File

class DevOptions(val container: Container) {

    var watchDirs = mutableListOf<File>()

    fun watchDir(vararg files: File) = files.forEach {
        watchDirs.add(it)
    }

    fun watchConfigDir(vararg paths: String) = paths.forEach {
        watchDirs.add(File(container.host.configDir, it))
    }
}
