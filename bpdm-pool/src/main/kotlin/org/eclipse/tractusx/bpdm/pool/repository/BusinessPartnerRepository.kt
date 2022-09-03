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

package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.BusinessPartner
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import java.time.Instant

interface BusinessPartnerRepository : PagingAndSortingRepository<BusinessPartner, Long>{
    fun findByBpn(bpn: String): BusinessPartner?

    fun existsByBpn(bpn: String): Boolean

    fun findDistinctByBpnIn(bpns: Collection<String>): Set<BusinessPartner>

    fun findByUpdatedAtAfter(updatedAt: Instant, pageable: Pageable): Page<BusinessPartner>

    @Query("SELECT DISTINCT i.partner FROM Identifier i WHERE i.type = ?1 AND i.value = ?2")
    fun findByIdentifierTypeAndValue(type: IdentifierType, idValue: String): BusinessPartner?

    @Query("SELECT DISTINCT i.partner FROM Identifier i WHERE i.type.technicalKey = ?1 AND i.value in ?2")
    fun findByIdentifierTypeAndValues(type: String, values: Collection<String>): Set<BusinessPartner>

    @Query("SELECT DISTINCT p FROM BusinessPartner p LEFT JOIN FETCH p.legalForm WHERE p IN :partners")
    fun joinLegalForm(partners: Set<BusinessPartner>): Set<BusinessPartner>

    @Query("SELECT DISTINCT p FROM BusinessPartner p LEFT JOIN FETCH p.names WHERE p IN :partners")
    fun joinNames(partners: Set<BusinessPartner>): Set<BusinessPartner>

    @Query("SELECT DISTINCT p FROM BusinessPartner p LEFT JOIN FETCH p.identifiers WHERE p IN :partners")
    fun joinIdentifiers(partners: Set<BusinessPartner>): Set<BusinessPartner>

    @Query("SELECT DISTINCT p FROM BusinessPartner p LEFT JOIN FETCH p.stati WHERE p IN :partners")
    fun joinStatuses(partners: Set<BusinessPartner>): Set<BusinessPartner>

    @Query("SELECT DISTINCT p FROM BusinessPartner p LEFT JOIN FETCH p.classification WHERE p IN :partners")
    fun joinClassifications(partners: Set<BusinessPartner>): Set<BusinessPartner>

    @Query("SELECT DISTINCT p FROM BusinessPartner p LEFT JOIN FETCH p.bankAccounts WHERE p IN :partners")
    fun joinBankAccounts(partners: Set<BusinessPartner>): Set<BusinessPartner>

    @Query("SELECT DISTINCT p FROM BusinessPartner p LEFT JOIN FETCH p.startNodeRelations LEFT JOIN FETCH p.endNodeRelations WHERE p IN :partners")
    fun joinRelations(partners: Set<BusinessPartner>): Set<BusinessPartner>

    @Query("SELECT DISTINCT p FROM BusinessPartner p LEFT JOIN FETCH p.startNodeRelations LEFT JOIN FETCH p.types WHERE p IN :partners")
    fun joinTypes(partners: Set<BusinessPartner>): Set<BusinessPartner>

    @Query("SELECT DISTINCT p FROM BusinessPartner p LEFT JOIN FETCH p.startNodeRelations LEFT JOIN FETCH p.roles WHERE p IN :partners")
    fun joinRoles(partners: Set<BusinessPartner>): Set<BusinessPartner>

    @Query("SELECT DISTINCT p FROM BusinessPartner p LEFT JOIN FETCH p.legalAddress WHERE p IN :partners")
    fun joinLegalAddresses(partners: Set<BusinessPartner>): Set<BusinessPartner>
}