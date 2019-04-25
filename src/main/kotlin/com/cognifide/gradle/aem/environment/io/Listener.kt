package com.cognifide.gradle.aem.environment.io

import java.io.File
import org.apache.commons.io.monitor.FileAlterationListener
import org.apache.commons.io.monitor.FileAlterationObserver

class Listener(private val notify: (String) -> Unit) : FileAlterationListener {
    override fun onDirectoryDelete(directory: File?) {
        notify("${directory?.name} deleted")
    }

    override fun onFileCreate(file: File?) {
        notify("${file?.name} created")
    }

    override fun onFileChange(file: File?) {
        notify("${file?.name} changed")
    }

    override fun onDirectoryCreate(directory: File?) {
        notify("${directory?.name} created")
    }

    override fun onFileDelete(file: File?) {
        notify("${file?.name} deleted")
    }

    override fun onDirectoryChange(directory: File?) {}
    override fun onStart(observer: FileAlterationObserver?) {}
    override fun onStop(observer: FileAlterationObserver?) {}
}