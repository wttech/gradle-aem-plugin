package com.cognifide.gradle.aem.common.file

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.utils.Patterns
import java.io.File
import org.apache.commons.io.monitor.FileAlterationListener
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver

open class FileWatcher(val aem: AemExtension) {

    lateinit var dirs: List<File>

    lateinit var onChange: (Event) -> Unit

    var interval = 500L

    var ignores = mutableListOf("**/*___jb_*___")

    fun ignore(vararg paths: String) = ignore(paths.toList())

    fun ignore(paths: Iterable<String>) {
        ignores.addAll(paths)
    }

    fun start() {
        if (!::dirs.isInitialized) {
            throw AemException("File watcher directories are not specified!")
        }

        if (!::onChange.isInitialized) {
            throw AemException("File watcher on change callback is not specified!")
        }

        // register watching

        val monitor = FileAlterationMonitor(interval).apply {
            dirs.forEach { dir ->
                addObserver(FileAlterationObserver(dir).apply {
                    addListener(CustomFileAlterationListener { event ->
                        if (!Patterns.wildcard(event.file, ignores)) {
                            onChange(event)
                        }
                    })
                })
            }

            start()
        }

        // handle on exit
        // TODO remove/refactor shutdown hook somehow
        Runtime.getRuntime().addShutdownHook(Thread(Runnable {
            try {
                monitor.stop()
            } catch (ignored: Exception) {
                // ignore
            }
        }))
    }

    class Event(val file: File, val type: EventType) {
        override fun toString(): String {
            return "$file [${type.name.toLowerCase().replace("_", " ")}]"
        }
    }

    enum class EventType {
        FILE_CREATED,
        FILE_CHANGED,
        FILE_DELETED,
        DIR_CREATED,
        DIR_DELETED,
    }

    private class CustomFileAlterationListener(private val notify: (Event) -> Unit) : FileAlterationListener {

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
}
