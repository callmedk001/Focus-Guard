package com.dk.focusguard.services.vpn

import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * Representation of a parsed DNS query request.
 */
data class ParsedDnsQuery(
    val transactionId: Int,
    val domain: String,
    val queryType: Int,
    val queryClass: Int,
    val isQuery: Boolean,
    val rawQuestionBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ParsedDnsQuery
        return transactionId == other.transactionId && domain == other.domain
    }

    override fun hashCode(): Int {
        return 31 * transactionId + domain.hashCode()
    }
}

/**
 * Low-level, allocation-efficient DNS packet processor.
 *
 * Implements RFC 1035, RFC 3596, and RFC 9460 DNS message parsing and synthesis.
 * Handles UDP DNS, IPv4, and IPv6 structures with strict bounds checking.
 * Guarantees zero crashes on malformed packets.
 */
class DnsPacketProcessor {

    companion object {
        const val DNS_PORT = 53
        const val TYPE_A = 1
        const val TYPE_CNAME = 5
        const val TYPE_TXT = 16
        const val TYPE_AAAA = 28
        const val TYPE_HTTPS = 65
        const val CLASS_IN = 1

        const val DEFAULT_SINKHOLE_IPV4 = "0.0.0.0"
        const val DEFAULT_SINKHOLE_IPV6 = "::"
        const val DEFAULT_TTL_SECONDS = 60

        /**
         * Calculates standard RFC 791 IPv4 16-bit one's complement checksum over header data.
         */
        fun calculateIpChecksum(data: ByteArray, offset: Int, length: Int): Int {
            var sum = 0
            var i = offset
            val end = offset + length
            while (i < end - 1) {
                val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
                sum += word
                i += 2
            }
            if (i < end) {
                sum += (data[i].toInt() and 0xFF) shl 8
            }
            while ((sum shr 16) > 0) {
                sum = (sum and 0xFFFF) + (sum shr 16)
            }
            return sum.inv() and 0xFFFF
        }
    }

    /**
     * Parses a raw DNS message payload into a structured ParsedDnsQuery.
     * Accurately supports compression pointers in QNAME and bounds checks labels.
     * Returns null if the packet is malformed, truncated, or not a standard query.
     */
    fun parseDnsQuery(data: ByteArray, offset: Int = 0, length: Int = data.size): ParsedDnsQuery? {
        if (length < 12 || offset + length > data.size || offset < 0) {
            return null // Minimum DNS header is 12 bytes
        }

        try {
            val buffer = ByteBuffer.wrap(data, offset, length)

            val transactionId = buffer.short.toInt() and 0xFFFF
            val flags = buffer.short.toInt() and 0xFFFF

            val isQuery = (flags and 0x8000) == 0
            val opcode = (flags shr 11) and 0x0F
            if (!isQuery || opcode != 0) {
                return null // Process only standard queries
            }

            val qdCount = buffer.short.toInt() and 0xFFFF
            if (qdCount < 1) {
                return null // Must contain at least one question
            }

            // Skip anCount, nsCount, arCount
            buffer.short
            buffer.short
            buffer.short

            val questionStart = buffer.position()

            // Parse QNAME with RFC 1035 compression pointer support
            val domainBuilder = StringBuilder()
            var currentPos = questionStart
            var jumped = false
            var pointerJumps = 0
            var finalQuestionEnd = -1

            while (true) {
                if (currentPos >= length) return null
                val rawLen = data[offset + currentPos].toInt() and 0xFF

                if (rawLen == 0) {
                    if (!jumped) {
                        finalQuestionEnd = currentPos + 1
                    }
                    break
                }

                // Check for RFC 1035 compression pointer (top 2 bits set: 0xC0)
                if ((rawLen and 0xC0) == 0xC0) {
                    if (currentPos + 1 >= length) return null
                    if (!jumped) {
                        finalQuestionEnd = currentPos + 2
                    }
                    if (++pointerJumps > 5) return null // Prevent compression loops

                    val pointerOffset = ((rawLen and 0x3F) shl 8) or (data[offset + currentPos + 1].toInt() and 0xFF)
                    if (pointerOffset >= length) return null
                    currentPos = pointerOffset
                    jumped = true
                    continue
                }

                // Regular label
                val labelLength = rawLen
                currentPos++
                if (currentPos + labelLength > length) return null

                if (domainBuilder.isNotEmpty()) {
                    domainBuilder.append('.')
                }

                val labelBytes = ByteArray(labelLength)
                System.arraycopy(data, offset + currentPos, labelBytes, 0, labelLength)
                domainBuilder.append(String(labelBytes, Charsets.US_ASCII))

                currentPos += labelLength
            }

            val qEndPos = if (finalQuestionEnd != -1) finalQuestionEnd else currentPos
            buffer.position(qEndPos)

            if (buffer.remaining() < 4) {
                return null // Must have QTYPE (2) + QCLASS (2)
            }

            val qType = buffer.short.toInt() and 0xFFFF
            val qClass = buffer.short.toInt() and 0xFFFF

            val rawQuestionEnd = buffer.position()
            val rawQuestion = ByteArray(rawQuestionEnd - questionStart)
            System.arraycopy(data, offset + questionStart, rawQuestion, 0, rawQuestion.size)

            return ParsedDnsQuery(
                transactionId = transactionId,
                domain = domainBuilder.toString().lowercase(),
                queryType = qType,
                queryClass = qClass,
                isQuery = true,
                rawQuestionBytes = rawQuestion
            )
        } catch (_: Exception) {
            return null // Malformed packet safely handled
        }
    }

