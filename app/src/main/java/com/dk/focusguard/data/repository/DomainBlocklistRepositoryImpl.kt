package com.dk.focusguard.data.repository

import com.dk.focusguard.data.preferences.AppPreferencesManager
import com.dk.focusguard.domain.repository.DomainBlocklistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Production implementation of DomainBlocklistRepository.
 * Manages starter adult domains and persistent custom user domains.
 * Supports in-memory operation for local JVM unit testing.
 */
class DomainBlocklistRepositoryImpl(
    private val preferencesManager: AppPreferencesManager? = null,
    initialCustomDomains: Set<String> = emptySet(),
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null
) : DomainBlocklistRepository {

    companion object {
        // High-traffic, verified adult domains starter blocklist
        val STARTER_ADULT_DOMAINS = setOf(
            "pornhub.com",
            "xvideos.com",
            "xnxx.com",
            "xhamster.com",
            "redtube.com",
            "youporn.com",
            "chaturbate.com",
            "onlyfans.com",
            "cam4.com",
            "livejasmin.com",
            "bongacams.com",
            "stripchat.com",
            "spankbang.com",
            "tube8.com",
            "beeg.com",
            "brazzers.com",
            "eporner.com",
            "thumbzilla.com",
            "daftsex.com",
            "hqporner.com",
            "tnaflix.com",
            "drtuber.com",
            "pornmd.com",
            "sunporno.com",
            "fuq.com",
            "txxx.com",
            "nuvid.com",
            "hentaihaven.xxx",
            "hanime.tv",
            "rule34.xxx",
            "rule34.paheal.net",
            "gelbooru.com",
            "danbooru.donmai.us",
            "e-hentai.org",
            "nhentai.net",
            "tsumino.com",
            "hitomi.la",
            "fakku.net",
            "pururin.io",
            "luscious.net",
            "luscious.today",
            "heavy-r.com",
            "motherless.com",
            "porntrex.com",
            "xtapes.to",
            "sheshaft.com",
            "badjojo.com",
            "tblop.com",
            "porn.com",
            "pornhat.com",
            "pornone.com",
            "porndoe.com",
            "pornburst.xxx",
            "4tube.com",
            "fapdu.com",
            "gotporn.com",
            "palimas.com",
            "anysex.com",
            "freeones.com",
            "babes.com",
            "twistys.com",
            "mofos.com",
            "realitykings.com",
            "bangbros.com",
            "naughtyamerica.com",
            "digitalplayground.com",
            "evilangel.com",
            "wicked.com",
            "vixen.com",
            "blacked.com",
            "tushy.com",
            "deeper.com",
            "slushe.com",
            "fansly.com",
            "manyvids.com",
            "fancentro.com",
            "loyalfans.com",
            "myfreecams.com",
            "camsoda.com",
            "flirt4free.com",
            "imlive.com",
            "streamate.com",
            "jasmin.com",
            "camwhores.tv",
            "camwhores.video",
            "camcaps.me",
            "pornbox.com",
            "pornhd.com",
            "porndig.com",
            "porngo.com",
            "flyflv.com",
            "megatube.xxx",
            "tubegalore.com",
            "cliphunter.com",
            "empflix.com",
            "porn300.com",
            "upornia.com",
            "hclips.com",
            "zbporn.com",
            "javfree.sh",
            "javhd.com",
            "javlibrary.com",
            "javmost.com",
            "javbus.com",
            "missav.com",
            "jable.tv",
            "7mmtv.tv",
            "avgle.com",
            "xcafe.com",
            "youjizz.com",
            "keezmovies.com",
            "extremetube.com",
            "hotshame.com",
            "pornsharing.com",
            "cumlouder.com",
            "pornve.com",
            "fantasti.cc",
            "fapster.xxx",
            "upornia.com",
            "fuskator.com",
            "erome.com",
            "bravoteens.com",
            "czechav.com",
            "privatedelights.ch",
            "tryst.link",
            "eroticmonkey.ch",
            "skip-the-games.com",
            "escortalligator.com",
            "eros.com",
            "adultsearch.com",
            "adultwork.com",
            "vivastreet.co.uk",
            "bedpage.com",
            "yespornplease.com",
            "porndroids.com",
            "hdpornfull.com",
            "bravotube.net",
            "pandamovies.org",
            "porn555.com",
            "porndude.com",
            "theporndude.com",
            "pornktube.com",
            "pornovideoshub.com",
            "netpornsex.com",
            "pornflip.com",
            "pornhat.one",
            "xozilla.com",
            "alohatube.com",
            "porndish.com",
            "porntube.com"
        )

        val ADULT_TLDS = setOf(".xxx", ".adult", ".porn", ".sex")

        private val DOMAIN_REGEX = Regex("^([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}$")
    }

    override val defaultBlockedDomains: Set<String> = STARTER_ADULT_DOMAINS

    // In-memory state flow used when preferencesManager is absent or for immediate reads
    private val inMemoryCustom = MutableStateFlow(initialCustomDomains.map { it.lowercase().trim() }.toSet())

    override val customBlockedDomains: Flow<Set<String>> =
        preferencesManager?.customBlockedDomains ?: inMemoryCustom.asStateFlow()

    override val allBlockedDomains: Flow<Set<String>> = customBlockedDomains.map { custom ->
        STARTER_ADULT_DOMAINS + custom
    }

    private var cachedCustomDomains: Set<String> = initialCustomDomains.map { it.lowercase().trim() }.toSet()

    init {
        preferencesManager?.let { pm ->
            val scope = coroutineScope ?: kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
            scope.launch {
                pm.customBlockedDomains.collect { domains ->
                    cachedCustomDomains = domains.map { it.lowercase().trim() }.toSet()
                    inMemoryCustom.value = cachedCustomDomains
                }
            }
        }
    }

    override fun getAllBlockedDomainsDirect(): Set<String> {
        return STARTER_ADULT_DOMAINS + cachedCustomDomains
    }

    override fun isValidDomain(domain: String): Boolean {
        val clean = domain.trim().lowercase().removeSuffix(".")
        if (clean.length !in 3..253) return false
        return DOMAIN_REGEX.matches(clean)
    }

    override fun isDomainBlocked(domain: String): Boolean {
        val clean = domain.trim().lowercase().removeSuffix(".")
        if (clean.isBlank()) return false

        // Check if top-level domain is an adult TLD
        if (ADULT_TLDS.any { clean.endsWith(it) }) {
            return true
        }

        val activeBlocklist = getAllBlockedDomainsDirect()

        return activeBlocklist.any { blocked ->
            clean == blocked || clean.endsWith(".$blocked")
        }
    }

    override suspend fun addCustomBlockedDomain(domain: String): Result<Unit> {
        val clean = domain.trim().lowercase().removeSuffix(".")
        if (!isValidDomain(clean)) {
            return Result.failure(IllegalArgumentException("Invalid domain format: $domain"))
        }

        cachedCustomDomains = cachedCustomDomains + clean
        inMemoryCustom.value = cachedCustomDomains
        preferencesManager?.addCustomBlockedDomain(clean)
        return Result.success(Unit)
    }

    override suspend fun removeCustomBlockedDomain(domain: String): Result<Unit> {
        val clean = domain.trim().lowercase().removeSuffix(".")
        cachedCustomDomains = cachedCustomDomains - clean
        inMemoryCustom.value = cachedCustomDomains
        preferencesManager?.removeCustomBlockedDomain(clean)
        return Result.success(Unit)
    }

    override suspend fun importDomains(domains: List<String>): Result<Int> {
        val validDomains = domains
            .map { it.trim().lowercase().removeSuffix(".") }
            .filter { isValidDomain(it) }
            .toSet()

        if (validDomains.isEmpty()) {
            return Result.success(0)
        }

        val updated = cachedCustomDomains + validDomains
        val newlyAddedCount = updated.size - cachedCustomDomains.size
        cachedCustomDomains = updated
        inMemoryCustom.value = updated
        preferencesManager?.setCustomBlockedDomains(updated)
        return Result.success(newlyAddedCount)
    }

    override suspend fun clearCustomDomains(): Result<Unit> {
        cachedCustomDomains = emptySet()
        inMemoryCustom.value = emptySet()
        preferencesManager?.setCustomBlockedDomains(emptySet())
        return Result.success(Unit)
    }
}
