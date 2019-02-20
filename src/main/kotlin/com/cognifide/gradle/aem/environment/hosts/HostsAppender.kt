package com.cognifide.gradle.aem.environment.hosts

class HostsAppender {
    fun appendedContent(hostsString: String, hosts: List<Host>): String {
        val lines = hostsString.split(System.lineSeparator())
        val map = lines.map { it to null }.toList().toMap()
        val newHosts = hosts.filter { !map.containsKey(it.text) }.map { it.text }
        return assembleFileContent(lines + newHosts)
    }

    fun linesToAppend(hostsString: String, hosts: List<Host>): List<String> {
        val lines = hostsString.split(System.lineSeparator())
        val map = lines.map { it to null }.toList().toMap()
        return hosts.filter { !map.containsKey(it.text) }.map { it.text }
    }

    private fun assembleFileContent(lines: List<String>) =
        lines.joinToString(System.lineSeparator())
}