    /**
     * Builds a synthesized DNS response for a blocked domain (sinkholed to 0.0.0.0 / :: or NXDOMAIN).
     * RFC 3596 compliant: outputs exactly 16 bytes for TYPE_AAAA and 4 bytes for TYPE_A.
     * Safely returns NXDOMAIN or empty answer for other query types (e.g. HTTPS type 65).
     */
    fun buildBlockedDnsResponse(
        query: ParsedDnsQuery,
        sinkholeIp: String = DEFAULT_SINKHOLE_IPV4,
        returnNxDomain: Boolean = false
    ): ByteArray {
        val out = ByteBuffer.allocate(512)

        // 1. Header
        out.putShort(query.transactionId.toShort())

        val shouldReturnAnswer = !returnNxDomain && (query.queryType == TYPE_A || query.queryType == TYPE_AAAA)

        val responseFlags = if (returnNxDomain) {
            // QR(1) + Opcode(0) + AA(1) + TC(0) + RD(1) + RA(1) + RCODE(3: NXDOMAIN)
            0x8183.toShort()
        } else {
            // QR(1) + Opcode(0) + AA(1) + TC(0) + RD(1) + RA(1) + RCODE(0: NoError)
            0x8180.toShort()
        }
        out.putShort(responseFlags)

        out.putShort(1.toShort()) // QDCOUNT = 1
        out.putShort((if (shouldReturnAnswer) 1 else 0).toShort()) // ANCOUNT = 1 (or 0 for non-A/AAAA or NXDOMAIN)
        out.putShort(0.toShort()) // NSCOUNT = 0
        out.putShort(0.toShort()) // ARCOUNT = 0

        // 2. Question section (echo query)
        out.put(query.rawQuestionBytes)

        // 3. Answer section (if returning sinkhole A or AAAA record)
        if (shouldReturnAnswer) {
            out.putShort(0xC00C.toShort()) // Name compression pointer -> offset 12 (QNAME)
            out.putShort(query.queryType.toShort())
            out.putShort(CLASS_IN.toShort())
            out.putInt(DEFAULT_TTL_SECONDS)

            val ipBytes = if (query.queryType == TYPE_AAAA) {
                // RFC 3596: AAAA record RDATA must be exactly 16 bytes
                runCatching { InetAddress.getByName(DEFAULT_SINKHOLE_IPV6).address }
                    .getOrDefault(ByteArray(16))
            } else {
                // RFC 1035: A record RDATA must be exactly 4 bytes
                runCatching { InetAddress.getByName(sinkholeIp).address }
                    .getOrDefault(byteArrayOf(0, 0, 0, 0))
            }

            out.putShort(ipBytes.size.toShort())
            out.put(ipBytes)
        }

        out.flip()
        val result = ByteArray(out.remaining())
        out.get(result)
        return result
    }

    /**
     * Builds a synthesized DNS response redirecting to a Safe Search VIP IP.
     */
    fun buildSafeSearchDnsResponse(query: ParsedDnsQuery, redirectIp: String): ByteArray {
        return buildBlockedDnsResponse(query, sinkholeIp = redirectIp, returnNxDomain = false)
    }

    /**
     * Inspects whether an IP packet is a UDP DNS query directed to port 53.
     * Supports standard IPv4 packets.
     */
    fun isUdpDnsPacket(packet: ByteArray, length: Int): Boolean {
        if (length < 28) return false // IPv4 (20) + UDP (8) minimum
        val version = (packet[0].toInt() shr 4) and 0x0F
        if (version != 4) return false

        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return false // Protocol 17 = UDP

        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (ihl < 20 || length < ihl + 8) return false

        val destPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
        return destPort == DNS_PORT
    }

    /**
     * Inspects whether an IP packet is an IPv6 UDP DNS query directed to port 53.
     */
    fun isIpv6UdpDnsPacket(packet: ByteArray, length: Int): Boolean {
        if (length < 48) return false // IPv6 header (40) + UDP (8) minimum
        val version = (packet[0].toInt() shr 4) and 0x0F
        if (version != 6) return false

        val nextHeader = packet[6].toInt() and 0xFF
        if (nextHeader != 17) return false // Protocol 17 = UDP

        val destPort = ((packet[42].toInt() and 0xFF) shl 8) or (packet[43].toInt() and 0xFF)
        return destPort == DNS_PORT
    }

    /**
     * Inspects whether an IP packet is a TCP DNS query directed to port 53.
     */
    fun isTcpDnsPacket(packet: ByteArray, length: Int): Boolean {
        if (length < 40) return false // IPv4 (20) + TCP (20) minimum
        val version = (packet[0].toInt() shr 4) and 0x0F
        if (version != 4) return false

        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 6) return false // Protocol 6 = TCP

        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (ihl < 20 || length < ihl + 20) return false

        val destPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
        return destPort == DNS_PORT
    }
}
