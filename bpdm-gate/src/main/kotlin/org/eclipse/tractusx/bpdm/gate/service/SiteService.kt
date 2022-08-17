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

import org.eclipse.tractusx.bpdm.common.dto.cdq.BusinessPartnerCdq
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateInput
import org.eclipse.tractusx.bpdm.gate.exception.CdqNonexistentParentException
import org.springframework.stereotype.Service

@Service
class SiteService(
    private val cdqRequestMappingService: CdqRequestMappingService,
    private val cdqClient: CdqClient
) {
    fun upsertSites(sites: Collection<SiteGateInput>) {
        val parentLegalEntitiesPage = cdqClient.getLegalEntities(externalIds = sites.map { it.legalEntityExternalId }.toList())
        val parentLegalEntitiesByExternalId = parentLegalEntitiesPage.values.associateBy { it.externalId }

        val sitesCdq = sites.map { toCdqModel(it, parentLegalEntitiesByExternalId[it.legalEntityExternalId]) }
        cdqClient.upsertSites(sitesCdq)

        val relations = sites.map {
            CdqClient.SiteLegalEntityRelation(
                siteExternalId = it.externalId,
                legalEntityExternalId = it.legalEntityExternalId
            )
        }.toList()
        cdqClient.upsertSiteRelations(relations)
    }

    fun toCdqModel(site: SiteGateInput, parentLegalEntity: BusinessPartnerCdq?): BusinessPartnerCdq {
        if (parentLegalEntity == null) {
            throw CdqNonexistentParentException(site.legalEntityExternalId)
        }
        val siteCdq = cdqRequestMappingService.toCdqModel(site)
        return siteCdq.copy(identifiers = siteCdq.identifiers.plus(parentLegalEntity.identifiers))
    }
}