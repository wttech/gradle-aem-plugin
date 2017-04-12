package com.cognifide.gradle.aem.deploy

import org.gradle.api.tasks.TaskAction
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class UploadTask : DeployTask() {

    companion object {
        val NAME = "aemUpload"

        private val LOG = LoggerFactory.getLogger(UploadTask::class.java)
    }

    @TaskAction
    fun upload() {
        deploy { sync ->
            val file = File(determineLocalPackagePath())
            val url = sync.jsonTargetUrl + "/?cmd=upload"

            LOG.info("Uploading package at path '{}' to URL '{}'", file.path, url)

            try {
                val json = sync.post(url, mapOf(
                        "package" to file,
                        "force" to config.deployForce
                ))
                val response = UploadResponse.fromJson(json)

                if (response.isSuccess) {
                    LOG.info(response.msg)
                } else {
                    LOG.error(response.msg)
                    throw DeployException(response.msg.orEmpty())
                }
            } catch (e: FileNotFoundException) {
                throw DeployException(String.format("Package file '%s' not found!", file.path), e)
            } catch (e: IOException) {
                throw DeployException(e.message.orEmpty(), e)
            }
        }
    }


}
