package com.cognifide.gradle.aem.environment.docker.base

import java.io.InputStream
import java.io.OutputStream

open class DockerExecSpec {

    lateinit var command: String

    var options: List<String> = listOf()

    fun workDir(path: String) {
        options = options + "--workdir $path"
    }

    fun user(id: String) {
        options = options + "--user $id"
    }

    fun env(vars: Map<String, String>) {
        vars.forEach { (varName, varValue) -> env(varName, varValue) }
    }

    fun env(varName: String, varValue: String) {
        options = options + "--env $varName=$varValue"
    }

    fun privileged() {
        options = options + "--privileged"
    }

    var exitCodes: List<Int> = listOf(0)

    var input: InputStream? = null

    var output: OutputStream? = null

    var errors: OutputStream? = null
}
