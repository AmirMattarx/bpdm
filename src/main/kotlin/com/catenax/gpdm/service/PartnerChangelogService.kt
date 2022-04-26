package com.catenax.gpdm.service

import com.catenax.gpdm.dto.ChangelogEntryDto
import com.catenax.gpdm.entity.PartnerChangelogEntry
import com.catenax.gpdm.repository.PartnerChangelogEntryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import javax.transaction.Transactional

/**
 * Provides access to changelog entries of business partners. Changelog entries must be created manually via this service, when business partner (including
 * related child entities) are created/updated/deleted.
 *
 * The changelog entries can be used during synchronization of business partner data in order to know which business partners need to be synchronized.
 */
@Service
class PartnerChangelogService(
    val partnerChangelogEntryRepository: PartnerChangelogEntryRepository
) {
    @Transactional
    fun createChangelogEntry(changelogEntry: ChangelogEntryDto): PartnerChangelogEntry {
        return partnerChangelogEntryRepository.save(changelogEntry.toEntity())
    }

    @Transactional
    fun createChangelogEntries(changelogEntries: Collection<ChangelogEntryDto>): List<PartnerChangelogEntry> {
        val entities = changelogEntries.map { it.toEntity() }
        return partnerChangelogEntryRepository.saveAll(entities)
    }

    fun getChangelogEntriesStartingAfterId(startId: Long = -1, pageIndex: Int, pageSize: Int): Page<PartnerChangelogEntry> {
        return partnerChangelogEntryRepository.findAllByIdGreaterThan(startId, PageRequest.of(pageIndex, pageSize, Sort.by("id").ascending()))
    }

    private fun ChangelogEntryDto.toEntity(): PartnerChangelogEntry {
        return PartnerChangelogEntry(this.changelogType, this.bpn)
    }
}