package ashtein.trongate.service.scanner

import ashtein.trongate.repository.WalletRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

@Service
class AddressCacheService(
    private val walletRepository: WalletRepository
) {
    private val log = LoggerFactory.getLogger(AddressCacheService::class.java)
    private val addresses: MutableSet<ByteBuffer> = ConcurrentHashMap.newKeySet()

    @PostConstruct
    fun initialize() {
        val all = walletRepository.findAll()
        for (wallet in all) {
            addresses.add(ByteBuffer.wrap(wallet.address.copyOf()))
        }
        log.info("Address cache loaded: {} wallets", addresses.size)
    }

    fun register(address: ByteArray) {
        addresses.add(ByteBuffer.wrap(address.copyOf()))
    }

    fun contains(address: ByteArray): Boolean {
        return addresses.contains(ByteBuffer.wrap(address))
    }
}
