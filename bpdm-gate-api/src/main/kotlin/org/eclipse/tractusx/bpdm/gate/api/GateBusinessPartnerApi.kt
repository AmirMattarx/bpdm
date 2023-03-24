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

package org.eclipse.tractusx.bpdm.gate.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.gate.dto.BusinessPartnerCandidateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.TypeMatchResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@RequestMapping("/api/catena/business-partners")
@HttpExchange("/api/catena/business-partners")
interface GateBusinessPartnerApi {

    @Operation(
        summary = "Determine the LSA type of a business partner candidate",
        description = "For one business partner candidate this request determines its likely type of either legal entity, site or address." +
                "It is possible that no type could be determined." +
                "The candidate needs to contain either a name or an identifier as a minimum requirement."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Records were successfully processed."),
            ApiResponse(responseCode = "400", description = "No name and no identifier given, or malformed request", content = [Content()])
        ]
    )
    @PostMapping("/type-match")
    @PostExchange("/type-match")
    fun determineLsaType(@RequestBody candidate: BusinessPartnerCandidateDto): TypeMatchResponse

}