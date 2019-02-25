package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.environment.hosts.Host
import com.cognifide.gradle.aem.environment.hosts.HostsAppender
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class HostsAppenderTest {

    object IO {
        fun text(resource: String): String = FileUtils.readFileToString(file(resource), "UTF8")

        private fun file(resource: String) = File(this::class.java.classLoader.getResource(resource).file)
    }

    private val hostsAppender = object : HostsAppender() {
        override fun appendHosts() {}
    }

    @Test
    fun shouldAppendEtcHostsWithRequiredHosts() {
        // given
        val hosts = listOf(Host("127.0.0.1", "example.com"), Host("127.0.0.1", "demo.example.com"))
        val hostsText = IO.text("com/cognifide/gradle/aem/test/hosts/hosts")

        // when
        val content = hostsAppender.appendedContent(hostsText, hosts)

        // then
        hosts.forEach { assertTrue(content.contains(it.text)) }
    }

    @Test
    fun appendingEtcHostsShouldBeIdempotent() {
        // given
        val hosts = listOf(Host("127.0.0.1", "example.com"), Host("127.0.0.1", "demo.example.com"))
        val hostsText = IO.text("com/cognifide/gradle/aem/test/hosts/hosts_already_appended")

        // when
        val content: String = hostsAppender.appendedContent(hostsText, hosts)

        // then
        hosts.forEach { assertEquals(1, StringUtils.countMatches(content, it.text)) }
    }

    @Test
    fun shouldIgnoreCommentsWhenAppending() {
        // given
        val hosts = listOf(Host("127.0.0.1", "example.com"), Host("127.0.0.1", "demo.example.com"))
        val hostsText = IO.text("com/cognifide/gradle/aem/test/hosts/hosts_with_comments")

        // when
        val content: String = hostsAppender.appendedContent(hostsText, hosts)

        // then
        hosts.forEach { assertEquals(2, StringUtils.countMatches(content, it.text)) }
    }
}
