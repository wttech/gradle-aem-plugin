package com.cognifide.gradle.aem.common.file

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.watcher.DelegatingFileAlterationListener
import com.cognifide.gradle.aem.common.file.watcher.Event
import com.cognifide.gradle.aem.common.utils.Patterns
import java.io.File
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
                    addListener(DelegatingFileAlterationListener { event ->
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
}
