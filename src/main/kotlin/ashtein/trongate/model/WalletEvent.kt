package ashtein.trongate.model

import ashtein.trongate.util.UUIDv7
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigInteger
import java.util.UUID

@Entity
@Table(name = "wallet_event")
class WalletEvent(
    @Id
    val id: UUID = UUIDv7.randomUUID(),

    @Column(name = "wallet_address", nullable = false)
    val walletAddress: String,

    @Column(name = "tx_hash", nullable = false)
    val txHash: String,

    @Column(name = "block_number", nullable = false)
    val blockNumber: Long,

    @Column(name = "block_timestamp", nullable = false)
    val blockTimestamp: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    val eventType: EventType,

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    val direction: Direction,

    @Column(name = "from_address", nullable = false)
    val fromAddress: String,

    @Column(name = "to_address", nullable = false)
    val toAddress: String,

    @Column(name = "amount", nullable = false, precision = 38, scale = 0)
    val amount: BigInteger,

    @Column(name = "token_address")
    val tokenAddress: String? = null,

    @Column(name = "confirmed", nullable = false)
    var confirmed: Boolean = false
) {
    enum class EventType {
        TRX, TRC20, CONTRACT
    }

    enum class Direction {
        IN, OUT
    }
}
