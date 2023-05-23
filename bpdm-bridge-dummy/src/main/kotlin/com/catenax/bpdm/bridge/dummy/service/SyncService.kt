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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.*
import org.eclipse.tractusx.bpdm.gate.api.model.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime

@Service
class SyncService(
    val poolClient: PoolApiClient,
    val gateClient: GateClient
) {

    private val logger = KotlinLogging.logger { }

    fun sync() {
        // Check changelog entries from Gate (after last sync time)
        val externalIdsByType = getChangedExternalIdsByLsaTypeFromGate(null)
        // TODO persist syncAfter=LocalDateTime.now()

        externalIdsByType[LsaType.LegalEntity]?.let { syncLegalEntities(it) }
        externalIdsByType[LsaType.Site]?.let { syncSites(it) }
        externalIdsByType[LsaType.Address]?.let { syncAddresses(it) }
    }

    private fun getChangedExternalIdsByLsaTypeFromGate(modifiedAfter: Instant?): Map<LsaType, Collection<String>> {
        // TODO use pagination properly
        val entriesGate = gateClient.changelog().getChangelogEntriesLsaType(
            lsaType = null,
            fromTime = modifiedAfter,
            paginationRequest = PaginationRequest(0, 100)
        )

        return entriesGate.content
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

    private fun syncLegalEntities(externalIdsRequested: Collection<String>) {
        // Retrieve business partners (LSA) from Gate
        val entries = getLegalEntitiesFromGate(externalIdsRequested)
        val (entriesToCreate, entriesToUpdate) = entries.partition { it.bpn == null }

        // Create or update (LSAs) in Pool
        if (entriesToCreate.isNotEmpty()) {
            createLegalEntitiesInPool(entriesToCreate)
        }
        if (entriesToUpdate.isNotEmpty()) {
            updateLegalEntitiesInPool(entriesToUpdate)
        }
    }

    private fun syncSites(externalIdsRequested: Collection<String>) {
        // Retrieve business partners (LSA) from Gate
        val entries = getSitesFromGate(externalIdsRequested)
        val (entriesToCreate, entriesToUpdate) = entries.partition { it.bpn == null }

        // Create or update (LSAs) in Pool
        if (entriesToCreate.isNotEmpty()) {
            createSitesInPool(entriesToCreate)
        }
        if (entriesToUpdate.isNotEmpty()) {
            updateSitesInPool(entriesToUpdate)
        }
    }

    private fun syncAddresses(externalIdsRequested: Collection<String>) {
        // Retrieve business partners (LSA) from Gate
        val entries = getAddressesFromGate(externalIdsRequested)
        val (entriesToCreate, entriesToUpdate) = entries.partition { it.bpn == null }

        // Create or update (LSAs) in Pool
        if (entriesToCreate.isNotEmpty()) {
            createAddressesInPool(entriesToCreate)
        }
        if (entriesToUpdate.isNotEmpty()) {
            updateAddressesInPool(entriesToUpdate)
        }
    }

    private fun getLegalEntitiesFromGate(externalIds: Collection<String>): Collection<LegalEntityGateInputResponse> {
        if (externalIds.isEmpty()) {
            return emptyList()
        }
        // TODO use pagination properly
        val response = gateClient.legalEntities().getLegalEntitiesByExternalIds(
            externalIds = externalIds,
            paginationRequest = PaginationStartAfterRequest(null, 100)
        )
        logger.info { "Gate returned ${response.content.size} valid legal entities, ${response.invalidEntries} were invalid" }
        return response.content
    }

    private fun getSitesFromGate(externalIds: Collection<String>): Collection<SiteGateInputResponse> {
        if (externalIds.isEmpty()) {
            return emptyList()
        }
        // TODO use pagination properly
        val response = gateClient.sites().getSitesByExternalIds(
            externalIds = externalIds,
            paginationRequest = PaginationStartAfterRequest(null, 100)
        )
        logger.info { "Gate returned ${response.content.size} valid sites, ${response.invalidEntries} were invalid" }
        return response.content
    }

    private fun getAddressesFromGate(externalIds: Collection<String>): Collection<AddressGateInputResponse> {
        if (externalIds.isEmpty()) {
            return emptyList()
        }
        // TODO use pagination properly
        val response = gateClient.addresses().getAddressesByExternalIds(
            externalIds = externalIds,
            paginationRequest = PaginationStartAfterRequest(null, 100)
        )
        logger.info { "Gate returned ${response.content.size} valid addresses, ${response.invalidEntries} were invalid" }
        return response.content
    }

    private fun createLegalEntitiesInPool(entriesToCreate: Collection<LegalEntityGateInputResponse>) {
        val createRequests = entriesToCreate.map {
            LegalEntityPartnerCreateRequest(
                legalEntity = it.legalEntity,
                index = it.externalId
            )
        }

        val responseWrapper = poolClient.legalEntities().createBusinessPartners(createRequests)
        logger.info { "Pool accepted ${responseWrapper.entityCount} new legal entities, ${responseWrapper.errorCount} were refused" }

        handleLegalEntityCreateResponse(responseWrapper)
    }

    private fun updateLegalEntitiesInPool(entriesToUpdate: Collection<LegalEntityGateInputResponse>) {
        val updateRequests = entriesToUpdate.map {
            LegalEntityPartnerUpdateRequest(
                legalEntity = it.legalEntity,
                bpn = it.bpn!!
            )
        }

        val responseWrapper = poolClient.legalEntities().updateBusinessPartners(updateRequests)
        logger.info { "Pool accepted ${responseWrapper.entityCount} updated legal entities, ${responseWrapper.errorCount} were refused" }

        val externalIdByBpn = entriesToUpdate.associateBy { it.bpn!! }.mapValues { (_, entry) -> entry.externalId }
        handleLegalEntityUpdateResponse(responseWrapper, externalIdByBpn)
    }

    private fun createSitesInPool(entriesToCreate: List<SiteGateInputResponse>) {
        val leParentsByExternalId = entriesToCreate
            .map { it.legalEntityExternalId }
            .let { getLegalEntitiesFromGate(it) }
            .associateBy { it.externalId }
        val createRequests = entriesToCreate.mapNotNull { entry ->
            leParentsByExternalId[entry.legalEntityExternalId]?.bpn
                ?.let { leParentBpn ->
                    SitePartnerCreateRequest(
                        site = entry.site,
                        index = entry.externalId,
                        bpnParent = leParentBpn
                    )
                }
        }

        if (createRequests.size != entriesToCreate.size) {
            logger.warn {
                "Not all found Gate sites (${entriesToCreate.size}) are passed to the Pool (only ${createRequests.size}) " +
                        "because some parent BPN-L entries are missing!"
            }
        }
        val responseWrapper = poolClient.sites().createSite(createRequests)
        logger.info { "Pool accepted ${responseWrapper.entityCount} new sites, ${responseWrapper.errorCount} were refused" }

        handleSiteCreateResponse(responseWrapper)
    }

    private fun updateSitesInPool(entriesToUpdate: List<SiteGateInputResponse>) {
        val updateRequests = entriesToUpdate.map {
            SitePartnerUpdateRequest(
                site = it.site,
                bpn = it.bpn!!
            )
        }

        val responseWrapper = poolClient.sites().updateSite(updateRequests)
        logger.info { "Pool accepted ${responseWrapper.entityCount} updated sites, ${responseWrapper.errorCount} were refused" }

        val externalIdByBpn = entriesToUpdate.associateBy { it.bpn!! }.mapValues { (_, entry) -> entry.externalId }
        handleSiteUpdateResponse(responseWrapper, externalIdByBpn)
    }

    private fun createAddressesInPool(entriesToCreate: List<AddressGateInputResponse>) {
        val leParentsByExternalId = entriesToCreate
            .mapNotNull { it.legalEntityExternalId }
            .let { getLegalEntitiesFromGate(it) }
            .associateBy { it.externalId }
        val leParentsCreateRequests = entriesToCreate.mapNotNull { entry ->
            leParentsByExternalId[entry.legalEntityExternalId]?.bpn
                ?.let { leParentBpn ->
                    AddressPartnerCreateRequest(
                        address = entry.address,
                        index = entry.externalId,
                        bpnParent = leParentBpn
                    )
                }
        }

        val siteParentsByExternalId = entriesToCreate
            .mapNotNull { it.siteExternalId }
            .let { getSitesFromGate(it) }
            .associateBy { it.externalId }
        val siteParentsCreateRequests = entriesToCreate.mapNotNull { entry ->
            siteParentsByExternalId[entry.siteExternalId]?.bpn
                ?.let { siteParentBpn ->
                    AddressPartnerCreateRequest(
                        address = entry.address,
                        index = entry.externalId,
                        bpnParent = siteParentBpn
                    )
                }
        }

        val createRequests = leParentsCreateRequests.plus(siteParentsCreateRequests)
        if (createRequests.size != entriesToCreate.size) {
            logger.warn {
                "Not all found Gate addresses (${entriesToCreate.size}) are passed to the Pool (only ${createRequests.size}) " +
                        "because some parent BPN-L/S entries are missing!"
            }
        }
        val responseWrapper = poolClient.addresses().createAddresses(createRequests)
        logger.info { "Pool accepted ${responseWrapper.entityCount} new addresses, ${responseWrapper.errorCount} were refused" }

        handleAddressCreateResponse(responseWrapper)
    }

    private fun updateAddressesInPool(entriesToUpdate: List<AddressGateInputResponse>) {
        val updateRequests = entriesToUpdate.map {
            AddressPartnerUpdateRequest(
                address = it.address,
                bpn = it.bpn!!
            )
        }

        val responseWrapper = poolClient.addresses().updateAddresses(updateRequests)
        logger.info { "Pool accepted ${responseWrapper.entityCount} updated addresses, ${responseWrapper.errorCount} were refused" }

        val externalIdByBpn = entriesToUpdate.associateBy { it.bpn!! }.mapValues { (_, entry) -> entry.externalId }
        handleAddressUpdateResponse(responseWrapper, externalIdByBpn)
    }


    // TODO Update BPN and status in Gate via new endpoint for create/update and L/S/A.
    //  It's very important to update the BPN in the Gate.
    //  Otherwise duplicates may be created in the Pool (or the request fails because of uniqueness constraints).
    //  For improved robustness we should maybe persistently track all sync entries (status) by LSAType/externalID.

    private fun handleLegalEntityCreateResponse(
        responseWrapper: LegalEntityPartnerCreateResponseWrapper
    ) {
        for (entity in responseWrapper.entities) {
            buildSuccessSharingStateDto(LsaType.LegalEntity, entity.index, entity.legalEntity.bpn)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        for (errorInfo in responseWrapper.errors) {
            // entityKey should be an externalId
            buildErrorSharingStateDto(LsaType.LegalEntity, errorInfo.entityKey, errorInfo, true)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        logger.info { "Sharing states for ${responseWrapper.entityCount} valid and ${responseWrapper.errorCount} invalid new legal entities were updated in the Gate" }
    }

    private fun handleLegalEntityUpdateResponse(
        responseWrapper: LegalEntityPartnerUpdateResponseWrapper,
        externalIdByBpn: Map<String, String>
    ) {
        for (errorInfo in responseWrapper.errors) {
            // entityKey should be a BPN
            val externalId = externalIdByBpn[errorInfo.entityKey]
            buildErrorSharingStateDto(LsaType.LegalEntity, externalId, errorInfo, false)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        logger.info { "Sharing states for ${responseWrapper.errorCount} invalid modified legal entities were updated in the Gate" }
    }

    private fun handleSiteCreateResponse(
        responseWrapper: SitePartnerCreateResponseWrapper
    ) {
        for (entity in responseWrapper.entities) {
            buildSuccessSharingStateDto(LsaType.Site, entity.index, entity.site.bpn)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        for (errorInfo in responseWrapper.errors) {
            // entityKey should be an externalId
            buildErrorSharingStateDto(LsaType.Site, errorInfo.entityKey, errorInfo, true)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        logger.info { "Sharing states for ${responseWrapper.entityCount} valid and ${responseWrapper.errorCount} invalid new sites were updated in the Gate" }
    }

    private fun handleSiteUpdateResponse(
        responseWrapper: SitePartnerUpdateResponseWrapper,
        externalIdByBpn: Map<String, String>
    ) {
        for (errorInfo in responseWrapper.errors) {
            // entityKey should be a BPN
            val externalId = externalIdByBpn[errorInfo.entityKey]
            buildErrorSharingStateDto(LsaType.Site, externalId, errorInfo, false)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        logger.info { "Sharing states for ${responseWrapper.errorCount} invalid modified sites were updated in the Gate" }
    }

    private fun handleAddressCreateResponse(
        responseWrapper: AddressPartnerCreateResponseWrapper
    ) {
        for (entity in responseWrapper.entities) {
            buildSuccessSharingStateDto(LsaType.Address, entity.index, entity.address.bpn)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        for (errorInfo in responseWrapper.errors) {
            // entityKey should be an externalId
            buildErrorSharingStateDto(LsaType.Address, errorInfo.entityKey, errorInfo, true)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        logger.info { "Sharing states for ${responseWrapper.entityCount} valid and ${responseWrapper.errorCount} invalid new addresses were updated in the Gate" }
    }

    private fun handleAddressUpdateResponse(
        responseWrapper: AddressPartnerUpdateResponseWrapper,
        externalIdByBpn: Map<String, String>
    ) {
        for (errorInfo in responseWrapper.errors) {
            // entityKey should be a BPN
            val externalId = externalIdByBpn[errorInfo.entityKey]
            buildErrorSharingStateDto(LsaType.Address, externalId, errorInfo, false)
                ?.let { gateClient.sharingState().upsertSharingState(it) }
        }
        logger.info { "Sharing states for ${responseWrapper.errorCount} invalid modified addresses were updated in the Gate" }
    }

    private fun buildSuccessSharingStateDto(lsaType: LsaType, index: String?, bpn: String): SharingStateDto? {
        if (index == null) {
            logger.warn { "Encountered index=null in Pool response for $bpn, can't update the Gate sharing state" }
            return null
        }
        return SharingStateDto(
            lsaType = lsaType,
            externalId = index,
            sharingStateType = SharingStateType.Success,
            bpn = bpn,
            sharingProcessStarted = LocalDateTime.now()
        )
    }

    private fun buildErrorSharingStateDto(lsaType: LsaType, externalId: String?, errorInfo: ErrorInfo<*>, processStarted: Boolean): SharingStateDto? {
        if (externalId == null) {
            logger.warn { "Couldn't determine externalId for $errorInfo, can't update the Gate sharing state" }
            return null
        }
        return SharingStateDto(
            lsaType = lsaType,
            externalId = externalId,
            sharingStateType = SharingStateType.Error,
            sharingErrorCode = BusinessPartnerSharingError.SharingProcessError,
            sharingErrorMessage = "${errorInfo.message} (${errorInfo.errorCode})",
            sharingProcessStarted = if (processStarted) LocalDateTime.now() else null
        )
    }
}