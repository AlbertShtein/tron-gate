package ashtein.trongate.model

import ashtein.trongate.util.EncryptedStringConverter
import ashtein.trongate.util.IntegrityCheckListener
import ashtein.trongate.vo.Private
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "wallet")
@EntityListeners(IntegrityCheckListener::class)
class Wallet (
    @Id
    val address: ByteArray,

    @Convert(converter = EncryptedStringConverter::class)
    val private: Private,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    override var sign: ByteArray? = null
): Signable {
    override fun dataToSign(): ByteArray {
        return address + private.value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Wallet) return false
        return address.contentEquals(other.address)
    }

    override fun hashCode(): Int = address.contentHashCode()
}
