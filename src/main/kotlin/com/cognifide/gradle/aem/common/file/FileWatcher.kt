package com.cognifide.gradle.aem.common.file

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.AemExtension
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.apache.commons.io.monitor.FileAlterationListener
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import java.io.File

@UseExperimental(ObsoleteCoroutinesApi::class)
open class FileWatcher(val aem: AemExtension) {

    private val modificationsChannel = Channel<String>(Channel.UNLIMITED)

    lateinit var dir: File

    lateinit var onChange: (List<String>) -> Unit

    var interval = 500L

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
                GlobalScope.launch {
                    modificationsChannel.send(event)
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

    private class CustomFileAlterationListener(private val notify: (String) -> Unit) : FileAlterationListener {

        override fun onFileCreate(file: File) {
            notify("$file [file created]")
        }

        override fun onFileChange(file: File) {
            notify("$file [file changed]")
        }

        override fun onFileDelete(file: File) {
            notify("$file [file deleted]")
        }

        override fun onDirectoryCreate(directory: File) {
            notify("$directory [dir created]")
        }

        override fun onDirectoryDelete(directory: File) {
            notify("$directory [dir deleted]")
        }

        override fun onDirectoryChange(directory: File?) {}

        override fun onStart(observer: FileAlterationObserver?) {}

        override fun onStop(observer: FileAlterationObserver?) {}
    }

}