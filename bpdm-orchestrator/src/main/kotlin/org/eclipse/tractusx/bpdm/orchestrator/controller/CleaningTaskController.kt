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

package org.eclipse.tractusx.bpdm.orchestrator.controller

import org.eclipse.tractusx.bpdm.common.exception.BpdmUpsertLimitException
import org.eclipse.tractusx.bpdm.orchestrator.config.ApiConfigProperties
import org.eclipse.tractusx.orchestrator.api.CleaningTaskApi
import org.eclipse.tractusx.orchestrator.api.model.*
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class CleaningTaskController(
    val apiConfigProperties: ApiConfigProperties
) : CleaningTaskApi {

    //While we don't have an implementation use a dummy response for the endpoints
    val dummyResponseCreateTask =
        TaskCreateResponse(
            listOf(
                TaskRequesterState(
                    taskId = "0",
                    businessPartnerResult = null,
                    processingState = TaskProcessingStateDto(
                        cleaningStep = CleaningStep.CleanAndSync,
                        reservationState = ReservationState.Queued,
                        resultState = ResultState.Pending,
                        errors = emptyList(),
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now()
                    )
                ),
                TaskRequesterState(
                    taskId = "1",
                    businessPartnerResult = null,
                    processingState = TaskProcessingStateDto(
                        cleaningStep = CleaningStep.CleanAndSync,
                        reservationState = ReservationState.Queued,
                        resultState = ResultState.Pending,
                        errors = emptyList(),
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now()
                    )
                )
            )
        )

    //While we don't have an implementation use a dummy response for the endpoints
    val dummyResponseTaskState =
        TaskStateResponse(
            listOf(
                TaskRequesterState(
                    taskId = "0",
                    businessPartnerResult = null,
                    processingState = TaskProcessingStateDto(
                        cleaningStep = CleaningStep.CleanAndSync,
                        reservationState = ReservationState.Queued,
                        resultState = ResultState.Pending,
                        errors = emptyList(),
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now()
                    )
                ),
                TaskRequesterState(
                    taskId = "1",
                    businessPartnerResult = null,
                    processingState = TaskProcessingStateDto(
                        cleaningStep = CleaningStep.Clean,
                        reservationState = ReservationState.Queued,
                        resultState = ResultState.Pending,
                        errors = emptyList(),
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now()
                    )
                )
            )
        )


    override fun createCleaningTasks(createRequest: TaskCreateRequest): TaskCreateResponse {
        if (createRequest.businessPartners.size > apiConfigProperties.upsertLimit)
            throw BpdmUpsertLimitException(createRequest.businessPartners.size, apiConfigProperties.upsertLimit)

        //ToDo: Replace with service logic
        return dummyResponseCreateTask
    }


    override fun searchCleaningTaskState(searchTaskIdRequest: TaskStateRequest): TaskStateResponse {
        // ToDo: Replace with service logic
        return dummyResponseTaskState
    }

}