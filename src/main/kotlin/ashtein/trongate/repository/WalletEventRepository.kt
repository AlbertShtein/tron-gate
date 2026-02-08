package ashtein.trongate.repository

import ashtein.trongate.model.WalletEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface WalletEventRepository : JpaRepository<WalletEvent, UUID> {
    fun existsByTxHashAndWalletAddressAndDirection(
        txHash: String,
        walletAddress: String,
        direction: WalletEvent.Direction
    ): Boolean

    fun findByBlockNumberGreaterThanOrderByBlockNumberAsc(blockNumber: Long): List<WalletEvent>
}
