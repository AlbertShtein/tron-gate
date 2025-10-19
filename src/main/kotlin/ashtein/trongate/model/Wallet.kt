package ashtein.trongate.model

import ashtein.trongate.util.EncryptedStringConverter
import ashtein.trongate.util.IntegrityCheckListener
import ashtein.trongate.util.UUIDv7
import ashtein.trongate.vo.Private
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "wallet")
@EntityListeners(IntegrityCheckListener::class)
class Wallet (
    @Id
    val id: UUID = UUIDv7.randomUUID(),

    @Convert(converter = EncryptedStringConverter::class)
    val private: Private,

    override var sign: ByteArray? = null
): Signable {
    override fun dataToSign(): ByteArray {
        val idBA = id
            .toString()
            .replace("-", "")
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

        return idBA + private.value
    }
}