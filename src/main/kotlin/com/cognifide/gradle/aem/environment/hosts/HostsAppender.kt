package com.cognifide.gradle.aem.environment.hosts

class HostsAppender {
    fun appendedContent(hostsString: String, hosts: List<Host>): String {
        val lines = hostsString.split(System.lineSeparator())
        val map = lines.map { it to null }.toList().toMap()
        val newHosts = hosts.filter { !map.containsKey(it.text) }.map { it.text }
        return assembleFileContent(lines + newHosts)
    }

    private fun assembleFileContent(lines: List<String>) =
        lines.joinToString(System.lineSeparator())
}
