package com.catenax.gpdm.component.elastic.mock.service

import com.catenax.gpdm.component.elastic.SearchService
import com.catenax.gpdm.component.elastic.impl.doc.SuggestionType
import com.catenax.gpdm.dto.request.BusinessPartnerSearchRequest
import com.catenax.gpdm.dto.request.PaginationRequest
import com.catenax.gpdm.dto.response.BusinessPartnerSearchResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.dto.response.SuggestionResponse
import com.catenax.gpdm.repository.BusinessPartnerRepository
import com.catenax.gpdm.service.toDto
import com.catenax.gpdm.service.toSearchDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class SearchServiceMock(
    val businessPartnerRepository: BusinessPartnerRepository
): SearchService {
    override fun searchBusinessPartners(
        searchRequest: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<BusinessPartnerSearchResponse> {
        val resultPage = businessPartnerRepository.findAll(PageRequest.of(paginationRequest.page, paginationRequest.size))
        return resultPage.toDto(resultPage.content.map { it.toSearchDto(1f) })
    }

    override fun getSuggestion(
        field: SuggestionType,
        text: String?,
        searchRequest: BusinessPartnerSearchRequest,
        paginationRequest: PaginationRequest
    ): PageResponse<SuggestionResponse> {
        val emptyPage =  Page.empty<SuggestionResponse>(PageRequest.of(paginationRequest.page, paginationRequest.size))
        return emptyPage.toDto(emptyPage.content)
    }
}