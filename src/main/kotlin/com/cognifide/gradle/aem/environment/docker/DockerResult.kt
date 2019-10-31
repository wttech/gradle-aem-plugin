package com.cognifide.gradle.aem.environment.docker

import org.buildobjects.process.ProcResult

class DockerResult(val base: ProcResult) {

    val executionTime: Long get() = base.executionTime

    val procString: String get() = base.procString

    val exitCode: Int get() = base.exitValue

    val outputString: String get() = base.outputString

    val outputBytes: ByteArray get() = base.outputBytes

    val errorString: String get() = base.errorString

    val errorBytes: ByteArray get() = base.errorBytes
}
