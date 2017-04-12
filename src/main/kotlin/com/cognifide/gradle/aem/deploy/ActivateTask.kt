package com.cognifide.gradle.aem.deploy

import org.gradle.api.tasks.TaskAction
import org.slf4j.LoggerFactory
import java.io.IOException

class ActivateTask : DeployTask() {

    companion object {
        val NAME = "aemActivate"

        private val LOG = LoggerFactory.getLogger(ActivateTask::class.java)
    }

    @TaskAction
    fun activate() {
        deploy { sync ->
            val path = determineRemotePackagePath(sync)
            val url = sync.jsonTargetUrl + path + "/?cmd=replicate"

            LOG.info("Activating package using command: " + url)

            val json: String
            try {
                json = sync.post(url)
            } catch (e: DeployException) {
                throw DeployException(e.message.orEmpty(), e)
            }

            val response: UploadResponse = try {
                UploadResponse.fromJson(json)
            } catch (e: IOException) {
                LOG.error("Malformed JSON response", e)
                throw DeployException("Package activation failed")
            }

            if (response.isSuccess) {
                LOG.info("Package activated")
            } else {
                LOG.error("Package activation failed: + " + response.msg)
                throw DeployException(response.msg.orEmpty())
            }
        }
    }

}
