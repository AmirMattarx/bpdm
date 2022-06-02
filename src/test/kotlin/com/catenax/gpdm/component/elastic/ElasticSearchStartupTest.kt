package com.catenax.gpdm.component.elastic

import com.catenax.gpdm.Application
import com.catenax.gpdm.dto.response.BusinessPartnerSearchResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.util.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, ElasticsearchValidIndexInitializer::class])
class ElasticSearchValidIndexStartupTest @Autowired constructor(
    val webTestClient: WebTestClient,
    val testHelpers: TestHelpers
) {

    companion object {
        @RegisterExtension
        var wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.cdq.host") { wireMockServer.baseUrl() }
        }
    }

    @Test
    fun acceptValidIndexOnStartup() {
        testHelpers.importAndGetResponse(listOf(CdqValues.businessPartner1), webTestClient, wireMockServer)

        val searchResult = webTestClient.get().uri(EndpointValues.CATENA_BUSINESS_PARTNER_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<BusinessPartnerSearchResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(searchResult.content).isNotEmpty
    }
}

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, ElasticsearchOutdatedIndexInitializer::class])
class ElasticSearchOutdatedIndexStartupTest @Autowired constructor(
    val webTestClient: WebTestClient,
    val testHelpers: TestHelpers
) {

    companion object {
        @RegisterExtension
        var wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.cdq.host") { wireMockServer.baseUrl() }
        }
    }

    @Test
    fun recreateOutdatedIndexOnStartup() {
        testHelpers.importAndGetResponse(listOf(CdqValues.businessPartner1), webTestClient, wireMockServer)

        val searchResult = webTestClient.get().uri(EndpointValues.CATENA_BUSINESS_PARTNER_PATH)
            .exchange()
            .expectStatus().is2xxSuccessful
            .returnResult<PageResponse<BusinessPartnerSearchResponse>>()
            .responseBody
            .blockFirst()!!

        assertThat(searchResult.content).isEmpty()
    }
}