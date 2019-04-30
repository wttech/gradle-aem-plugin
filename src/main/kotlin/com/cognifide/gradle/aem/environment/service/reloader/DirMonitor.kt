package com.cognifide.gradle.aem.environment.service.reloader

import java.io.File
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver

// TODO get rid off shutdown hook
class DirMonitor(private val dir: File, private val modificationChannel: SendChannel<String>) {

    fun start() {
        val fao = FileAlterationObserver(dir)
        fao.addListener(DirMonitorListener { event ->
            GlobalScope.launch {
                modificationChannel.send(event)
            }
        })
        val monitor = FileAlterationMonitor(WATCH_INTERVAL)
        monitor.addObserver(fao)
        monitor.start()

        Runtime.getRuntime().addShutdownHook(Thread(Runnable {
            try {
                monitor.stop()
            } catch (ignored: Exception) {
            }
        }))
    }

    companion object {
        const val WATCH_INTERVAL = 500L
    }
}
