package com.cognifide.gradle.aem.common.file.transfer

import kotlinx.coroutines.*

@UseExperimental(ObsoleteCoroutinesApi::class)
object ParallelExecutor {

    private const val POOL_SIZE = 3

    private const val POOL_NAME = "fileTransferPool"

    private val pool = newFixedThreadPoolContext(POOL_SIZE, POOL_NAME)

    fun <A, B> queueTask(executor: A, task: A.() -> B): B {
        return runBlocking(pool) {
            println("Running $executor  on ${Thread.currentThread()}")
            executor.run(task)
        }
    }
}