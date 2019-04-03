package com.cognifide.gradle.aem.environment.docker

interface Stack {
    fun deploy(composeFilePath: String)
    fun rm()
    fun isDown(): Boolean
}