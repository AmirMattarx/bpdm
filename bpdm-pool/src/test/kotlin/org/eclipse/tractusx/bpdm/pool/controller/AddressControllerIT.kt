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

package org.eclipse.tractusx.bpdm.pool.controller

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.IdentifierTypeDto
import org.eclipse.tractusx.bpdm.common.dto.request.AddressPartnerBpnSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.util.*
import org.eclipse.tractusx.bpdm.pool.util.RequestValues.addressIdentifier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OpenSearchContextInitializer::class])
class AddressControllerIT @Autowired constructor(
    val testHelpers: TestHelpers,
    val poolClient: PoolApiClient
) {

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        testHelpers.createTestMetadata()
    }

    /**
     * Given partners in db
     * When requesting an address by bpn-a
     * Then address is returned
     */
    @Test
    fun `get address by bpn-a`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    addresses = listOf(RequestValues.addressPartnerCreate2)
                )
            )
        )

        val importedPartner = createdStructures.single().legalEntity
        val addressesByBpnL = importedPartner.legalEntity.bpnl
            .let { bpnL -> requestAddressesOfLegalEntity(bpnL).content }
        // 1 legal address, 1 regular address
        assertThat(addressesByBpnL.size).isEqualTo(2)
        assertThat(addressesByBpnL.count { it.isLegalAddress }).isEqualTo(1)

        // Same address if we use the address-by-BPNA method
        addressesByBpnL
            .forEach { address ->
                val addressByBpnA = requestAddress(address.bpna)
                assertThat(addressByBpnA.bpnLegalEntity).isEqualTo(importedPartner.legalEntity.bpnl)
                assertThat(addressByBpnA).isEqualTo(address)
            }
    }

    /**
     * Given partners in db
     * When requesting an address by non-existent bpn-a
     * Then a "not found" response is sent
     */
    @Test
    fun `get address by bpn-a, not found`() {
        testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    addresses = listOf(RequestValues.addressPartnerCreate1)
                )
            )
        )

        testHelpers.`get address by bpn-a, not found`("NONEXISTENT_BPN")

    }

    /**
     * Given multiple address partners
     * When searching addresses with BPNA
     * Then return those addresses
     */
    @Test
    fun `search addresses by BPNA`() {

        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    addresses = listOf(RequestValues.addressPartnerCreate1, RequestValues.addressPartnerCreate2, RequestValues.addressPartnerCreate3)
                )
            )
        )

        val bpnA1 = createdStructures[0].addresses[0].address.bpna
        val bpnA2 = createdStructures[0].addresses[1].address.bpna

        val searchRequest = AddressPartnerBpnSearchRequest(addresses = listOf(bpnA1, bpnA2))
        val searchResult =
            poolClient.addresses().searchAddresses(searchRequest, PaginationRequest())

        val expected = listOf(
            ResponseValues.addressPartner1,
            ResponseValues.addressPartner2
        )

        assertAddressesAreEqual(searchResult.content, expected)
    }

    /**
     * Given multiple addresses of business partners
     * When searching addresses with BPNL
     * Then return addresses belonging to those legal entities (including legal addresses!)
     */
    @Test
    fun `search addresses by BPNL`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    // no additional addresses
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    addresses = listOf(RequestValues.addressPartnerCreate3)
                )
            )
        )

        val bpnL2 = createdStructures[1].legalEntity.legalEntity.bpnl

        val searchRequest = AddressPartnerBpnSearchRequest(legalEntities = listOf(bpnL2))
        val searchResult = poolClient.addresses().searchAddresses(searchRequest, PaginationRequest())

        val expected = listOf(
            ResponseValues.addressPartner2.copy(isLegalAddress = true),
            ResponseValues.addressPartner3
        )

        assertAddressesAreEqual(searchResult.content, expected)
    }

    /**
     * Given multiple addresses of business partners
     * When searching addresses with BPNS
     * Then return addresses belonging to those sites (including main addresses!)
     */
    @Test
    fun `search addresses by BPNS`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(
                            site = RequestValues.siteCreate1,
                            addresses = listOf(RequestValues.addressPartnerCreate1, RequestValues.addressPartnerCreate2)
                        )
                    )
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    siteStructures = listOf(
                        SiteStructureRequest(
                            site = RequestValues.siteCreate2,
                            addresses = listOf(RequestValues.addressPartnerCreate3)
                        )
                    )
                )
            )
        )

        val bpnS1 = createdStructures[0].siteStructures[0].site.site.bpns
        val bpnS2 = createdStructures[1].siteStructures[0].site.site.bpns

        // search for site1 -> main address and 2 regular addresses
        AddressPartnerBpnSearchRequest(sites = listOf(bpnS1))
            .let { poolClient.addresses().searchAddresses(it, PaginationRequest()) }
            .let {
                assertAddressesAreEqual(
                    it.content, listOf(
                        ResponseValues.addressPartner1.copy(isMainAddress = true),
                        ResponseValues.addressPartner1,
                        ResponseValues.addressPartner2,
                    )
                )
            }

        // search for site2 -> main address and 1 regular address
        AddressPartnerBpnSearchRequest(sites = listOf(bpnS2))       // search for site2
            .let { poolClient.addresses().searchAddresses(it, PaginationRequest()) }
            .let {
                assertAddressesAreEqual(
                    it.content, listOf(
                        ResponseValues.addressPartner2.copy(isMainAddress = true),
                        ResponseValues.addressPartner3,
                    )
                )
            }

        // search for site1 and site2 -> 2 main addresses and 3 regular addresses
        AddressPartnerBpnSearchRequest(sites = listOf(bpnS2, bpnS1))    // search for site1 and site2
            .let { poolClient.addresses().searchAddresses(it, PaginationRequest()) }
            .let {
                assertAddressesAreEqual(
                    it.content, listOf(
                        // site1
                        ResponseValues.addressPartner1.copy(isMainAddress = true),
                        ResponseValues.addressPartner1,
                        ResponseValues.addressPartner2,
                        // site2
                        ResponseValues.addressPartner2.copy(isMainAddress = true),
                        ResponseValues.addressPartner3,
                    )
                )
            }
    }

    /**
     * Given sites and legal entities
     * When creating addresses for sites and legal entities
     * Then new addresses created and returned
     */
    @Test
    fun `create new addresses`() {

        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate1))
                ),
            )
        )

        val bpnL = givenStructure[0].legalEntity.legalEntity.bpnl
        val bpnS = givenStructure[0].siteStructures[0].site.site.bpns

        val expected = listOf(
            ResponseValues.addressPartnerCreate1,
            ResponseValues.addressPartnerCreate2,
            ResponseValues.addressPartnerCreate3
        )

        val toCreate = listOf(
            RequestValues.addressPartnerCreate1.copy(bpnParent = bpnL),
            RequestValues.addressPartnerCreate2.copy(bpnParent = bpnL),
            RequestValues.addressPartnerCreate3.copy(bpnParent = bpnS)
        )

        val response = poolClient.addresses().createAddresses(toCreate)

        assertCreatedAddressesAreEqual(response.entities, expected)
