package com.dk.focusguard

import com.dk.focusguard.data.repository.DomainBlocklistRepositoryImpl
import com.dk.focusguard.services.vpn.DomainFilterDecision
import com.dk.focusguard.services.vpn.DomainFilterEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite verifying Domain Blocklist management, syntax validation,
 * parent/subdomain matching, Safe Search enforcement, and emergency overrides.
 * Covers requirements 1-7, 16, 17, 18.
 */
class DomainBlocklistAndFilterTest {

    private lateinit var blocklistRepo: DomainBlocklistRepositoryImpl
    private var isAdultProtectionEnabled = true
    private var isSafeSearchEnabled = true
    private var isEmergencyOverrideActive = false
    private lateinit var filterEngine: DomainFilterEngine

    @Before
    fun setUp() {
        isAdultProtectionEnabled = true
        isSafeSearchEnabled = true
        isEmergencyOverrideActive = false

        blocklistRepo = DomainBlocklistRepositoryImpl(
            preferencesManager = null, // In-memory mode for JVM unit tests
            initialCustomDomains = emptySet()
        )

        filterEngine = DomainFilterEngine(
            blocklistRepository = blocklistRepo,
            isAdultProtectionEnabled = { isAdultProtectionEnabled },
            isSafeSearchEnabled = { isSafeSearchEnabled },
            isEmergencyOverrideActive = { isEmergencyOverrideActive }
        )
    }

    @Test
    fun test1_adultDomainMatching_starterDomainsAreBlocked() {
        // Scenario 1: Exact match against known adult starter domain
        assertTrue(blocklistRepo.isDomainBlocked("pornhub.com"))
        assertTrue(blocklistRepo.isDomainBlocked("xvideos.com"))
        assertTrue(blocklistRepo.isDomainBlocked("onlyfans.com"))
        assertTrue(blocklistRepo.isDomainBlocked("chaturbate.com"))

        val decision = filterEngine.evaluateDomain("pornhub.com")
        assertTrue(decision is DomainFilterDecision.Block)
        assertEquals("pornhub.com", (decision as DomainFilterDecision.Block).domain)
    }

    @Test
    fun test2_subdomainMatching_subdomainsAreProperlyMatched() {
        // Scenario 2: Subdomain and nested subdomain matching
        assertTrue(blocklistRepo.isDomainBlocked("m.pornhub.com"))
        assertTrue(blocklistRepo.isDomainBlocked("video.cdn.xvideos.com"))
        assertTrue(blocklistRepo.isDomainBlocked("sub.deep.onlyfans.com"))

        val decision = filterEngine.evaluateDomain("mobile.xhamster.com")
        assertTrue(decision is DomainFilterDecision.Block)
    }

    @Test
    fun test3_unrelatedDomainAllowance_noFalsePositives() {
        // Scenario 3: Unrelated domains containing blocked names as substrings must NOT be blocked!
        assertFalse(blocklistRepo.isDomainBlocked("notpornhub.com"))
        assertFalse(blocklistRepo.isDomainBlocked("pornhubfans.org"))
        assertFalse(blocklistRepo.isDomainBlocked("google.com"))
        assertFalse(blocklistRepo.isDomainBlocked("wikipedia.org"))
        assertFalse(blocklistRepo.isDomainBlocked("github.com"))

        val decision = filterEngine.evaluateDomain("wikipedia.org")
        assertTrue(decision is DomainFilterDecision.Allow)
    }

    @Test
    fun test4_customDomainAddition_addsAndBlocksDomain() = runBlocking {
        // Scenario 4: User custom blocked domain addition
        val customDomain = "gambling-casino-test.com"
        assertFalse(blocklistRepo.isDomainBlocked(customDomain))

        val addResult = blocklistRepo.addCustomBlockedDomain(customDomain)
        assertTrue(addResult.isSuccess)
        assertTrue(blocklistRepo.isDomainBlocked(customDomain))
        assertTrue(blocklistRepo.isDomainBlocked("vip.gambling-casino-test.com"))

        val decision = filterEngine.evaluateDomain(customDomain)
        assertTrue(decision is DomainFilterDecision.Block)
    }

    @Test
    fun test5_customDomainRemoval_unblocksDomain() = runBlocking {
        // Scenario 5: User custom blocked domain removal
        val customDomain = "custom-distraction.net"
        blocklistRepo.addCustomBlockedDomain(customDomain)
        assertTrue(blocklistRepo.isDomainBlocked(customDomain))

        val removeResult = blocklistRepo.removeCustomBlockedDomain(customDomain)
        assertTrue(removeResult.isSuccess)
        assertFalse(blocklistRepo.isDomainBlocked(customDomain))

        val decision = filterEngine.evaluateDomain(customDomain)
        assertTrue(decision is DomainFilterDecision.Allow)
    }

    @Test
    fun test6_invalidDomainRejection_rejectsMalformedSyntax() = runBlocking {
        // Scenario 6: RFC 1035 syntax validation rejects bad domain formats
        assertFalse(blocklistRepo.isValidDomain(""))
        assertFalse(blocklistRepo.isValidDomain("invalid_nodots"))
        assertFalse(blocklistRepo.isValidDomain("-badstart.com"))
        assertFalse(blocklistRepo.isValidDomain("badend-.com"))
        assertFalse(blocklistRepo.isValidDomain("space in domain.com"))

        val result1 = blocklistRepo.addCustomBlockedDomain("bad_domain")
        assertTrue(result1.isFailure)

        val result2 = blocklistRepo.addCustomBlockedDomain("")
        assertTrue(result2.isFailure)
    }

