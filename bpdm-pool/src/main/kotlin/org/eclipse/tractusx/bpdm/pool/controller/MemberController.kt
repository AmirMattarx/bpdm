/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.controller

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.PoolMembersApi
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ChangelogEntryVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.springframework.web.bind.annotation.RestController

@RestController
class MemberController : PoolMembersApi {
    override fun searchLegalEntities(
        searchRequest: LegalEntitySearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<LegalEntityWithLegalAddressVerboseDto> {
        TODO("Not yet implemented")
    }

    override fun searchSites(searchRequest: SiteSearchRequest, paginationRequest: PaginationRequest): PageDto<SiteWithMainAddressVerboseDto> {
        TODO("Not yet implemented")
    }

    override fun searchAddresses(searchRequest: AddressSearchRequest, paginationRequest: PaginationRequest): PageDto<LogisticAddressVerboseDto> {
        TODO("Not yet implemented")
    }

    override fun searchChangelogEntries(
        changelogSearchRequest: ChangelogSearchRequest,
        paginationRequest: PaginationRequest
    ): PageDto<ChangelogEntryVerboseDto> {
        TODO("Not yet implemented")
    }

}