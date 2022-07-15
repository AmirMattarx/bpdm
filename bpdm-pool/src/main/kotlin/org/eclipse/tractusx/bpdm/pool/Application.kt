package org.eclipse.tractusx.bpdm.pool

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRestClientAutoConfiguration
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication


@SpringBootApplication(
    exclude = [
        ElasticsearchRestClientAutoConfiguration::class,
        ElasticsearchDataAutoConfiguration::class,
        ElasticsearchRepositoriesAutoConfiguration::class,
        ReactiveElasticsearchRestClientAutoConfiguration::class],
    scanBasePackages = [
        "org.eclipse.tractusx.bpdm.pool.config",
        "org.eclipse.tractusx.bpdm.pool.controller",
        "org.eclipse.tractusx.bpdm.pool.repository",
        "org.eclipse.tractusx.bpdm.pool.service",
        "org.eclipse.tractusx.bpdm.common"
    ]
)
@ConfigurationPropertiesScan(
    basePackages = [
        "org.eclipse.tractusx.bpdm.pool",
        "org.eclipse.tractusx.bpdm.common"]
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}