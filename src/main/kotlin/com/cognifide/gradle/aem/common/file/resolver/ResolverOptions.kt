package com.cognifide.gradle.aem.common.file.resolver

import com.cognifide.gradle.aem.common.AemExtension
import java.io.Serializable

class ResolverOptions(aem: AemExtension) : Serializable {

    var httpUsername: String? = aem.props.string("resolver.http.username")

    var httpPassword: String? = aem.props.string("resolver.http.password")

    var httpConnectionIgnoreSsl: Boolean? = aem.props.boolean("resolver.http.connectionIgnoreSsl")

    var sftpUsername: String? = aem.props.prop("resolver.sftp.username")

    var sftpPassword: String? = aem.props.prop("resolver.sftp.password")

    var sftpHostChecking = aem.props.boolean("resolver.sftp.hostChecking")

    var smbDomain: String? = aem.props.prop("resolver.smb.domain")

    var smbUsername: String? = aem.props.prop("resolver.smb.username")

    var smbPassword: String? = aem.props.prop("resolver.smb.password")
}