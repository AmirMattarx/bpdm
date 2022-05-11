package com.catenax.gpdm.util

import com.catenax.gpdm.dto.response.SyncResponse
import com.catenax.gpdm.entity.SyncStatus
import org.assertj.core.api.Assertions
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory

private const val RETRY_IMPORT_TIMES: Int = 10
private const val RETRY_IMPORT_BACKOFF: Long = 200
private const val BPDM_DB_SCHEMA_NAME: String = "bpdm"

@Component
class TestHelpers(
    entityManagerFactory: EntityManagerFactory
) {

    val em: EntityManager = entityManagerFactory.createEntityManager()

    fun truncateDbTables() {
        em.transaction.begin()

        em.createNativeQuery(
            """
            DO $$ DECLARE table_names RECORD;
            BEGIN
                FOR table_names IN SELECT table_name
                    FROM information_schema.tables
                    WHERE table_schema='${BPDM_DB_SCHEMA_NAME}'
                    AND table_name NOT IN ('flyway_schema_history') 
                LOOP 
                    EXECUTE format('TRUNCATE TABLE ${BPDM_DB_SCHEMA_NAME}.%I CONTINUE IDENTITY CASCADE;', table_names.table_name);
                END LOOP;
            END $$;
        """.trimIndent()
        ).executeUpdate()

        em.transaction.commit()
    }

    fun startSyncAndAwaitSuccess(client: WebTestClient, syncPath: String): SyncResponse {
        client.post().uri(syncPath)
            .exchange()
            .expectStatus()
            .is2xxSuccessful

        //check for async import to finish several times
        var i = 1
        var syncResponse: SyncResponse
        do{
            Thread.sleep(RETRY_IMPORT_BACKOFF)

            syncResponse = client.get().uri(syncPath)
                .exchange()
                .expectStatus()
                .is2xxSuccessful
                .returnResult<SyncResponse>()
                .responseBody
                .blockFirst()!!

            if(syncResponse.status == SyncStatus.SUCCESS)
                break

            i++
        }while (i < RETRY_IMPORT_TIMES)

        Assertions.assertThat(syncResponse.status).isEqualTo(SyncStatus.SUCCESS)

        return syncResponse
    }
}