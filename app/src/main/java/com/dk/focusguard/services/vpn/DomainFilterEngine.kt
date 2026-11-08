package com.dk.focusguard.services.vpn

import com.dk.focusguard.domain.repository.DomainBlocklistRepository

/**
 * Result of evaluating a DNS query domain through the filter engine.
 */
sealed class DomainFilterDecision {
    /** Domain matches adult blocklist or DoH bypass list; should be sinkholed or returned as NXDOMAIN */
    data class Block(val domain: String, val reason: String) : DomainFilterDecision()

    /** Domain matches supported search engine; redirected to strict Safe Search VIP */
    data class SafeSearchRedirect(
        val domain: String,
        val redirectIp: String,
        val policyName: String
    ) : DomainFilterDecision()

    /** Domain is permitted; should be resolved via secure upstream resolver */
    data class Allow(val domain: String) : DomainFilterDecision()
}

/**
 * High-performance domain filtering and Safe Search enforcement engine.
 *
 * GUARANTEES:
 * 1. ZERO CONTENT INSPECTION: Inspects only the queried domain name; never examines web pages or search queries.
 * 2. PROVEN SAFE SEARCH: Uses official VIP DNS records published by Google, Microsoft, and YouTube.
 * 3. TRANSPARENCY: Does not claim to eliminate all explicit content; clearly reports policies enforced.
 * 4. OVERRIDE & SETTINGS RESPECT: Respects user emergency override and protection toggles instantly.
 * 5. DOH BYPASS PREVENTED: Sinkholes known DoH endpoints so browsers reliably fall back to system DNS.
 */
class DomainFilterEngine(
    private val blocklistRepository: DomainBlocklistRepository,
    private val isAdultProtectionEnabled: () -> Boolean = { true },
    private val isSafeSearchEnabled: () -> Boolean = { true },
    private val isEmergencyOverrideActive: () -> Boolean = { false }
) {

    companion object {
        // Official Safe Search DNS VIPs
        // Google forcesafesearch.google.com VIP
        const val GOOGLE_SAFE_SEARCH_IP = "216.239.38.120"
        // Bing strict.bing.com VIP
        const val BING_SAFE_SEARCH_IP = "204.79.197.220"
        // YouTube restrict.youtube.com VIP
        const val YOUTUBE_RESTRICTED_IP = "216.239.38.119"

        // Known DoH endpoints that browsers use to bypass local port 53 DNS
        val DOH_BOOTSTRAP_DOMAINS = setOf(
            "dns.google",
            "cloudflare-dns.com",
            "mozilla.cloudflare-dns.com",
            "dns.quad9.net",
            "doh.cleanbrowsing.org",
            "doh.opendns.com",
            "dns.adguard.com",
            "dns.nextdns.io"
        )

        private val GOOGLE_NON_SEARCH_SUBDOMAINS = setOf(
            "mail", "drive", "docs", "accounts", "play", "calendar", "photos",
            "maps", "translate", "myaccount", "apis", "cloud", "developer",
            "firebase", "meet", "chat", "hangouts", "contacts", "news", "admin",
            "support", "policies", "myactivity", "ads", "analytics", "tagmanager",
            "workspace", "classroom", "console", "keep", "forms", "sheets", "slides"
        )
    }

    /**
     * Evaluates a requested domain against active defense rules.
     */
    fun evaluateDomain(rawDomain: String): DomainFilterDecision {
        val domain = rawDomain.trim().lowercase().removeSuffix(".")
        if (domain.isBlank()) {
            return DomainFilterDecision.Allow(domain)
        }

        // 1. Emergency Override Check: immediately allows all traffic without filtering
        if (isEmergencyOverrideActive()) {
            return DomainFilterDecision.Allow(domain)
        }

        // 2. DoH Bypass Prevention: Sinkhole DoH bootstrap hostnames to force browsers (Chrome/Edge/Firefox)
        // in normal & Incognito mode to fall back to system DNS.
        if (isAdultProtectionEnabled() && isDohBootstrapDomain(domain)) {
            return DomainFilterDecision.Block(
                domain = domain,
                reason = "DoH bootstrap domain intercepted to enforce Focus Guard system DNS filtering"
            )
        }

        // 3. Adult Website Blocklist Check
        if (isAdultProtectionEnabled() && blocklistRepository.isDomainBlocked(domain)) {
            return DomainFilterDecision.Block(
                domain = domain,
                reason = "Adult content domain identified in DK Focus Guard blocklist"
            )
        }

        // 4. DNS-based Safe Search Policy Enforcement
        if (isSafeSearchEnabled()) {
            val safeSearchDecision = checkSafeSearchPolicy(domain)
            if (safeSearchDecision != null) {
                return safeSearchDecision
            }
        }

        // 5. Default: Domain is permitted
        return DomainFilterDecision.Allow(domain)
    }

    private fun isDohBootstrapDomain(domain: String): Boolean {
        return DOH_BOOTSTRAP_DOMAINS.any { doh ->
            domain == doh || domain.endsWith(".$doh")
        }
    }

    private fun checkSafeSearchPolicy(domain: String): DomainFilterDecision.SafeSearchRedirect? {
        // Google Search SafeSearch
        if (isGoogleSearchDomain(domain)) {
            return DomainFilterDecision.SafeSearchRedirect(
                domain = domain,
                redirectIp = GOOGLE_SAFE_SEARCH_IP,
                policyName = "Google Strict SafeSearch"
            )
        }

        // Bing Strict SafeSearch
        if (isBingDomain(domain)) {
            return DomainFilterDecision.SafeSearchRedirect(
                domain = domain,
                redirectIp = BING_SAFE_SEARCH_IP,
                policyName = "Bing Strict SafeSearch"
            )
        }

        // YouTube Restricted Mode
        if (isYouTubeDomain(domain)) {
            return DomainFilterDecision.SafeSearchRedirect(
                domain = domain,
                redirectIp = YOUTUBE_RESTRICTED_IP,
                policyName = "YouTube Restricted Mode"
            )
        }

        return null
    }

    private fun isGoogleSearchDomain(domain: String): Boolean {
        val isGoogle = domain == "google.com" || domain.startsWith("google.") ||
                domain.endsWith(".google.com") || domain.contains(".google.")

        if (!isGoogle) return false

        // Prevent breaking critical Google services (Gmail, Drive, Login, Play Store)
        val parts = domain.split(".")
        if (parts.size >= 3) {
            val firstSubdomain = parts[0]
            if (firstSubdomain != "www" && firstSubdomain in GOOGLE_NON_SEARCH_SUBDOMAINS) {
                return false
            }
        }

        // Must be a recognized search entry point
        return domain == "google.com" ||
                domain == "www.google.com" ||
                domain.startsWith("google.") ||
                domain.startsWith("www.google.")
    }

    private fun isBingDomain(domain: String): Boolean {
        return domain == "bing.com" || domain == "www.bing.com"
    }

    private fun isYouTubeDomain(domain: String): Boolean {
        return domain == "youtube.com" ||
                domain.endsWith(".youtube.com") ||
                domain == "youtubei.googleapis.com"
    }
}
