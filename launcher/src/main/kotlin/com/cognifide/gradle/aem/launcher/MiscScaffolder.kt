package com.cognifide.gradle.aem.launcher

class MiscScaffolder(private val launcher: Launcher) {

    fun scaffold() {
        appendGitIgnore()
    }

    private fun appendGitIgnore() = launcher.workFile(".gitignore") {
        val content = """
            ### Gradle/GAP ###
            .gradle/
            build/
            /gap.jar
        """.trimIndent()

        if (!readText().contains(content)) {
            println("Appending lines to Git ignore file '$this'")
            appendText("${launcher.eol}${content}${launcher.eol}")
        }
    }
}