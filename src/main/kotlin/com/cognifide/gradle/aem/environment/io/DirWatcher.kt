package com.cognifide.gradle.aem.environment.io

import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.attribute.BasicFileAttributes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

class DirWatcher(private val directory: String, private val modificationChannel: SendChannel<List<String>>) {
    private val watcher = FileSystems.getDefault().newWatchService()

    fun watch() {
        GlobalScope.run {
            registerRecursive(directory)
            launch(Dispatchers.IO) {
                while (true) {
                    val changes = async {
                        val key = watcher.take()
                        val changes = mapModificationEvents(key)
                        key.reset()
                        changes
                    }
                    modificationChannel.send(changes.await())
                }
            }
        }
    }

    private fun mapModificationEvents(key: WatchKey): List<String> {
        return key.pollEvents().map { change ->
            when (change.kind()) {
                ENTRY_CREATE -> "${change.context()} was created"
                ENTRY_MODIFY -> "${change.context()} was modified"
                OVERFLOW -> "${change.context()} overflow"
                ENTRY_DELETE -> "${change.context()} was deleted"
                else -> "unknown change"
            }
        }.toList()
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