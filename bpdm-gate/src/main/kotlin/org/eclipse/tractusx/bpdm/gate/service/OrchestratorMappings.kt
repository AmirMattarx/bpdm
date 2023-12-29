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
import org.eclipse.tractusx.bpdm.common.dto.GeoCoordinateDto
import org.eclipse.tractusx.bpdm.common.model.StageType
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
        identifiers = entity.identifiers.map { toIdentifierDto(it) },
        states = entity.states.map { toStateDto(it) },
        roles = entity.roles,
        ownerBpnL = getOwnerBpnL(entity),
        legalEntity = toLegalEntityComponentDto(entity),
        site = toSiteComponentDto(entity),
        address = toAddressComponentDto(entity.bpnA, entity.postalAddress)

    )

    private fun toLegalEntityComponentDto(entity: BusinessPartner) = LegalEntityComponent(
        bpnL = entity.bpnL,
        legalName = entity.legalName,
        shortName = entity.shortName,
        legalForm = entity.legalForm,
        classifications = entity.classifications.map { toClassificationDto(it) }
    )

    private fun toSiteComponentDto(entity: BusinessPartner) = SiteComponent(
        bpnS = entity.bpnS,
    )

    private fun toAddressComponentDto(bpnA: String?, entity: PostalAddress) = AddressComponent(
        bpnA = bpnA,
        addressType = entity.addressType,
        physicalPostalAddress = entity.physicalPostalAddress?.let(::toPhysicalPostalAddressDto),
        alternativePostalAddress = entity.alternativePostalAddress?.let(this::toAlternativePostalAddressDto)
    )

    private fun toClassificationDto(entity: Classification) =
        BusinessPartnerClassificationDto(type = entity.type, code = entity.code, value = entity.value)

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

    //Mapping BusinessPartnerGenericDto from to BusinessPartner
    fun toBusinessPartner(dto: BusinessPartnerGenericDto, externalId: String) = BusinessPartner(
        externalId = externalId,
        nameParts = dto.nameParts.toMutableList(),
        shortName = dto.legalEntity.shortName,
        identifiers = dto.identifiers.mapNotNull { toIdentifier(it) }.toSortedSet(),
        legalName = dto.legalEntity.legalName,
        legalForm = dto.legalEntity.legalForm,
        states = dto.states.mapNotNull { toState(it) }.toSortedSet(),
        classifications = dto.legalEntity.classifications.map { toClassification(it) }.toSortedSet(),
        roles = dto.roles.toSortedSet(),
        postalAddress = toPostalAddress(dto.address),
        bpnL = dto.legalEntity.bpnL,
        bpnS = dto.site.bpnS,
        bpnA = dto.address.bpnA,
        stage = StageType.Output
    )

    private fun toIdentifier(dto: BusinessPartnerIdentifierDto) =
        dto.type?.let { type ->
            dto.value?.let { value ->
                Identifier(type = type, value = value, issuingBody = dto.issuingBody)
            }
        }

    private fun toState(dto: BusinessPartnerStateDto) =
        dto.type?.let { State(type = it, validFrom = dto.validFrom, validTo = dto.validTo, description = dto.description) }

    private fun toClassification(dto: BusinessPartnerClassificationDto) =
        Classification(type = dto.type, code = dto.code, value = dto.value)

    private fun toPostalAddress(dto: AddressComponent) =
        PostalAddress(
            addressType = dto.addressType,
            physicalPostalAddress = dto.physicalPostalAddress?.let(::toPhysicalPostalAddress),
            alternativePostalAddress = dto.alternativePostalAddress?.let(this::toAlternativePostalAddress)
        )

    private fun toPhysicalPostalAddress(dto: PhysicalPostalAddressDto) =
        PhysicalPostalAddress(
            geographicCoordinates = dto.geographicCoordinates?.let(::toGeographicCoordinate),
            country = dto.country,
            administrativeAreaLevel1 = dto.administrativeAreaLevel1,
            administrativeAreaLevel2 = dto.administrativeAreaLevel2,
            administrativeAreaLevel3 = dto.administrativeAreaLevel3,
            postalCode = dto.postalCode,
            city = dto.city,
            district = dto.district,
            street = dto.street?.let(::toStreet),
            companyPostalCode = dto.companyPostalCode,
            industrialZone = dto.industrialZone,
            building = dto.building,
            floor = dto.floor,
            door = dto.door
        )

    private fun toAlternativePostalAddress(dto: AlternativePostalAddressDto) =
        AlternativePostalAddress(
            geographicCoordinates = dto.geographicCoordinates?.let(::toGeographicCoordinate),
            country = dto.country,
            administrativeAreaLevel1 = dto.administrativeAreaLevel1,
            postalCode = dto.postalCode,
            city = dto.city,
            deliveryServiceType = dto.deliveryServiceType,
            deliveryServiceQualifier = dto.deliveryServiceQualifier,
            deliveryServiceNumber = dto.deliveryServiceNumber
        )

    private fun toStreet(dto: StreetDto) =
        Street(
            name = dto.name,
            houseNumber = dto.houseNumber,
            milestone = dto.milestone,
            direction = dto.direction,
            namePrefix = dto.namePrefix,
            additionalNamePrefix = dto.additionalNamePrefix,
            nameSuffix = dto.nameSuffix,
            additionalNameSuffix = dto.additionalNameSuffix
        )

    private fun toGeographicCoordinate(dto: GeoCoordinateDto) =
        GeographicCoordinate(latitude = dto.latitude, longitude = dto.longitude, altitude = dto.altitude)
}