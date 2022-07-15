package org.eclipse.tractusx.bpdm.common.dto.cdq

data class FetchResponse(
    val businessPartner: BusinessPartnerCdq?,
    val message: String?,
    val status: Status
) {
    enum class Status {
        OK,
        NOT_FOUND
    }
}