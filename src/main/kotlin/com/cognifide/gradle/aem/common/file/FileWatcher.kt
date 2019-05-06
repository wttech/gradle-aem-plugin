package com.cognifide.gradle.aem.common.file

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.Patterns
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.apache.commons.io.monitor.FileAlterationListener
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver

// TODO refactor / remove shutdown hook somehow
@UseExperimental(ObsoleteCoroutinesApi::class)
open class FileWatcher(val aem: AemExtension) {

    private val modificationsChannel = Channel<Event>(Channel.UNLIMITED)

    lateinit var dir: File

    lateinit var onChange: (List<Event>) -> Unit

    var interval = 500L

    var ignores = mutableListOf("**/___jb_tmp___")

    fun ignore(vararg paths: String) =  ignore(paths.toList())

    fun ignore(paths: Iterable<String>) {
        ignores.addAll(paths)
    }

    fun start() {
        if (!::dir.isInitialized) {
            throw AemException("File watcher directory is not specified!")
        }

        if (!::onChange.isInitialized) {
            throw AemException("File watcher on change callback is not specified!")
        }

        runBlocking {
            // register watching
            val fao = FileAlterationObserver(dir)
            fao.addListener(CustomFileAlterationListener { event ->
                if (!Patterns.wildcard(event.file, ignores)) {
                    GlobalScope.launch {
                        modificationsChannel.send(event)
                    }
                }
            })
            val monitor = FileAlterationMonitor(interval)
            monitor.addObserver(fao)
            monitor.start()

            // handle on exit
            Runtime.getRuntime().addShutdownHook(Thread(Runnable {
                try {
                    monitor.stop()
                } catch (ignored: Exception) {
                }
            }))

            // listen watched changes
            launch(Dispatchers.IO) {
                while (true) {
                    val changes = modificationsChannel.receiveAvailable()
                    onChange(changes)
                }
            }
        }
    }

    private suspend fun <E> ReceiveChannel<E>.receiveAvailable(): List<E> {
        val allMessages = mutableListOf<E>()
        allMessages.add(receive())
        var next = poll()
        while (next != null) {
            allMessages.add(next)
            next = poll()
        }
        return allMessages
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

        override fun onDirectoryChange(directory: File?) {}

        override fun onStart(observer: FileAlterationObserver?) {}

        override fun onStop(observer: FileAlterationObserver?) {}
    }
}