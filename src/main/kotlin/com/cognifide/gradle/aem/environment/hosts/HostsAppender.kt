package com.cognifide.gradle.aem.environment.hosts

import org.apache.commons.lang3.SystemUtils

abstract class HostsAppender {
    fun appendedContent(hostsString: String, hosts: List<Host>): String {
        val lines = hostsString.split(System.lineSeparator())
        val map = lines.map { it to null }.toList().toMap()
        val newHosts = hosts.filter { !map.containsKey(it.text) }.map { it.text }
        return assembleFileContent(lines + newHosts)
    }

    private fun assembleFileContent(lines: List<String>) =
            lines.joinToString(System.lineSeparator())

    abstract fun appendHosts()

    companion object {
        fun create(hosts: HostsOptions) = when {
            SystemUtils.IS_OS_WINDOWS -> HostsAppenderWindows(hosts)
            else -> HostsAppenderUnix(hosts)
        }
    }
}
