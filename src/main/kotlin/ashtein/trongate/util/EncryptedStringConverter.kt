package ashtein.trongate.util

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
    @param:Value("\${parameters.encryption.aes256.base64}") private val key: String
): AttributeConverter<String, String> {
    private val algorithm = "AES"
    private val transformation = "AES/GCM/NoPadding"
    private val ivLength = 12 // Bytes
    private val gcmTagLength = 128 // Bits

    override fun convertToDatabaseColumn(attribute: String): String {
        val iv = ByteArray(ivLength).apply { SecureRandom().nextBytes(this) }
        val gcmSpec = GCMParameterSpec(gcmTagLength, iv)
        val keyBytes = Base64.getDecoder().decode(key)
        val keySpec = SecretKeySpec(keyBytes, algorithm)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val encrypted = cipher.doFinal(attribute.toByteArray())

        return Base64.getEncoder().encodeToString(iv + encrypted)
    }

    override fun convertToEntityAttribute(dbData: String): String {
        val combined = Base64.getDecoder().decode(dbData)
        val iv = combined.sliceArray(0..<ivLength)
        val gcmSpec = GCMParameterSpec(gcmTagLength, iv)
        val encryptedBytes = combined.sliceArray(ivLength until combined.size)
        val keyBytes = Base64.getDecoder().decode(key)
        val keySpec = SecretKeySpec(keyBytes, algorithm)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        val decryptedBytes = cipher.doFinal(encryptedBytes)

        return String(decryptedBytes)
    }

}