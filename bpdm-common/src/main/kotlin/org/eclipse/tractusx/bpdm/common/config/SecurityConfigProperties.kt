package org.eclipse.tractusx.bpdm.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm.security")
@ConstructorBinding
data class SecurityConfigProperties(
    val enabled: Boolean = false,
    val authUrl: String = "",
    val tokenUrl:String = "",
    val refreshUrl: String = "",
    val corsOrigins: Collection<String> = emptyList()
)
