package com.cognifide.gradle.aem.common.file.resolver

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.utils.formats.JsonPassword
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.Serializable

class ResolverOptions(aem: AemExtension) : Serializable {

    var httpUsername: String? = aem.props.string("resolver.http.username")

    @JsonSerialize(using = JsonPassword::class, `as` = String::class)
    var httpPassword: String? = aem.props.string("resolver.http.password")

    var httpConnectionIgnoreSsl: Boolean? = aem.props.boolean("resolver.http.connectionIgnoreSsl")

    var sftpUsername: String? = aem.props.string("resolver.sftp.username")

    @JsonSerialize(using = JsonPassword::class, `as` = String::class)
    var sftpPassword: String? = aem.props.string("resolver.sftp.password")

    var sftpHostChecking = aem.props.boolean("resolver.sftp.hostChecking")

    var smbDomain: String? = aem.props.string("resolver.smb.domain")

    var smbUsername: String? = aem.props.string("resolver.smb.username")

    @JsonSerialize(using = JsonPassword::class, `as` = String::class)
    var smbPassword: String? = aem.props.string("resolver.smb.password")
}