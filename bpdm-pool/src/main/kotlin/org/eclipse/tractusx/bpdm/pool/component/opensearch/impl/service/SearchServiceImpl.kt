package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.component.elastic.SearchService
import org.eclipse.tractusx.bpdm.pool.component.elastic.impl.doc.SuggestionType
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.TextDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.repository.BusinessPartnerDocSearchRepository
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.repository.TextDocSearchRepository
import org.eclipse.tractusx.bpdm.pool.dto.request.BusinessPartnerSearchRequest
import org.eclipse.tractusx.bpdm.pool.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerSearchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SuggestionResponse
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerFetchService
import org.eclipse.tractusx.bpdm.pool.service.toSearchDto
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * Implements search functionality by using OpenSearch
 */
@Service("OpenSearchServiceImpl")
@Primary
class SearchServiceImpl(
    val businessPartnerDocSearchRepository: BusinessPartnerDocSearchRepository,
    val textDocSearchRepository: TextDocSearchRepository,
    val businessPartnerFetchService: BusinessPartnerFetchService,
    val objectMapper: ObjectMapper
) : SearchService {

    private val logger = KotlinLogging.logger { }

    /**
     * Uses the [searchRequest] to perform an OpenSearch query for business partners.
     * The BPNs of found partners are used to query the whole business partner records from the database.
     * The records are supplied with relevancy scores of the search hits and returned as a paginated result.
     * In case BPNs found by OpenSearch can not be found in the database, the [PageResponse] properties are
     * adapted accordingly from the OpenSearch page information
     *
     */
    override fun searchBusinessPartners(
        searchRequest: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<BusinessPartnerSearchResponse> {

        logger.debug { "Search index for business partners" }

        val searchResult = businessPartnerDocSearchRepository.findBySearchRequest(
            searchRequest,
            PageRequest.of(paginationRequest.page, paginationRequest.size)
        )

        logger.debug { "Found ${searchResult.hits.size} business partners in OpenSearch. (${searchResult.totalHits} in total)" }

        val bpnHitMap = searchResult.associateBy { it.id }

        val businessPartners = businessPartnerFetchService.fetchByBpns(bpnHitMap.keys)
        val missingPartners = bpnHitMap.keys.minus(businessPartners.map { it.bpn }.toSet())

        if (missingPartners.isNotEmpty())
            logger.warn { "Some BPNs could not be found in the database: ${missingPartners.joinToString()}" }

        val responseContent =
            businessPartners.map { it.toSearchDto(bpnHitMap[it.bpn]!!.score) }.sortedByDescending { it.score }

        val totalHits = searchResult.totalHits!!.value - missingPartners.size
        val totalPages = totalHits.toInt() / paginationRequest.size

        return PageResponse(totalHits, totalPages, paginationRequest.page, responseContent.size, responseContent)
    }

    /**
     * Query OpenSearch for [field] values by [text] and [filters]
     *
     * The found values and their hit scores are converted to [SuggestionResponse] and returned as a paginated result.
     */
    override fun getSuggestion(
        field: SuggestionType,
        text: String?,
        filters: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {

        logger.debug { "Search index for suggestion type $field" }

        val hits = textDocSearchRepository.findByFieldAndTextAndFilters(
            field,
            text,
            filters,
            PageRequest.of(paginationRequest.page, paginationRequest.size)
        )

        logger.debug { "Returning ${hits.hits.size} suggestions for $field. (${hits.totalHits} found in total)" }

        return PageResponse(
            hits.totalHits!!.value,
            hits.totalHits!!.value.toInt() / paginationRequest.size,
            paginationRequest.page,
            hits.hits.size,
            hits.map { hit ->
                SuggestionResponse(extractTextDocText(hit.sourceAsString), hit.score)
            }.toList()
        )
    }

    private fun extractTextDocText(textDocJson: String): String {
        val textDoc: TextDoc = objectMapper.readValue(textDocJson)
        return textDoc.text
    }
}