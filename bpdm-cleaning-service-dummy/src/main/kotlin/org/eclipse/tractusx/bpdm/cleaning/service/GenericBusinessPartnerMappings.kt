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

package org.eclipse.tractusx.bpdm.cleaning.service


import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerIdentifierDto
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerStateDto
import org.eclipse.tractusx.bpdm.common.dto.ClassificationDto
import org.eclipse.tractusx.orchestrator.api.model.*


fun BusinessPartnerGenericDto.toLegalEntityDto(bpnReferenceDto: BpnReferenceDto, legalAddress: LogisticAddressDto): LegalEntityDto {

    return LegalEntityDto(
        bpnLReference = bpnReferenceDto,
        hasChanged = true,
        legalName = nameParts.joinToString(" "),
        legalShortName = shortName,
        identifiers = identifiers.mapNotNull { it.toLegalEntityIdentifierDto() },
        legalForm = legalForm,
        states = states.mapNotNull { it.toLegalEntityState() },
        classifications = classifications.map { it.toBusinessPartnerClassificationDto() },
        legalAddress = legalAddress

    )
}

fun ClassificationDto.toBusinessPartnerClassificationDto(): ClassificationDto {

    return ClassificationDto(code = code, type = type, value = value)
}

fun BusinessPartnerIdentifierDto.toLegalEntityIdentifierDto(): LegalEntityIdentifierDto? {

    return value?.let { value ->
        type?.let { type ->
            LegalEntityIdentifierDto(value = value, type = type, issuingBody = issuingBody)
        }
    }

}

fun BusinessPartnerStateDto.toLegalEntityState(): LegalEntityState? {

    return type?.let { LegalEntityState(description, validFrom, validTo, it) }
}

fun BusinessPartnerStateDto.toSiteState(): SiteStateDto? {

    return type?.let { SiteStateDto(description, validFrom, validTo, it) }
}

fun BusinessPartnerGenericDto.toLogisticAddressDto(bpnReferenceDto: BpnReferenceDto):
        LogisticAddressDto {
    return LogisticAddressDto(
        bpnAReference = bpnReferenceDto,
        hasChanged = true,
        name = nameParts.joinToString(" "),
        states = emptyList(),
        identifiers = emptyList(),
        physicalPostalAddress = postalAddress.physicalPostalAddress,
        alternativePostalAddress = postalAddress.alternativePostalAddress
    )
}

fun BusinessPartnerGenericDto.toSiteDto(bpnReferenceDto: BpnReferenceDto, legalName: String, siteAddressReference: LogisticAddressDto): SiteDto {


    return SiteDto(
        bpnSReference = bpnReferenceDto,
        hasChanged = true,
        name = legalName,
        states = states.mapNotNull { it.toSiteState() },
        mainAddress = siteAddressReference

    )
}





