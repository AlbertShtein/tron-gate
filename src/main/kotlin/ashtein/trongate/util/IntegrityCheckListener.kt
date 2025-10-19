package ashtein.trongate.util

import ashtein.trongate.model.Signable
import jakarta.persistence.PostLoad
import jakarta.persistence.PrePersist
import org.springframework.beans.factory.annotation.Value
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class IntegrityCheckListener(
    @param:Value("\${parameters.encryption.256.base64}") private val key: String,
) {
    private val signAlgorithm = "HmacSHA256"

    @PrePersist
    fun initializeSignature(entity: Signable) {
        if (entity.sign != null) {
            return
        }

        entity.sign = sign(entity.dataToSign())
    }

    @PostLoad
    fun verifySignature(entity: Signable) {
        val expectedMac = sign(entity.dataToSign())

        if (!expectedMac.contentEquals(entity.sign)) {
            throw SecurityException("Data integrity violation: HMAC mismatch")
        }
    }

    private fun sign(data: ByteArray): ByteArray {
        val keyBytes = Base64.getDecoder().decode(key)
        val mac = Mac.getInstance(signAlgorithm)
        mac.init(SecretKeySpec(keyBytes, signAlgorithm))

        return mac.doFinal(data)
    }
}