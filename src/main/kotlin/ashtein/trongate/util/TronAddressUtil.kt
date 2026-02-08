package ashtein.trongate.util

import org.tron.trident.core.utils.Base58
import java.security.MessageDigest

object TronAddressUtil {
    fun decodeBase58Check(base58: String): ByteArray {
        val decoded = Base58.decode(base58)
        require(decoded.size == 25) { "Invalid address length" }
        val address = decoded.copyOfRange(0, 21)
        val checksum = decoded.copyOfRange(21, 25)
        val hash = sha256(sha256(address))
        require(hash.copyOfRange(0, 4).contentEquals(checksum)) { "Checksum mismatch" }
        return address
    }

    fun encodeBase58Check(address: ByteArray): String {
        val hash = sha256(sha256(address))
        return Base58.encode(address + hash.copyOfRange(0, 4))
    }

    private fun sha256(input: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(input)
    }
}
