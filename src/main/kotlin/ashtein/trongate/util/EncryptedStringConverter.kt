package ashtein.trongate.util

import ashtein.trongate.vo.Private
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.beans.factory.annotation.Value
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Converter(autoApply = true)
class EncryptedStringConverter(
    @param:Value("\${parameters.encryption.256.base64}") private val key: String
): AttributeConverter<Private, ByteArray> {
    private val algorithm = "AES"
    private val transformation = "AES/GCM/NoPadding"
    private val ivLength = 12 // Bytes
    private val gcmTagLength = 128 // Bits

    override fun convertToDatabaseColumn(attribute: Private): ByteArray {
        return encrypt(attribute.value)
    }

    override fun convertToEntityAttribute(dbData: ByteArray): Private {
        return Private(decrypt(dbData))
    }

    private fun encrypt(attribute: ByteArray): ByteArray {
        val iv = ByteArray(ivLength).apply { SecureRandom().nextBytes(this) }
        val gcmSpec = GCMParameterSpec(gcmTagLength, iv)
        val keyBytes = Base64.getDecoder().decode(key)
        val keySpec = SecretKeySpec(keyBytes, algorithm)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        return iv + cipher.doFinal(attribute)
    }

    private fun decrypt(dbData: ByteArray): ByteArray {
        val iv = dbData.sliceArray(0..<ivLength)
        val gcmSpec = GCMParameterSpec(gcmTagLength, iv)
        val encryptedBytes = dbData.sliceArray(ivLength until dbData.size)
        val keyBytes = Base64.getDecoder().decode(key)
        val keySpec = SecretKeySpec(keyBytes, algorithm)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        return cipher.doFinal(encryptedBytes)
    }
}