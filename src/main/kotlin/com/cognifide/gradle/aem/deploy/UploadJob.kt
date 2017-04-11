package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemInstance
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class UploadJob(instance: AemInstance, config: AemConfig) : AbstractJob(instance, config) {

    companion object {
        private val LOG = LoggerFactory.getLogger(UploadJob::class.java)
    }

    fun upload(packageFile: File): UploadResponse {
        val url = sync.jsonTargetUrl + "/?cmd=upload"

        LOG.info("Uploading package at path '{}' to URL '{}'", packageFile.path, url)

        try {
            val json = sync.post(url, mapOf("package" to packageFile, "force" to config.deployForce))
            val response = UploadResponse.fromJson(json)

            if (response.isSuccess) {
                LOG.info(response.msg)
            } else {
                LOG.error(response.msg)
                throw DeployException(response.msg.orEmpty())
            }

            return response
        } catch (e: FileNotFoundException) {
            throw DeployException(String.format("Package file '%s' not found!", packageFile.path), e)
        } catch (e: IOException) {
            throw DeployException(e.message.orEmpty(), e)
        }
    }

}