//        response.entities.forEach { assertThat(it.address.bpn).matches(testHelpers.bpnAPattern) }
//        testHelpers.assertRecursively(response.entities)
//            .ignoringFields(LogisticAddressResponse::bpn.name)
//            .isEqualTo(expected)
        assertThat(response.errorCount).isEqualTo(0)
    }

    /**
     * Given no legal entities
     * When creating new legal entity with duplicate identifiers on legal entity and address
     * Then new legal entity is returned with error
     */
    @Test
    fun `create new addresses and get duplicate error`() {

        poolClient.metadata().createIdentifierType(
            IdentifierTypeDto(
                technicalKey = addressIdentifier.type,
                businessPartnerType = IdentifierBusinessPartnerType.ADDRESS, name = addressIdentifier.value
            )
        )

        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(RequestValues.siteCreate1))
                ),
            )
        )

        val bpnL = givenStructure[0].legalEntity.legalEntity.bpnl


        val toCreate = RequestValues.addressPartnerCreate5.copy(bpnParent = bpnL)
        val secondCreate = RequestValues.addressPartnerCreate5.copy(bpnParent = bpnL, index = CommonValues.index4)

        val response = poolClient.addresses().createAddresses(listOf(toCreate, secondCreate))


        assertThat(response.errorCount).isEqualTo(2)
        assertThat(response.entityCount).isEqualTo(0)
        val errors = response.errors.toList()
        testHelpers.assertErrorResponse(errors[0], AddressCreateError.AddressDuplicateIdentifier, toCreate.index!!)
        testHelpers.assertErrorResponse(errors[1], AddressCreateError.AddressDuplicateIdentifier, secondCreate.index!!)

    }

    /**
     * Given no address entities
     * When creating some address entities in one request that have duplicate identifiers (regarding type and value)
     * Then for these address entities an error is returned
     */
    @Test
    fun `update address entities and get duplicate identifier error`() {

        poolClient.metadata().createIdentifierType(
            IdentifierTypeDto(
                technicalKey = addressIdentifier.type,
                businessPartnerType = IdentifierBusinessPartnerType.ADDRESS, name = addressIdentifier.value
            )
        )

        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(
                            site = RequestValues.siteCreate1,
                            addresses = listOf(RequestValues.addressPartnerCreate1, RequestValues.addressPartnerCreate2)
                        )
                    )
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    addresses = listOf(RequestValues.addressPartnerCreate3)
                )
            )
        )

        val bpnA1 = givenStructure[0].siteStructures[0].addresses[0].address.bpna
        val bpnA2 = givenStructure[0].siteStructures[0].addresses[1].address.bpna
        val bpnA3 = givenStructure[1].addresses[0].address.bpna

        val expected = listOf(
            ResponseValues.addressPartner1.copy(bpna = bpnA2),
            ResponseValues.addressPartner2.copy(bpna = bpnA3),
            ResponseValues.addressPartner3.copy(bpna = bpnA1)
        )

        val toUpdate = listOf(
            RequestValues.addressPartnerUpdate1.copy(bpna = bpnA2, address = RequestValues.logisticAddress5),
            RequestValues.addressPartnerUpdate2.copy(bpna = bpnA3, address = RequestValues.logisticAddress5),
            RequestValues.addressPartnerUpdate3.copy(bpna = bpnA1, address = RequestValues.logisticAddress5)
        )

        val response = poolClient.addresses().updateAddresses(toUpdate)

        assertThat(response.errorCount).isEqualTo(3)
        assertThat(response.entityCount).isEqualTo(0)
        val errors = response.errors.toList()
        testHelpers.assertErrorResponse(errors[0], AddressUpdateError.AddressDuplicateIdentifier, toUpdate[0].bpna)
        testHelpers.assertErrorResponse(errors[1], AddressUpdateError.AddressDuplicateIdentifier, toUpdate[1].bpna)
        testHelpers.assertErrorResponse(errors[2], AddressUpdateError.AddressDuplicateIdentifier, toUpdate[2].bpna)
    }

    /**
     * Given sites and legal entities
     * When creating addresses with some having non-existent parents
     * Then only addresses with existing parents created and returned
     */
    @Test
    fun `don't create addresses with non-existent parent`() {
        val bpnL = poolClient.legalEntities().createBusinessPartners(listOf(RequestValues.legalEntityCreate1))
            .entities.single().legalEntity.bpnl

        val expected = listOf(
            ResponseValues.addressPartnerCreate1,
        )
        val expectedErrors = listOf(
            ErrorInfo(AddressCreateError.BpnNotValid, "message ignored", CommonValues.index3),
            ErrorInfo(AddressCreateError.SiteNotFound, "message ignored", CommonValues.index1),
            ErrorInfo(AddressCreateError.LegalEntityNotFound, "message ignored ", CommonValues.index2)
        )

        val invalidSiteBpn = "BPNSXXXXXXXXXX"
        val invalidLegalEntityBpn = "BPNLXXXXXXXXXX"
        val completelyInvalidBpn = "XYZ"
        val toCreate = listOf(
            RequestValues.addressPartnerCreate1.copy(bpnParent = bpnL),
            RequestValues.addressPartnerCreate1.copy(bpnParent = invalidSiteBpn),
            RequestValues.addressPartnerCreate2.copy(bpnParent = invalidLegalEntityBpn),
            RequestValues.addressPartnerCreate3.copy(bpnParent = completelyInvalidBpn),
        )

        val response = poolClient.addresses().createAddresses(toCreate)
        assertCreatedAddressesAreEqual(response.entities, expected)
//        response.entities.forEach { assertThat(it.address.bpn).matches(testHelpers.bpnAPattern) }
//        testHelpers.assertRecursively(response.entities).ignoringFields(LogisticAddressResponse::bpn.name).isEqualTo(expected)

        assertThat(response.errorCount).isEqualTo(3)
        testHelpers.assertRecursively(response.errors)
            .ignoringFields(ErrorInfo<AddressCreateError>::message.name)
            .isEqualTo(expectedErrors)
    }

    /**
     * Given addresses
     * When updating addresses via BPNs
     * Then update and return those addresses
     */
    @Test
    fun `update addresses`() {
        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(
                            site = RequestValues.siteCreate1,
                            addresses = listOf(RequestValues.addressPartnerCreate1, RequestValues.addressPartnerCreate2)
                        )
                    )
                ),
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate2,
                    addresses = listOf(RequestValues.addressPartnerCreate3)
                )
            )
        )

        val bpnA1 = givenStructure[0].siteStructures[0].addresses[0].address.bpna
        val bpnA2 = givenStructure[0].siteStructures[0].addresses[1].address.bpna
        val bpnA3 = givenStructure[1].addresses[0].address.bpna

        val expected = listOf(
            ResponseValues.addressPartner1.copy(bpna = bpnA2),
            ResponseValues.addressPartner2.copy(bpna = bpnA3),
            ResponseValues.addressPartner3.copy(bpna = bpnA1)
        )

        val toUpdate = listOf(
            RequestValues.addressPartnerUpdate1.copy(bpna = bpnA2),
            RequestValues.addressPartnerUpdate2.copy(bpna = bpnA3),
            RequestValues.addressPartnerUpdate3.copy(bpna = bpnA1)
        )

        val response = poolClient.addresses().updateAddresses(toUpdate)

        assertAddressesAreEqual(response.entities, expected)
        assertThat(response.errorCount).isEqualTo(0)
    }

    /**
     * Given addresses
     * When updating addresses with some having non-existent BPNs
     * Then only update and return addresses with existent BPNs
     */
    @Test
    fun `updates addresses, ignore non-existent`() {
        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = RequestValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(
                            site = RequestValues.siteCreate1,
                            addresses = listOf(RequestValues.addressPartnerCreate1)
                        )
                    )
                )
            )
        )

        val bpnA1 = givenStructure[0].siteStructures[0].addresses[0].address.bpna

        val expected = listOf(
            ResponseValues.addressPartner2.copy(bpna = bpnA1)
        )

        val firstInvalidBpn = "BPNLXXXXXXXX"
        val secondInvalidBpn = "BPNAXXXXXXXX"
        val toUpdate = listOf(
            RequestValues.addressPartnerUpdate2.copy(bpna = bpnA1),
            RequestValues.addressPartnerUpdate2.copy(bpna = firstInvalidBpn),
            RequestValues.addressPartnerUpdate3.copy(bpna = secondInvalidBpn)
        )

        val response = poolClient.addresses().updateAddresses(toUpdate)

        assertAddressesAreEqual(response.entities, expected)

        assertThat(response.errorCount).isEqualTo(2)
        testHelpers.assertErrorResponse(response.errors.first(), AddressUpdateError.AddressNotFound, firstInvalidBpn)
        testHelpers.assertErrorResponse(response.errors.last(), AddressUpdateError.AddressNotFound, secondInvalidBpn)
    }

    private fun assertCreatedAddressesAreEqual(actuals: Collection<AddressPartnerCreateVerboseDto>, expected: Collection<AddressPartnerCreateVerboseDto>) {
        actuals.forEach { assertThat(it.address.bpna).matches(testHelpers.bpnAPattern) }

        testHelpers.assertRecursively(actuals)
            .ignoringFields(
                AddressPartnerCreateVerboseDto::address.name + "." + LogisticAddressVerboseDto::bpna.name,
                AddressPartnerCreateVerboseDto::address.name + "." + LogisticAddressVerboseDto::bpnLegalEntity.name,
                AddressPartnerCreateVerboseDto::address.name + "." + LogisticAddressVerboseDto::bpnSite.name
            )
            .isEqualTo(expected)
    }

    private fun assertAddressesAreEqual(actuals: Collection<LogisticAddressVerboseDto>, expected: Collection<LogisticAddressVerboseDto>) {
        actuals.forEach { assertThat(it.bpna).matches(testHelpers.bpnAPattern) }

        testHelpers.assertRecursively(actuals)
            .ignoringFields(
                LogisticAddressVerboseDto::bpna.name,
                LogisticAddressVerboseDto::bpnLegalEntity.name,
                LogisticAddressVerboseDto::bpnSite.name
            )
            .isEqualTo(expected)
    }

    private fun requestAddress(bpnAddress: String) = poolClient.addresses().getAddress(bpnAddress)

    private fun requestAddressesOfLegalEntity(bpn: String) =
        poolClient.legalEntities().getAddresses(bpn, PaginationRequest())

}