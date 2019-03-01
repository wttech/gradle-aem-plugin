package com.cognifide.gradle.aem.common.file.resolver

import com.cognifide.gradle.aem.common.AemExtension
import java.io.Serializable

class ResolverOptions(aem: AemExtension) : Serializable {

    var httpUsername: String? = aem.props.string("aem.resolver.http.username")

    var httpPassword: String? = aem.props.string("aem.resolver.http.password")

    var httpConnectionIgnoreSsl: Boolean? = aem.props.boolean("aem.resolver.http.connectionIgnoreSsl")

    var sftpUsername: String? = aem.props.prop("aem.resolver.sftp.username")

    var sftpPassword: String? = aem.props.prop("aem.resolver.sftp.password")

    var sftpHostChecking = aem.props.boolean("aem.resolver.sftp.hostChecking")

    var smbDomain: String? = aem.props.prop("aem.resolver.smb.domain")

    var smbUsername: String? = aem.props.prop("aem.resolver.smb.username")

    var smbPassword: String? = aem.props.prop("aem.resolver.smb.password")
}