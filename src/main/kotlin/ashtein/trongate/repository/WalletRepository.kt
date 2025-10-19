package ashtein.trongate.repository

import ashtein.trongate.model.Wallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface WalletRepository: JpaRepository<Wallet, UUID>