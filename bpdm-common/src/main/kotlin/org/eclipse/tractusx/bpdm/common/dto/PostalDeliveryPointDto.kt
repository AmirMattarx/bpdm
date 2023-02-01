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

package org.eclipse.tractusx.bpdm.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.model.PostalDeliveryPointType

@Schema(name = "PostalDeliveryPoint", description = "Postal delivery point record for an address")
data class PostalDeliveryPointDto(
    @Schema(description = "Full name of the delivery point")
    val value: String,
    @Schema(description = "Abbreviation or shorthand, if applicable")
    val shortName: String? = null,
    @Schema(description = "Number/code of the delivery point, if applicable")
    val number: String? = null,
    @Schema(description = "Type of the specified delivery point", defaultValue = "OTHER")
    val type: PostalDeliveryPointType = PostalDeliveryPointType.OTHER
)