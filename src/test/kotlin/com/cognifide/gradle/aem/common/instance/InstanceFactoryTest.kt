package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InstanceFactoryTest {

    @Test
    fun `should create instance with correct url`() {
        val testUrl = "https://author-7777.adobeaemcloud.com"
        val instance = InstanceFactory(aem).createByUrl(testUrl)
        assertEquals(instance.httpUrl.get(), testUrl)
    }

    @Test
    fun `should create remote instance with correct url`() {
        val testUrl = "https://author-7777.adobeaemcloud.com"
        val instance = InstanceFactory(aem).remoteByUrl(testUrl)
        assertEquals(instance.httpUrl.get(), testUrl)
    }

    @Test
    fun `should create local instance with correct url`() {
        val testUrl = "http://localhost.com:7777"
        val instance = InstanceFactory(aem).localByUrl(testUrl)
        assertEquals(instance.httpUrl.get(), testUrl)
    }

    private val project get() = ProjectBuilder.builder().build()
    private val aem get() = AemExtension(project.also { it.plugins.apply("com.cognifide.aem.instance") })
}
