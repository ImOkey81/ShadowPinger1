package com.example.shadow.core.ip

import com.example.shadow.core.contracts.IpRange
import kotlin.math.pow

object IpAddressUtils {
    fun ipv4ToLong(ip: String): Long {
        val parts = ip.split(".")
        require(parts.size == 4) { "Invalid IPv4 address: $ip" }
        return parts.fold(0L) { acc, part ->
            val value = part.toLong()
            require(value in 0..255) { "Invalid IPv4 octet: $part" }
            (acc shl 8) + value
        }
    }

    fun longToIpv4(value: Long): String {
        val bytes = (0 until 4).map { index ->
            (value shr (8 * (3 - index)) and 0xFF).toInt()
        }
        return bytes.joinToString(".")
    }

    fun cidrToRange(cidr: String): IpRange {
        val parts = cidr.split("/")
        require(parts.size == 2) { "Invalid CIDR: $cidr" }
        val baseIp = ipv4ToLong(parts[0])
        val maskBits = parts[1].toInt()
        require(maskBits in 0..32) { "Invalid CIDR mask: $cidr" }
        val mask = if (maskBits == 0) 0L else (-1L shl (32 - maskBits)) and 0xFFFFFFFFL
        val network = baseIp and mask
        val hostCount = 2.0.pow(32 - maskBits.toDouble()).toLong()
        val broadcast = network + hostCount - 1
        val firstHost = if (maskBits >= 31) network else network + 1
        val lastHost = if (maskBits >= 31) broadcast else broadcast - 1
        return IpRange(from = firstHost, to = lastHost)
    }

    fun expandRange(range: IpRange): List<Long> {
        if (range.to < range.from) return emptyList()
        return (range.from..range.to).toList()
    }
}
