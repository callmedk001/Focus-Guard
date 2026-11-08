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
 * Hardening tests verifying that SafeSearch does NOT break essential Google services
 * (Gmail, Drive, Google Accounts, Play Store), and that adult domains are reliably filtered
 * with proper emergency override bypass and custom domain persistence.
 */
class SafeSearchAndDomainFilterHardeningTest {

    private lateinit var blocklistRepository: DomainBlocklistRepositoryImpl
    private lateinit var filterEngine: DomainFilterEngine
    private var isAdultEnabled = true
    private var isSafeSearchEnabled = true
    private var isOverrideActive = false

    @Before
    fun setUp() {
        blocklistRepository = DomainBlocklistRepositoryImpl()
        isAdultEnabled = true
        isSafeSearchEnabled = true
        isOverrideActive = false

        filterEngine = DomainFilterEngine(
            blocklistRepository = blocklistRepository,
            isAdultProtectionEnabled = { isAdultEnabled },
            isSafeSearchEnabled = { isSafeSearchEnabled },
            isEmergencyOverrideActive = { isOverrideActive }
        )
    }

    @Test
    fun testSafeSearch_essentialGoogleServices_mustNeverBeRedirected() {
        val nonSearchGoogleDomains = listOf(
            "mail.google.com",
            "drive.google.com",
            "docs.google.com",
            "accounts.google.com",
            "play.google.com",
            "calendar.google.com",
            "photos.google.com",
            "maps.google.com",
            "translate.google.com",
            "myaccount.google.com",
            "apis.google.com",
            "cloud.google.com",
            "developer.google.com",
            "firebase.google.com",
            "meet.google.com",
            "chat.google.com",
            "contacts.google.com",
            "news.google.com"
        )

        for (domain in nonSearchGoogleDomains) {
            val decision = filterEngine.evaluateDomain(domain)
            assertTrue(
                "Critical Google service '$domain' must be allowed (NOT redirected to SafeSearch VIP)",
                decision is DomainFilterDecision.Allow
            )
        }
    }

    @Test
    fun testSafeSearch_legitimateSearchDomains_properlyRedirected() {
        val searchDomains = listOf(
            "google.com" to DomainFilterEngine.GOOGLE_SAFE_SEARCH_IP,
            "www.google.com" to DomainFilterEngine.GOOGLE_SAFE_SEARCH_IP,
            "google.co.in" to DomainFilterEngine.GOOGLE_SAFE_SEARCH_IP,
            "www.google.co.in" to DomainFilterEngine.GOOGLE_SAFE_SEARCH_IP,
            "www.google.co.uk" to DomainFilterEngine.GOOGLE_SAFE_SEARCH_IP,
            "bing.com" to DomainFilterEngine.BING_SAFE_SEARCH_IP,
            "www.bing.com" to DomainFilterEngine.BING_SAFE_SEARCH_IP,
            "youtube.com" to DomainFilterEngine.YOUTUBE_RESTRICTED_IP,
            "www.youtube.com" to DomainFilterEngine.YOUTUBE_RESTRICTED_IP
        )

        for ((domain, expectedIp) in searchDomains) {
            val decision = filterEngine.evaluateDomain(domain)
            assertTrue("Domain '$domain' must be redirected to SafeSearch VIP", decision is DomainFilterDecision.SafeSearchRedirect)
            val redirect = decision as DomainFilterDecision.SafeSearchRedirect
            assertEquals(expectedIp, redirect.redirectIp)
        }
    }

    @Test
    fun testAdultDomain_starterBlocklist_properlyBlocked() {
        val blockedDomains = listOf(
            "pornhub.com",
            "sub.pornhub.com",
            "xvideos.com",
            "m.xvideos.com",
            "xnxx.com",
            "onlyfans.com",
            "creator.onlyfans.com"
        )

        for (domain in blockedDomains) {
            val decision = filterEngine.evaluateDomain(domain)
            assertTrue("Adult domain '$domain' must be blocked", decision is DomainFilterDecision.Block)
        }
    }

    @Test
    fun testNormalWebsites_allowedWithoutInterference() {
        val legitimateSites = listOf(
            "wikipedia.org",
            "github.com",
            "stackoverflow.com",
            "android.com",
            "oppo.com",
            "coloros.com",
            "kotlinlang.org"
        )

        for (domain in legitimateSites) {
            val decision = filterEngine.evaluateDomain(domain)
            assertTrue("Legitimate site '$domain' must be allowed", decision is DomainFilterDecision.Allow)
        }
    }

    @Test
    fun testEmergencyOverride_bypassesAllRestrictions() {
        // Adult protection is enabled, but emergency override is ACTIVE
        isOverrideActive = true

        val adultDecision = filterEngine.evaluateDomain("pornhub.com")
        assertTrue("When emergency override is active, all domains must be allowed", adultDecision is DomainFilterDecision.Allow)

        val googleDecision = filterEngine.evaluateDomain("google.com")
        assertTrue("When emergency override is active, SafeSearch redirection is suspended", googleDecision is DomainFilterDecision.Allow)
    }

    @Test
    fun testCustomDomains_additionAndInstantEnforcement() = runBlocking {
        val customDomain = "distraction-vault.xyz"
        assertFalse(blocklistRepository.isDomainBlocked(customDomain))

        val addResult = blocklistRepository.addCustomBlockedDomain(customDomain)
        assertTrue(addResult.isSuccess)
        assertTrue(blocklistRepository.isDomainBlocked(customDomain))
        assertTrue(blocklistRepository.isDomainBlocked("sub.$customDomain"))

        val decision = filterEngine.evaluateDomain(customDomain)
        assertTrue("Custom domain must be blocked by engine", decision is DomainFilterDecision.Block)

        blocklistRepository.removeCustomBlockedDomain(customDomain)
        assertFalse(blocklistRepository.isDomainBlocked(customDomain))
    }
}

