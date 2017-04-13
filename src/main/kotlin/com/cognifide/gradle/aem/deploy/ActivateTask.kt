package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.tasks.TaskAction
import java.io.IOException

open class ActivateTask : AbstractTask() {

    companion object {
        val NAME = "aemActivate"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Activates AEM package on instance(s)."
    }

    @TaskAction
    fun activate() {
        deploy { sync ->
            val path = determineRemotePackagePath(sync)
            val url = sync.jsonTargetUrl + path + "/?cmd=replicate"

            logger.info("Activating package using command: " + url)

            val json: String
            try {
                json = sync.post(url)
            } catch (e: DeployException) {
                throw DeployException(e.message.orEmpty(), e)
            }

            val response: UploadResponse = try {
                UploadResponse.fromJson(json)
            } catch (e: IOException) {
                logger.error("Malformed JSON response", e)
                throw DeployException("Package activation failed")
            }

            if (response.isSuccess) {
                logger.info("Package activated")
            } else {
                logger.error("Package activation failed: + " + response.msg)
                throw DeployException(response.msg.orEmpty())
            }
        }
    }

}
