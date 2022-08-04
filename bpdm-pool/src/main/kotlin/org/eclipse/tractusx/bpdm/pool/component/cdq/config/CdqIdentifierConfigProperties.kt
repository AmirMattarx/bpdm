package org.eclipse.tractusx.bpdm.pool.component.cdq.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm.cdq.id")
@ConstructorBinding
class CdqIdentifierConfigProperties (
    val typeKey: String = "CDQID",
    val typeName: String = "CDQ Identifier",
    val statusImportedKey: String = "CDQ_IMPORTED",
    val statusImportedName: String = "Imported from CDQ",
    val issuerKey: String = "CDQ",
    val issuerName: String = "CDQ AG"
)