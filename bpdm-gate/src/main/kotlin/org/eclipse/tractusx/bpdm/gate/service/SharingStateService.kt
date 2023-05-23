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

package org.eclipse.tractusx.bpdm.gate.service

import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.eclipse.tractusx.bpdm.gate.entity.SharingState
import org.eclipse.tractusx.bpdm.gate.repository.SharingStaterRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class SharingStateService(private val stateRepository: SharingStaterRepository) {

    fun upsertSharingState(request: SharingStateDto) {

        val sharingState = this.stateRepository.findByExternalIdAndLsaType(request.externalId, request.lsaType)
        if (sharingState == null) {
            insertSharingState(request)
        } else {
            updateSharingState(sharingState, request)
        }
    }

    private fun insertSharingState(dto: SharingStateDto) {

        this.stateRepository.save(
            SharingState(
                externalId = dto.externalId,
                lsaType = dto.lsaType,
                sharingStateType = dto.sharingStateType,
                sharingErrorCode = dto.sharingErrorCode,
                sharingErrorMessage = dto.sharingErrorMessage,
                bpn = dto.bpn,
                sharingProcessStarted = dto.sharingProcessStarted
            )
        )
    }

    private fun updateSharingState(entity: SharingState, dto: SharingStateDto) {

        entity.sharingStateType = dto.sharingStateType
        entity.sharingErrorCode = dto.sharingErrorCode
        entity.sharingErrorMessage = dto.sharingErrorMessage
        entity.bpn = dto.bpn
        entity.sharingProcessStarted = dto.sharingProcessStarted

        this.stateRepository.save(entity)
    }

    fun findSharingStates(paginationRequest: PaginationRequest, lsaType: LsaType, externalIds: Collection<String>): PageResponse<SharingStateDto> {

        val pageable = PageRequest.of(paginationRequest.page, paginationRequest.size)
        val page = this.stateRepository.findByExternalIdInAndLsaType(externalIds, lsaType, pageable)

        return page.toDto(page.content.map {
            SharingStateDto(
                externalId = it.externalId,
                lsaType = it.lsaType,
                sharingStateType = it.sharingStateType,
                sharingErrorCode = it.sharingErrorCode,
                sharingErrorMessage = it.sharingErrorMessage,
                bpn = it.bpn,
                sharingProcessStarted = it.sharingProcessStarted
            )
        })

    }
}