package com.dk.focusguard.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface managing the local domain blocklist for adult content filtering.
 */
interface DomainBlocklistRepository {

    /** Combined flow of starter adult domains and user-configured custom domains */
    val allBlockedDomains: Flow<Set<String>>

    /** Flow of only user-added custom blocked domains */
    val customBlockedDomains: Flow<Set<String>>

    /** Flow of default starter adult domains */
    val defaultBlockedDomains: Set<String>

    /** Returns direct snapshot of all active blocked domains */
    fun getAllBlockedDomainsDirect(): Set<String>

    /** Checks whether a given domain (or any of its parent domains) is blocked */
    fun isDomainBlocked(domain: String): Boolean

    /** Validates domain syntax according to RFC 1035 rules */
    fun isValidDomain(domain: String): Boolean

    /** Adds a custom blocked domain with syntax validation and deduplication */
    suspend fun addCustomBlockedDomain(domain: String): Result<Unit>

    /** Removes a custom blocked domain */
    suspend fun removeCustomBlockedDomain(domain: String): Result<Unit>

    /** Imports a list of domains, validating syntax and deduplicating */
    suspend fun importDomains(domains: List<String>): Result<Int>

    /** Clears all user custom blocked domains */
    suspend fun clearCustomDomains(): Result<Unit>
}

