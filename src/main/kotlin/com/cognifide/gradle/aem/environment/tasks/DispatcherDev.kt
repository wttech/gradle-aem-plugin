package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.environment.DirWatcher
import com.cognifide.gradle.aem.environment.docker.DockerTask
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.gradle.api.tasks.TaskAction

open class DispatcherDev : DockerTask() {
    init {
        description = "Listens to httpd/dispatcher configuration changes and reloads Apache."
    }

    private val dispatcherConfModificationChannel = Channel<Any>()
    private val dirWatcher = DirWatcher(config.dispatcherConfPath, dispatcherConfModificationChannel)

    @TaskAction
    fun dev() {
        aem.logger.lifecycle("Listening for HTTPD configuration changes at: ${config.dispatcherConfPath}")
        runBlocking {
            dirWatcher.watch()
            launch {
                while (true) {
                    dispatcherConfModificationChannel.receive()
                    stack.exec("dispatcher", HTTPD_RESTART_COMMAND, EXPECTED_HTTPD_RESTART_EXIT_CODE)
                    aem.logger.lifecycle("HTTPD configuration reloaded!")
                }
            }
        }
    }

    companion object {
        const val NAME = "aemDispatcherDev"
        private const val HTTPD_RESTART_COMMAND = "/usr/sbin/httpd -k restart"
        private const val EXPECTED_HTTPD_RESTART_EXIT_CODE = 129
    }
}