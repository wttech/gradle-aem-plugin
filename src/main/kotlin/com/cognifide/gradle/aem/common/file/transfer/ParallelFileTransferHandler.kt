package com.cognifide.gradle.aem.common.file.transfer

import kotlinx.coroutines.*

@UseExperimental(ObsoleteCoroutinesApi::class)
object ParallelFileTransferHandler {

    private const val POOL_SIZE = 2

    private const val POOL_NAME = "fileTransferPool"

    private val pool = newFixedThreadPoolContext(POOL_SIZE, POOL_NAME)

    fun <A : FileTransfer, B> queueTask(executor: A, task: A.() -> B): B {
        return runBlocking(pool) {
            println("Downloading package on ${Thread.currentThread()}")
            executor.run(task)
        }
    }
}