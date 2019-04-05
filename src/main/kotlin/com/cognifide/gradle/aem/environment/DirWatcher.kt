package com.cognifide.gradle.aem.environment

import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.attribute.BasicFileAttributes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

class DirWatcher(private val directory: String, private val modificationChannel: SendChannel<Any>) {
    private val watcher = FileSystems.getDefault().newWatchService()

    fun watch() {
        GlobalScope.run {
            registerRecursive(directory)
            launch(Dispatchers.IO) {
                while (true) {
                    val change = async { watcher.take() }
                    modificationChannel.send(change.await())
                }
            }
        }
    }

    private fun registerRecursive(root: String) {
        val rootPath = Paths.get(root)
        Files.walkFileTree(rootPath, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                return FileVisitResult.CONTINUE
            }
        })
    }
}