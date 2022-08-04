package org.eclipse.tractusx.bpdm.pool.component.cdq.service

import org.eclipse.tractusx.bpdm.common.dto.IdentifierDto
import org.eclipse.tractusx.bpdm.common.dto.cdq.BusinessPartnerCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.LegalFormCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.TypeKeyNameCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.TypeKeyNameUrlCdq
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeNameUrlDto
import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.service.CdqMappings
import org.eclipse.tractusx.bpdm.pool.component.cdq.config.CdqIdentifierConfigProperties
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.LegalFormRequest
import org.springframework.stereotype.Service

@Service
class CdqToRequestMapper(
    private val cdqIdentifierConfigProperties: CdqIdentifierConfigProperties
) {
    fun toRequest(partner: BusinessPartnerCdq): BusinessPartnerRequest {
        return BusinessPartnerRequest(
            partner.identifiers.find { it.type?.technicalKey == "BPN" }?.value,
            partner.identifiers.map { CdqMappings.toDto(it) }.plus(toCdqIdentifierRequest(partner.id!!)),
            partner.names.map { CdqMappings.toDto(it) },
            CdqMappings.toOptionalReference(partner.legalForm),
            if (partner.status != null) CdqMappings.toDto(partner.status!!) else null,
            partner.addresses.map { CdqMappings.toDto(it) },
            listOf(),
            CdqMappings.toDto(partner.profile),
            partner.types.map { CdqMappings.toTypeOrDefault<BusinessPartnerType>(it) }.toSet(),
            partner.bankAccounts.map { CdqMappings.toDto(it) }
        )
    }

    fun toCdqIdentifierRequest(idValue: String): IdentifierDto {
        return IdentifierDto(
            idValue,
            cdqIdentifierConfigProperties.typeKey,
            cdqIdentifierConfigProperties.issuerKey,
            cdqIdentifierConfigProperties.statusImportedKey
        )
    }

    fun toRequest(idType: TypeKeyNameUrlCdq): TypeKeyNameUrlDto<String> {
        return TypeKeyNameUrlDto(idType.technicalKey!!, idType.name ?: "", idType.url)
    }

    fun toRequest(idStatus: TypeKeyNameCdq): TypeKeyNameDto<String> {
        return TypeKeyNameDto(idStatus.technicalKey!!, idStatus.name!!)
    }

    fun toRequest(legalForm: LegalFormCdq, partner: BusinessPartnerCdq): LegalFormRequest {
        return LegalFormRequest(
            legalForm.technicalKey,
            legalForm.name!!,
            legalForm.url,
            legalForm.mainAbbreviation,
            CdqMappings.toLanguageCode(legalForm.language),
            partner.categories.map { toCategoryRequest(it) }
        )
    }

    fun toCategoryRequest(category: TypeKeyNameUrlCdq): TypeNameUrlDto {
        return TypeNameUrlDto(category.name!!, category.url)
    }
}