package org.eclipse.tractusx.bpdm.pool.util

object EndpointValues {

    const val CDQ_MOCK_STORAGE_PATH = "/test-cdq-api/storages/test-cdq-storage"
    const val CDQ_MOCK_BUSINESS_PARTNER_PATH = "$CDQ_MOCK_STORAGE_PATH/businesspartners"

    const val TEXT_PARAM_NAME = "text"

    const val CATENA_PATH = "/api/catena"
    const val CATENA_BUSINESS_PARTNER_PATH = "${CATENA_PATH}/business-partner"
    const val CATENA_NAME_PATH = "$CATENA_BUSINESS_PARTNER_PATH/name"
    const val CATENA_LEGAL_FORM_PATH = "$CATENA_BUSINESS_PARTNER_PATH/legal-form"
    const val CATENA_STATUS_PATH = "$CATENA_BUSINESS_PARTNER_PATH/status"
    const val CATENA_CLASSIFICATION_PATH = "$CATENA_BUSINESS_PARTNER_PATH/classification"
    const val CATENA_SITE_PATH = "$CATENA_BUSINESS_PARTNER_PATH/site"

    const val CATENA_ADDRESS_PATH = "$CATENA_BUSINESS_PARTNER_PATH/address"
    const val CATENA_ADMIN_AREA_PATH = "$CATENA_ADDRESS_PATH/administrative-area"
    const val CATENA_POST_CODE_PATH = "$CATENA_ADDRESS_PATH/postcode"
    const val CATENA_LOCALITY_PATH = "$CATENA_ADDRESS_PATH/locality"
    const val CATENA_THOROUGHFARE_PATH = "$CATENA_ADDRESS_PATH/thoroughfare"
    const val CATENA_PREMISE_PATH = "$CATENA_ADDRESS_PATH/premise"
    const val CATENA_POSTAL_DELIVERY_POINT_PATH = "$CATENA_ADDRESS_PATH/postal-delivery-point"

    const val CATENA_CONFIRM_UP_TO_DATE_PATH_POSTFIX = "/confirm-up-to-date"
    const val CATENA_CHANGELOG_PATH_POSTFIX = "/changelog"
    const val CATENA_ADDRESSES_PATH_POSTFIX = "/addresses"
    const val CATENA_SITES_PATH_POSTFIX = "/sites"

    const val CATENA_BPN_SEARCH_PATH = "/api/catena/bpn/search"

    const val CATENA_ADDRESSES_PATH = "/api/catena/addresses"
    const val CATENA_ADDRESSES_SEARCH_PATH = "$CATENA_ADDRESSES_PATH/search"

    const val CATENA_SITES_PATH = "/api/catena/sites"
    const val CATENA_SITE_SEARCH_PATH = "$CATENA_SITES_PATH/search"

    const val CDQ_SYNCH_PATH = "/api/cdq/business-partner/sync"

    const val OPENSEARCH_SYNC_PATH = "api/opensearch/business-partner"

    const val CATENA_METADATA_IDENTIFIER_TYPE_PATH = "$CATENA_PATH/identifier-type"
    const val CATENA_METADATA_IDENTIFIER_STATUS_PATH = "$CATENA_PATH/identifier-status"
    const val CATENA_METADATA_ISSUING_BODY_PATH = "$CATENA_PATH/issuing-body"
    const val CATENA_METADATA_LEGAL_FORM_PATH = "$CATENA_PATH/legal-form"

}