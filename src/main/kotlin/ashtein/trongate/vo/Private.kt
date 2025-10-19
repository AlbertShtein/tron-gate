package ashtein.trongate.vo

import org.bouncycastle.util.encoders.Hex

class Private (val value: ByteArray) {
    fun toHex(): String {
        return Hex.toHexString(value)
    }
}