package ashtein.trongate.model

import ashtein.trongate.util.EncryptedStringConverter
import ashtein.trongate.util.UUIDv7
import ashtein.trongate.vo.Private
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "wallet")
class Wallet (
    @Id
    val id: UUID = UUIDv7.randomUUID(),

    @Convert(converter = EncryptedStringConverter::class)
    val private: Private
)