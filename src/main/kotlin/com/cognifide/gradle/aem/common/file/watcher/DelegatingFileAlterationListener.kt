package com.cognifide.gradle.aem.common.file.watcher

import org.apache.commons.io.monitor.FileAlterationListener
import org.apache.commons.io.monitor.FileAlterationObserver
import java.io.File

class DelegatingFileAlterationListener(private val notify: (Event) -> Unit) : FileAlterationListener {

    override fun onFileCreate(file: File) {
        notify(Event(file, EventType.FILE_CREATED))
    }

    override fun onFileChange(file: File) {
        notify(Event(file, EventType.FILE_CHANGED))
    }

    override fun onFileDelete(file: File) {
        notify(Event(file, EventType.FILE_DELETED))
    }

    override fun onDirectoryCreate(directory: File) {
        notify(Event(directory, EventType.DIR_CREATED))
    }

    override fun onDirectoryDelete(directory: File) {
        notify(Event(directory, EventType.DIR_DELETED))
    }

    override fun onDirectoryChange(directory: File?) {
        // do nothing
    }

    override fun onStart(observer: FileAlterationObserver?) {
        // do nothing
    }

    override fun onStop(observer: FileAlterationObserver?) {
        // do nothing
    }
}
