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

package org.eclipse.tractusx.bpdm.gate.controller

import org.eclipse.tractusx.bpdm.gate.client.service.businessPartner.GateClientBusinessPartnerInterface
import org.eclipse.tractusx.bpdm.gate.dto.BusinessPartnerCandidateDto
import org.eclipse.tractusx.bpdm.gate.dto.response.TypeMatchResponse
import org.eclipse.tractusx.bpdm.gate.exception.BpdmInvalidPartnerException
import org.eclipse.tractusx.bpdm.gate.service.TypeMatchingService
import org.springframework.web.bind.annotation.RestController

@RestController
class GateClientBusinessPartnerController(
    private val typeMatchingService: TypeMatchingService
) : GateClientBusinessPartnerInterface {

    override fun determineLsaType(candidate: BusinessPartnerCandidateDto): TypeMatchResponse {
        if (candidate.names.isEmpty() && candidate.identifiers.isEmpty())
            throw BpdmInvalidPartnerException("Candidate", "Business partner candidate needs to specify either a name or identifier.")

        return typeMatchingService.determineCandidateType(candidate)
    }

}