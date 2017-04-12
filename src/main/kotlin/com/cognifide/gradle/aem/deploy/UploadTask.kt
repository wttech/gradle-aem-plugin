package com.cognifide.gradle.aem.deploy

import org.gradle.api.tasks.TaskAction
import java.io.FileNotFoundException
import java.io.IOException

open class UploadTask : AbstractTask() {

    companion object {
        val NAME = "aemUpload"
    }

    @TaskAction
    fun upload() {
        deploy { sync ->
            val file = determineLocalPackage()
            val url = sync.jsonTargetUrl + "/?cmd=upload"

            logger.info("Uploading package at path '{}' to URL '{}'", file.path, url)

            try {
                val json = sync.post(url, mapOf(
                        "package" to file,
                        "force" to config.deployForce
                ))
                val response = UploadResponse.fromJson(json)

                if (response.isSuccess) {
                    logger.info(response.msg)
                } else {
                    logger.error(response.msg)
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
