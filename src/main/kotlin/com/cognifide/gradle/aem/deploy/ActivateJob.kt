package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemInstance
import org.slf4j.LoggerFactory
import java.io.IOException

class ActivateJob(instance: AemInstance, config: AemConfig) : AbstractJob(instance, config) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ActivateJob::class.java)
    }

    fun activate(uploadedPackagePath: String): UploadResponse {
        val url = sync.jsonTargetUrl + uploadedPackagePath + "/?cmd=replicate"

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

        return response
    }


}
