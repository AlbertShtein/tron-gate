package ashtein.trongate.service.wallet

import ashtein.trongate.model.Wallet
import ashtein.trongate.repository.WalletRepository
import ashtein.trongate.vo.Private
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.tron.trident.core.ApiWrapper
import org.tron.trident.core.key.KeyPair
import java.util.UUID

open class WalletException(message: String) : Exception(message)
open class WalletNotFoundException(message: String) : WalletException(message)
open class WalletAddress(val public: String, val hex: String, val base58Check: String)
open class WalletAssets(val type: String, val balance: Double)

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    @param:Value("\${parameters.node.base}") private val node: String,
    @param:Value("\${parameters.node.solidity}") private val nodeSolidity: String,
    @param:Autowired private val adapters: List<AssetsAdapterInterface>
) {
    fun create(): String {
        val keyPair: KeyPair = KeyPair.generate()
        val wallet = Wallet(private = Private(keyPair.rawPair.privateKey.encoded))
        walletRepository.save(wallet)

        return wallet.id.toString()
    }

    @Throws(WalletException::class)
    fun getWalletAddress(id: String): WalletAddress {
        val wallet = this.getWallet(id)

        val keyPair = KeyPair(wallet.private.toHex())

        val address = WalletAddress(
            keyPair.toPublicKey(),
            keyPair.toHexAddress(),
            keyPair.toBase58CheckAddress(),
        )

        return address
    }

    @Throws(WalletException::class)
    fun getAssets(id: String): Sequence<WalletAssets> {
        val wallet = this.getWallet(id)
        val client = ApiWrapper(node, nodeSolidity, wallet.private.toHex())

        return sequence {
            try {
                for (adapter in adapters) {
                    yield(
                        WalletAssets(
                            adapter.getType(),
                            adapter.getBalance(client)
                        )
                    )
                }
            } finally {
                client.channel.shutdownNow()
                client.channelSolidity.shutdownNow()
            }
        }
    }

    @Throws(WalletException::class)
    private fun getWallet(id: String): Wallet {
        val opt = walletRepository.findById(UUID.fromString(id))
        if (opt.isEmpty) {
            throw WalletNotFoundException("No wallet found with id $id")
        }

        return opt.get()
    }
}