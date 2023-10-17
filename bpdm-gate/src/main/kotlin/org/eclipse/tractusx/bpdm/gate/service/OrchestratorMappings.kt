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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerIdentifierDto
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerStateDto
import org.eclipse.tractusx.bpdm.common.dto.ClassificationDto
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.AlternativePostalAddress
import org.eclipse.tractusx.bpdm.gate.entity.GeographicCoordinate
import org.eclipse.tractusx.bpdm.gate.entity.PhysicalPostalAddress
import org.eclipse.tractusx.bpdm.gate.entity.Street
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.stereotype.Service

@Service
class OrchestratorMappings(
    private val bpnConfigProperties: BpnConfigProperties
) {
    private val logger = KotlinLogging.logger { }
    fun toBusinessPartnerGenericDto(entity: BusinessPartner) = BusinessPartnerGenericDto(
        nameParts = entity.nameParts,
        shortName = entity.shortName,
        identifiers = entity.identifiers.map { toIdentifierDto(it) },
        legalForm = entity.legalForm,
        states = entity.states.map { toStateDto(it) },
        classifications = entity.classifications.map { toClassificationDto(it) },
        roles = entity.roles,
        postalAddress = toPostalAddressDto(entity.postalAddress),
        bpnL = entity.bpnL,
        bpnS = entity.bpnS,
        bpnA = entity.bpnA,
        ownerBpnL = getOwnerBpnL(entity)
    )

    private fun toClassificationDto(entity: Classification) =
        ClassificationDto(type = entity.type, code = entity.code, value = entity.value)

    private fun toPostalAddressDto(entity: PostalAddress) =
        PostalAddressDto(
            addressType = entity.addressType,
            physicalPostalAddress = entity.physicalPostalAddress?.let(::toPhysicalPostalAddressDto),
            alternativePostalAddress = entity.alternativePostalAddress?.let(this::toAlternativePostalAddressDto)
        )

    private fun toPhysicalPostalAddressDto(entity: PhysicalPostalAddress) =
        PhysicalPostalAddressDto(
            geographicCoordinates = entity.geographicCoordinates?.let(::toGeoCoordinateDto),
            country = entity.country,
            administrativeAreaLevel1 = entity.administrativeAreaLevel1,
            administrativeAreaLevel2 = entity.administrativeAreaLevel2,
            administrativeAreaLevel3 = entity.administrativeAreaLevel3,
            postalCode = entity.postalCode,
            city = entity.city,
            district = entity.district,
            street = entity.street?.let(this::toStreetDto),
            companyPostalCode = entity.companyPostalCode,
            industrialZone = entity.industrialZone,
            building = entity.building,
            floor = entity.floor,
            door = entity.door
        )

    private fun toAlternativePostalAddressDto(entity: AlternativePostalAddress): AlternativePostalAddressDto =
        AlternativePostalAddressDto(
            geographicCoordinates = entity.geographicCoordinates?.let(::toGeoCoordinateDto),
            country = entity.country,
            administrativeAreaLevel1 = entity.administrativeAreaLevel1,
            postalCode = entity.postalCode,
            city = entity.city,
            deliveryServiceType = entity.deliveryServiceType,
            deliveryServiceQualifier = entity.deliveryServiceQualifier,
            deliveryServiceNumber = entity.deliveryServiceNumber
        )

    private fun toStreetDto(entity: Street) =
        StreetDto(
            name = entity.name,
            houseNumber = entity.houseNumber,
            milestone = entity.milestone,
            direction = entity.direction,
            namePrefix = entity.namePrefix,
            additionalNamePrefix = entity.additionalNamePrefix,
            nameSuffix = entity.nameSuffix,
            additionalNameSuffix = entity.additionalNameSuffix
        )

    private fun toStateDto(entity: State) =
        BusinessPartnerStateDto(type = entity.type, validFrom = entity.validFrom, validTo = entity.validTo, description = entity.description)

    private fun toIdentifierDto(entity: Identifier) =
        BusinessPartnerIdentifierDto(type = entity.type, value = entity.value, issuingBody = entity.issuingBody)

    private fun toGeoCoordinateDto(entity: GeographicCoordinate) =
        GeoCoordinateDto(latitude = entity.latitude, longitude = entity.longitude, altitude = entity.altitude)

    private fun getOwnerBpnL(entity: BusinessPartner): String? {
        return if (entity.isOwnCompanyData) bpnConfigProperties.ownerBpnL else {
            logger.warn { "Owner BPNL property is not configured" }
            null
        }
    }

    fun toSharingStateType(resultState: ResultState) = when (resultState) {
        ResultState.Pending -> SharingStateType.Pending
        ResultState.Success -> SharingStateType.Success
        ResultState.Error -> SharingStateType.Error
    }
}