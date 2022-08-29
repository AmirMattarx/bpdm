/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.cdq.BusinessPartnerCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.FetchResponse
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.AddressGateInput
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.exception.CdqInvalidRecordException
import org.eclipse.tractusx.bpdm.gate.exception.CdqNonexistentParentException
import org.springframework.stereotype.Service

@Service
class AddressService(
    private val cdqRequestMappingService: CdqRequestMappingService,
    private val inputCdqMappingService: InputCdqMappingService,
    private val cdqClient: CdqClient,
    private val bpnConfigProperties: BpnConfigProperties
) {
    private val logger = KotlinLogging.logger { }

    fun getAddresses(limit: Int, startAfter: String?): PageStartAfterResponse<AddressGateInput> {
        val addressesPage = cdqClient.getAddresses(limit, startAfter)

        val validEntries = addressesPage.values.filter { validateAddressBusinessPartner(it) }

        return PageStartAfterResponse(
            total = addressesPage.total,
            nextStartAfter = addressesPage.nextStartAfter,
            content = validEntries.map { inputCdqMappingService.toInputAddress(it) },
            invalidEntries = addressesPage.values.size - validEntries.size
        )
    }

    fun getAddressByExternalId(externalId: String): AddressGateInput {
        val fetchResponse = cdqClient.getBusinessPartner(externalId)

        when (fetchResponse.status) {
            FetchResponse.Status.OK -> return toValidAddressInput(fetchResponse.businessPartner!!)
            FetchResponse.Status.NOT_FOUND -> throw BpdmNotFoundException("Address", externalId)
        }
    }

    fun upsertAddresses(addresses: Collection<AddressGateInput>) {
        val parentLegalEntitiesByExternalId: Map<String, BusinessPartnerCdq> = getParentLegalEntities(addresses)
        val parentSitesByExternalId: Map<String, BusinessPartnerCdq> = getParentSites(addresses)

        val addressesCdq =
            addresses.map { toCdqModel(it, parentLegalEntitiesByExternalId[it.legalEntityExternalId], parentSitesByExternalId[it.siteExternalId]) }
        cdqClient.upsertAddresses(addressesCdq)

        val legalEntityRelations = toLegalEntityRelations(addresses)
        val siteRelations = toSiteRelations(addresses)
        cdqClient.upsertAddressRelations(legalEntityRelations, siteRelations)
    }

    private fun toSiteRelations(addresses: Collection<AddressGateInput>) = addresses.filter {
        it.siteExternalId != null
    }.map {
        CdqClient.AddressSiteRelation(
            addressExternalId = it.externalId,
            siteExternalId = it.siteExternalId!!
        )
    }.toList()

    private fun toLegalEntityRelations(addresses: Collection<AddressGateInput>) = addresses.filter {
        it.legalEntityExternalId != null
    }.map {
        CdqClient.AddressLegalEntityRelation(
            addressExternalId = it.externalId,
            legalEntityExternalId = it.legalEntityExternalId!!
        )
    }.toList()

    private fun getParentSites(addresses: Collection<AddressGateInput>): Map<String, BusinessPartnerCdq> {
        val parentSiteExternalIds = addresses.mapNotNull { it.siteExternalId }.distinct().toList()
        var parentSitesByExternalId: Map<String, BusinessPartnerCdq> = HashMap()
        if (parentSiteExternalIds.isNotEmpty()) {
            val parentSitesPage = cdqClient.getSites(externalIds = parentSiteExternalIds)
            if (parentSitesPage.limit < parentSiteExternalIds.size) {
                // should not happen as long as configured upsert limit is lower than cdq's limit
                throw IllegalStateException("Could not fetch all parent sites in single request.")
            }
            parentSitesByExternalId = parentSitesPage.values.associateBy { it.externalId!! }
        }
        return parentSitesByExternalId
    }

    private fun getParentLegalEntities(addresses: Collection<AddressGateInput>): Map<String, BusinessPartnerCdq> {
        val parentLegalEntityExternalIds = addresses.mapNotNull { it.legalEntityExternalId }.distinct().toList()
        var parentLegalEntitiesByExternalId: Map<String, BusinessPartnerCdq> = HashMap()
        if (parentLegalEntityExternalIds.isNotEmpty()) {
            val parentLegalEntitiesPage = cdqClient.getLegalEntities(externalIds = parentLegalEntityExternalIds)
            if (parentLegalEntitiesPage.limit < parentLegalEntityExternalIds.size) {
                // should not happen as long as configured upsert limit is lower than cdq's limit
                throw IllegalStateException("Could not fetch all parent legal entities in single request.")
            }
            parentLegalEntitiesByExternalId = parentLegalEntitiesPage.values.associateBy { it.externalId!! }
        }
        return parentLegalEntitiesByExternalId
    }

    private fun toCdqModel(address: AddressGateInput, parentLegalEntity: BusinessPartnerCdq?, parentSite: BusinessPartnerCdq?): BusinessPartnerCdq {
        if (parentLegalEntity == null && parentSite == null) {
            throw CdqNonexistentParentException(address.legalEntityExternalId ?: address.siteExternalId!!)
        }
        val addressCdq = cdqRequestMappingService.toCdqModel(address)
        val parentIdentifiersWithoutBpn = (parentLegalEntity ?: parentSite!!).identifiers.filter { it.type?.technicalKey != bpnConfigProperties.id }
        return addressCdq.copy(identifiers = addressCdq.identifiers.plus(parentIdentifiersWithoutBpn))
    }

    private fun toValidAddressInput(partner: BusinessPartnerCdq): AddressGateInput {
        if (!validateAddressBusinessPartner(partner)) {
            throw CdqInvalidRecordException(partner.id)
        }
        return inputCdqMappingService.toInputAddress(partner)
    }

    private fun validateAddressBusinessPartner(partner: BusinessPartnerCdq): Boolean {
        val logMessageStart = "CDQ business partner for address with ${if (partner.id != null) "CDQ ID " + partner.id else "external id " + partner.externalId}"

        if (partner.addresses.size > 1) {
            logger.warn { "$logMessageStart has multiple addresses" }
        }
        if (partner.addresses.isEmpty()) {
            logger.warn { "$logMessageStart does not have an address" }
            return false
        }

        val numLegalEntityParents = inputCdqMappingService.toParentLegalEntityExternalIds(partner.relations).size
        if (numLegalEntityParents > 1) {
            logger.warn { "$logMessageStart has multiple parent legal entities." }
        }
        val numSiteParents = inputCdqMappingService.toParentSiteExternalIds(partner.relations).size
        if (numSiteParents > 1) {
            logger.warn { "$logMessageStart has multiple parent sites." }
        }

        if (numLegalEntityParents == 0 && numSiteParents == 0) {
            logger.warn { "$logMessageStart does not have a parent legal entity or site." }
        }

        if (numLegalEntityParents > 0 && numSiteParents > 0) {
            logger.warn { "$logMessageStart has both a parent legal entity and a parent site." }
        }

        return true
    }
}