    @Test
    fun test7_duplicateDomainHandling_deduplicatesEntries() = runBlocking {
        // Scenario 7: Deduplication of domain additions
        val domain = "duplicate-test.com"
        val initialSize = blocklistRepo.getAllBlockedDomainsDirect().size

        blocklistRepo.addCustomBlockedDomain(domain)
        val sizeAfterFirst = blocklistRepo.getAllBlockedDomainsDirect().size
        assertEquals(initialSize + 1, sizeAfterFirst)

        blocklistRepo.addCustomBlockedDomain(domain)
        val sizeAfterDuplicate = blocklistRepo.getAllBlockedDomainsDirect().size
        assertEquals(sizeAfterFirst, sizeAfterDuplicate) // Size remains unchanged!
    }

    @Test
    fun test16_emergencyOverride_suspendsDomainBlocking() {
        // Scenario 16: Emergency override active -> safe pass-through without blocking
        isEmergencyOverrideActive = true

        val decision = filterEngine.evaluateDomain("pornhub.com")
        assertTrue(decision is DomainFilterDecision.Allow)
    }

    @Test
    fun test17_protectionDisabled_permitsDomains() {
        // Scenario 17: Adult website protection toggle disabled
        isAdultProtectionEnabled = false

        val decision = filterEngine.evaluateDomain("pornhub.com")
        assertTrue(decision is DomainFilterDecision.Allow)
    }

    @Test
    fun test18_safeSearchPolicyConfiguration_enforcesSafeSearchVips() {
        // Scenario 18: Safe Search VIP redirection for Google, Bing, and YouTube
        isSafeSearchEnabled = true

        val googleDecision = filterEngine.evaluateDomain("www.google.com")
        assertTrue(googleDecision is DomainFilterDecision.SafeSearchRedirect)
        assertEquals(DomainFilterEngine.GOOGLE_SAFE_SEARCH_IP, (googleDecision as DomainFilterDecision.SafeSearchRedirect).redirectIp)

        val bingDecision = filterEngine.evaluateDomain("www.bing.com")
        assertTrue(bingDecision is DomainFilterDecision.SafeSearchRedirect)
        assertEquals(DomainFilterEngine.BING_SAFE_SEARCH_IP, (bingDecision as DomainFilterDecision.SafeSearchRedirect).redirectIp)

        val ytDecision = filterEngine.evaluateDomain("www.youtube.com")
        assertTrue(ytDecision is DomainFilterDecision.SafeSearchRedirect)
        assertEquals(DomainFilterEngine.YOUTUBE_RESTRICTED_IP, (ytDecision as DomainFilterDecision.SafeSearchRedirect).redirectIp)

        // When Safe Search is disabled, search engines resolve normally
        isSafeSearchEnabled = false
        val googleAllowed = filterEngine.evaluateDomain("www.google.com")
        assertTrue(googleAllowed is DomainFilterDecision.Allow)
    }

    @Test
    fun test_bulkDomainImport_validatesAndImports() = runBlocking {
        val importList = listOf(
            "site1.com",
            "site2.org",
            "invalid domain name",
            "site1.com", // duplicate in list
            "site3.net"
        )

        val result = blocklistRepo.importDomains(importList)
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow()) // 3 valid distinct domains added
        assertTrue(blocklistRepo.isDomainBlocked("site1.com"))
        assertTrue(blocklistRepo.isDomainBlocked("site2.org"))
        assertTrue(blocklistRepo.isDomainBlocked("site3.net"))
    }

    @Test
    fun test_adultTldAutoBlocking() {
        // Adult TLDs are automatically blocked regardless of specific domain
        assertTrue(blocklistRepo.isDomainBlocked("randomexplicit.xxx"))
        assertTrue(blocklistRepo.isDomainBlocked("sample.adult"))
        assertTrue(blocklistRepo.isDomainBlocked("videogallery.porn"))
        assertTrue(blocklistRepo.isDomainBlocked("clubmember.sex"))

        val decision = filterEngine.evaluateDomain("gallery.xxx")
        assertTrue(decision is DomainFilterDecision.Block)
    }

    @Test
    fun test_dohBootstrapInterception_forcesSystemDnsFallback() {
        // Known DoH bootstrap domains must be blocked to prevent bypass
        val googleDoh = filterEngine.evaluateDomain("dns.google")
        assertTrue(googleDoh is DomainFilterDecision.Block)

        val cloudflareDoh = filterEngine.evaluateDomain("cloudflare-dns.com")
        assertTrue(cloudflareDoh is DomainFilterDecision.Block)

        val quad9Doh = filterEngine.evaluateDomain("dns.quad9.net")
        assertTrue(quad9Doh is DomainFilterDecision.Block)

        // When emergency override is active, DoH domains are permitted
        isEmergencyOverrideActive = true
        val overrideDecision = filterEngine.evaluateDomain("dns.google")
        assertTrue(overrideDecision is DomainFilterDecision.Allow)
    }
}

