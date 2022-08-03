package org.eclipse.tractusx.bpdm.gate.service

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.model.AddressType
import org.eclipse.tractusx.bpdm.gate.config.CdqConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInput
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.exception.CdqInvalidRecordException
import org.eclipse.tractusx.bpdm.gate.exception.CdqRequestException
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

private const val BUSINESS_PARTNER_PATH = "/businesspartners"
private const val FETCH_BUSINESS_PARTNER_PATH = "$BUSINESS_PARTNER_PATH/fetch"

@Service
class LegalEntityService(
    private val webClient: WebClient,
    private val cdqRequestMappingService: CdqRequestMappingService,
    private val inputCdqMappingService: InputCdqMappingService,
    private val cdqConfigProperties: CdqConfigProperties,
    private val objectMapper: ObjectMapper
) {

    private val logger = KotlinLogging.logger { }

    fun upsertLegalEntities(legalEntities: Collection<LegalEntityGateInput>) {

        val legalEntitiesCdq = legalEntities.map { cdqRequestMappingService.toCdqModel(it) }
        val upsertRequest =
            UpsertRequest(
                cdqConfigProperties.datasource,
                legalEntitiesCdq,
                listOf(UpsertRequest.CdqFeatures.UPSERT_BY_EXTERNAL_ID, UpsertRequest.CdqFeatures.API_ERROR_ON_FAILURES)
            )

        try {
            webClient
                .put()
                .uri(BUSINESS_PARTNER_PATH)
                .bodyValue(objectMapper.writeValueAsString(upsertRequest))
                .retrieve()
                .bodyToMono<UpsertResponse>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Upsert business partners request failed.", e)
        }
    }

    fun getLegalEntityByExternalId(externalId: String): LegalEntityGateInput {
        val fetchRequest = FetchRequest(cdqConfigProperties.datasource, externalId)

        val fetchResponse = try {
            webClient
                .post()
                .uri(FETCH_BUSINESS_PARTNER_PATH)
                .bodyValue(objectMapper.writeValueAsString(fetchRequest))
                .retrieve()
                .bodyToMono<FetchResponse>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Fetch business partners request failed.", e)
        }

        when (fetchResponse.status) {
            FetchResponse.Status.OK -> return toValidLegalEntityInput(fetchResponse.businessPartner!!)
            FetchResponse.Status.NOT_FOUND -> throw BpdmNotFoundException("Legal Entity", externalId)
        }
    }

    fun getLegalEntities(limit: Int, startAfter: String?): PageStartAfterResponse<LegalEntityGateInput> {
        val partnerCollection = try {
            webClient
                .get()
                .uri { builder ->
                    builder
                        .path(BUSINESS_PARTNER_PATH)
                        .queryParam("limit", limit)
                        .queryParam("datasource", cdqConfigProperties.datasource)
                        .queryParam("featuresOn", "USE_NEXT_START_AFTER")
                    if (startAfter != null) builder.queryParam("startAfter", startAfter)
                    builder.build()
                }
                .retrieve()
                .bodyToMono<BusinessPartnerCollectionCdq>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Get business partners request failed.", e)
        }

        val validEntries = partnerCollection.values.filter { validateBusinessPartner(it) }

        return PageStartAfterResponse(
            total = partnerCollection.total,
            nextStartAfter = partnerCollection.nextStartAfter,
            content = validEntries.map { inputCdqMappingService.toInput(it) },
            invalidEntries = partnerCollection.values.size - validEntries.size
        )
    }

    private fun toValidLegalEntityInput(partner: BusinessPartnerCdq): LegalEntityGateInput {
        if (!validateBusinessPartner(partner)) {
            throw CdqInvalidRecordException(partner.id)
        }
        return inputCdqMappingService.toInput(partner)
    }

    private fun validateBusinessPartner(partner: BusinessPartnerCdq): Boolean {
        if (!partner.addresses.any { address -> address.types.any { type -> type.technicalKey == AddressType.LEGAL.name } }) {
            logger.warn { "CDQ business partner with CDQ ID ${partner.id} does not have legal address" }
            return false
        }

        return true
    }
}