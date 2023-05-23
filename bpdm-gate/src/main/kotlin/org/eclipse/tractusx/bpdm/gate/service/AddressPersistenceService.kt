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

import jakarta.transaction.Transactional
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.entity.LogisticAddress
import org.eclipse.tractusx.bpdm.gate.repository.GateAddressRepository
import org.eclipse.tractusx.bpdm.gate.repository.LegalEntityRepository
import org.springframework.stereotype.Service

@Service
class AddressPersistenceService(private val gateAddressRepository: GateAddressRepository,
                                private val legalEntityRepository: LegalEntityRepository) {

    @Transactional
    fun persistAddressBP(addresses: Collection<AddressGateInputRequest>) {

        val externalIdColl: MutableCollection<String> = mutableListOf()
        addresses.forEach { externalIdColl.add(it.externalId) }

        val addressRecord = gateAddressRepository.findByExternalIdIn(externalIdColl)

        addresses.forEach { address ->
            val legalEntityRecord = address.legalEntityExternalId?.let {legalEntityRepository.findByExternalId(it)?:throw BpdmNotFoundException("Business Partner", it)  }

            val fullAddress = address.toAddressGate(legalEntityRecord)
            addressRecord.find { it.externalId == address.externalId }?.let { existingAddress ->
                updateAddress(existingAddress, fullAddress)
                gateAddressRepository.save(existingAddress)
            } ?: run {
                gateAddressRepository.save(fullAddress)
            }
        }
    }

    private fun updateAddress(address: LogisticAddress, changeAddress: LogisticAddress) {

        address.name = changeAddress.name
        address.bpn = changeAddress.bpn
        address.externalId = changeAddress.externalId
        address.legalEntity= changeAddress.legalEntity
        address.siteExternalId = changeAddress.siteExternalId
        address.physicalPostalAddress = changeAddress.physicalPostalAddress
        address.alternativePostalAddress = changeAddress.alternativePostalAddress

        address.identifiers.replace(changeAddress.identifiers)
        address.states.replace(changeAddress.states)

    }

}