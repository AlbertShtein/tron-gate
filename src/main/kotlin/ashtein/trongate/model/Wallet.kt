package ashtein.trongate.model

import ashtein.trongate.util.EncryptedStringConverter
import ashtein.trongate.util.UUIDv7
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "wallet")
class Wallet (
    @Id
    val id: String = UUIDv7.randomUUID().toString(),

    @Convert(converter = EncryptedStringConverter::class)
    val private: String
)