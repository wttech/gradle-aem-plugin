package com.cognifide.gradle.aem.common.instance.service.auth

import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.common.CommonException

class AuthManager(sync: InstanceSync) : InstanceService(sync) {

    fun updatePassword(user: String, currentPassword: String, newPassword: String) {
        logger.info("Updating password for user '$user' on $instance")

        val node = sync.repository.query {
            path("/home/users")
            type("rep:User")
            or {
                propertyEquals("rep:authorizableId", user)
                propertyEquals("rep:principalName", user)
            }
            limit(1)
        }.nodeSequence().firstOrNull() ?: throw AuthException("Cannot find user '$user' for updating password on $instance!")

        try {
            sync.http.postUrlencoded("${node.path}.rw.userprops.html", mapOf(
                ":currentPassword" to currentPassword,
                "rep:password" to newPassword
            ))
        } catch (e: CommonException) {
            throw AuthException("Cannot update password for user '$user' on $instance!")
        }
    }
}
