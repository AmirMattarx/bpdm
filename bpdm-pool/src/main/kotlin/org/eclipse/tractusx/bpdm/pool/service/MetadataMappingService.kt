package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.pool.dto.MetadataMappingDto
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerRequest
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierStatus
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierType
import org.eclipse.tractusx.bpdm.pool.entity.IssuingBody
import org.eclipse.tractusx.bpdm.pool.entity.LegalForm
import org.eclipse.tractusx.bpdm.pool.exception.BpdmMultipleNotfound
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierStatusRepository
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.repository.IssuingBodyRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalFormRepository
import org.springframework.stereotype.Service

/**
 * Service for fetching and mapping metadata entities referenced by [BusinessPartnerRequest]
 */
@Service
class MetadataMappingService(
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val identifierStatusRepository: IdentifierStatusRepository,
    private val issuingBodyRepository: IssuingBodyRepository,
    private val legalFormRepository: LegalFormRepository
) {

    /**
     * Fetch metadata entities referenced in [partners] and map them by their referenced keys
     */
    fun mapRequests(partners: Collection<BusinessPartnerRequest>): MetadataMappingDto {
        return MetadataMappingDto(
            mapIdentifierTypes(partners),
            mapIdentifierStatuses(partners),
            mapIssuingBodies(partners),
            mapLegalForms(partners)
        )
    }

    /**
     * Fetch [IdentifierType] referenced in [partners] and map them by their referenced keys
     */
    fun mapIdentifierTypes(partners: Collection<BusinessPartnerRequest>): Map<String, IdentifierType>{
       return  mapIdentifierTypes(partners.flatMap { it.identifiers.map { id -> id.type } }.toSet())
    }

    /**
     * Fetch [IdentifierStatus] referenced in [partners] and map them by their referenced keys
     */
    fun mapIdentifierStatuses(partners: Collection<BusinessPartnerRequest>): Map<String, IdentifierStatus>{
       return mapIdentifierStatuses(partners.flatMap { it.identifiers.mapNotNull { id -> id.status } }.toSet())
    }

    /**
     * Fetch [IssuingBody] referenced in [partners] and map them by their referenced keys
     */
    fun mapIssuingBodies(partners: Collection<BusinessPartnerRequest>): Map<String, IssuingBody>{
        return mapIssuingBodies(partners.flatMap { it.identifiers.mapNotNull{  id -> id.issuingBody } }.toSet())
    }

    /**
     * Fetch [LegalForm] referenced in [partners] and map them by their referenced keys
     */
    fun mapLegalForms(partners: Collection<BusinessPartnerRequest>): Map<String, LegalForm>{
        return mapLegalForms(partners.mapNotNull { it.legalForm }.toSet())
    }


    private fun mapIdentifierTypes(keys: Set<String>): Map<String, IdentifierType>{
        val typeMap = identifierTypeRepository.findByTechnicalKeyIn(keys).associateBy { it.technicalKey }
        assertKeysFound(keys, typeMap)
        return typeMap
    }

    private fun mapIdentifierStatuses(keys: Set<String>): Map<String, IdentifierStatus>{
        val typeMap = identifierStatusRepository.findByTechnicalKeyIn(keys).associateBy { it.technicalKey }
        assertKeysFound(keys, typeMap)
        return typeMap
    }

    private fun mapIssuingBodies(keys: Set<String>): Map<String, IssuingBody>{
        val typeMap = issuingBodyRepository.findByTechnicalKeyIn(keys).associateBy { it.technicalKey }
        assertKeysFound(keys, typeMap)
        return typeMap
    }

    private fun mapLegalForms(keys: Set<String>): Map<String, LegalForm>{
        val typeMap = legalFormRepository.findByTechnicalKeyIn(keys).associateBy { it.technicalKey }
        assertKeysFound(keys, typeMap)
        return typeMap
    }

    private inline fun <reified T> assertKeysFound(keys: Set<String>, typeMap: Map<String, T>){
        val keysNotfound = keys.minus(typeMap.keys)
        if(keysNotfound.isNotEmpty()) throw BpdmMultipleNotfound(T::class.simpleName!!, keysNotfound )
    }

}