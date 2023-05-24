/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package com.catenax.bpdm.bridge.dummy.service

import com.catenax.bpdm.bridge.dummy.dto.GateAddressInfo
import com.catenax.bpdm.bridge.dummy.dto.GateLegalEntityInfo
import com.catenax.bpdm.bridge.dummy.dto.GateSiteInfo
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.springframework.stereotype.Service
import java.time.Instant

private const val pageSize = 100

@Service
class GateQueryService(
    val gateClient: GateClient
) {

    private val logger = KotlinLogging.logger { }

    fun getChangedExternalIdsByLsaType(modifiedAfter: Instant?): Map<LsaType, Set<String>> {
        var page = 0
        var totalPages: Int
        val content = mutableListOf<ChangelogResponse>()

        do {
            val pageResponse = gateClient.changelog().getChangelogEntriesLsaType(
                lsaType = null,
                fromTime = modifiedAfter,
                paginationRequest = PaginationRequest(page, pageSize)
            )
            page++
            totalPages = pageResponse.totalPages
            content.addAll(pageResponse.content)
        } while (page < totalPages)

        return content
            .groupBy { it.businessPartnerType }
            .mapValues { (_, list) -> list.map { it.externalId }.toSet() }
            .also {
                logger.info {
                    "Changed entries in Gate since last sync: " +
                            "${it[LsaType.LegalEntity]?.size ?: 0} legal entities, " +
                            "${it[LsaType.Site]?.size ?: 0} sites, " +
                            "${it[LsaType.Address]?.size ?: 0} addresses"
                }
            }
    }

    fun getLegalEntityInfos(externalIds: Set<String>): Collection<GateLegalEntityInfo> {
        val entries = getLegalEntitiesInput(externalIds)
        val bpnByExternalId = getBpnByExternalId(LsaType.LegalEntity, externalIds)

        return entries.map {
            GateLegalEntityInfo(
                legalEntity = it.legalEntity,
                externalId = it.externalId,
                bpn = bpnByExternalId[it.externalId]
            )
        }
    }

    fun getSiteInfos(externalIds: Set<String>): Collection<GateSiteInfo> {
        val entries = getSitesInput(externalIds)
        val bpnByExternalId = getBpnByExternalId(LsaType.Site, externalIds)

        return entries.map {
            GateSiteInfo(
                site = it.site,
                externalId = it.externalId,
                legalEntityExternalId = it.legalEntityExternalId,
                bpn = bpnByExternalId[it.externalId]
            )
        }
    }

    fun getAddressInfos(externalIds: Set<String>): Collection<GateAddressInfo> {
        val entries = getAddressesInput(externalIds)
        val bpnByExternalId = getBpnByExternalId(LsaType.Address, externalIds)

        return entries.map {
            GateAddressInfo(
                address = it.address,
                externalId = it.externalId,
                legalEntityExternalId = it.legalEntityExternalId,
                siteExternalId = it.siteExternalId,
                bpn = bpnByExternalId[it.externalId]
            )
        }
    }

    fun getBpnByExternalId(lsaType: LsaType, externalIds: Set<String>): Map<String, String> {
        if (externalIds.isEmpty()) {
            return emptyMap()
        }

        var page = 0
        var totalPages: Int
        val content = mutableListOf<SharingStateDto>()

        do {
            val pageResponse = gateClient.sharingState().getSharingStates(
                lsaType = lsaType,
                externalIds = externalIds,
                paginationRequest = PaginationRequest(page, pageSize)
            )
            page++
            totalPages = pageResponse.totalPages
            content.addAll(pageResponse.content)
        } while (page < totalPages)

        return content
            .associateBy { it.externalId }
            .filter { it.value.bpn != null }
            .mapValues { it.value.bpn!! }
    }

    private fun getLegalEntitiesInput(externalIds: Set<String>): Collection<LegalEntityGateInputResponse> {
        if (externalIds.isEmpty()) {
            return emptyList()
        }

        var pageStartAfter: String? = null
        val validContent = mutableListOf<LegalEntityGateInputResponse>()
        var invalidEntries = 0

        do {
            val pageResponse = gateClient.legalEntities().getLegalEntitiesByExternalIds(
                externalIds = externalIds,
                paginationRequest = PaginationStartAfterRequest(pageStartAfter, pageSize)
            )
            pageStartAfter = pageResponse.nextStartAfter
            validContent.addAll(pageResponse.content)
            invalidEntries += pageResponse.invalidEntries
        } while (pageStartAfter != null)

        logger.info { "Gate returned ${validContent.size} valid legal entities, $invalidEntries were invalid" }
        return validContent
    }

    private fun getSitesInput(externalIds: Set<String>): Collection<SiteGateInputResponse> {
        if (externalIds.isEmpty()) {
            return emptyList()
        }

        var pageStartAfter: String? = null
        val validContent = mutableListOf<SiteGateInputResponse>()
        var invalidEntries = 0

        do {
            val pageResponse = gateClient.sites().getSitesByExternalIds(
                externalIds = externalIds,
                paginationRequest = PaginationStartAfterRequest(pageStartAfter, pageSize)
            )
            pageStartAfter = pageResponse.nextStartAfter
            validContent.addAll(pageResponse.content)
            invalidEntries += pageResponse.invalidEntries
        } while (pageStartAfter != null)

        logger.info { "Gate returned ${validContent.size} valid sites, $invalidEntries were invalid" }
        return validContent
    }

    private fun getAddressesInput(externalIds: Set<String>): Collection<AddressGateInputResponse> {
        if (externalIds.isEmpty()) {
            return emptyList()
        }

        var pageStartAfter: String? = null
        val validContent = mutableListOf<AddressGateInputResponse>()
        var invalidEntries = 0

        do {
            val pageResponse = gateClient.addresses().getAddressesByExternalIds(
                externalIds = externalIds,
                paginationRequest = PaginationStartAfterRequest(pageStartAfter, pageSize)
            )
            pageStartAfter = pageResponse.nextStartAfter
            validContent.addAll(pageResponse.content)
            invalidEntries += pageResponse.invalidEntries
        } while (pageStartAfter != null)

        logger.info { "Gate returned ${validContent.size} valid addresses, $invalidEntries were invalid" }
        return validContent
    }

}
