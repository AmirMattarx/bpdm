package com.catenax.gpdm.config

import com.catenax.gpdm.component.cdq.CdqAdapterConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ConditionalOnProperty(
    value = ["bpdm.cdq.enabled"],
    havingValue = "true",
    matchIfMissing = false)
@Import(CdqAdapterConfig::class)
class CdqEnabledConfig