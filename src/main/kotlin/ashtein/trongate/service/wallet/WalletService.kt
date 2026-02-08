package ashtein.trongate.service.wallet

import ashtein.trongate.model.Wallet
import ashtein.trongate.repository.WalletRepository
import ashtein.trongate.service.scanner.AddressCacheService
import ashtein.trongate.util.TronAddressUtil
import ashtein.trongate.vo.Private
import org.bouncycastle.util.encoders.Hex
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.tron.trident.core.ApiWrapper
import org.tron.trident.core.key.KeyPair

open class WalletException(message: String) : Exception(message)
open class WalletNotFoundException(message: String) : WalletException(message)
open class WalletAssets(val type: String, val balance: Double)

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    @param:Value("\${parameters.node.base}") private val node: String,
    @param:Value("\${parameters.node.solidity}") private val nodeSolidity: String,
    @param:Autowired private val adapters: List<AssetsAdapterInterface>,
    private val addressCacheService: AddressCacheService
) {
    fun create(): String {
        val keyPair = KeyPair.generate()
        val addressBytes = Hex.decode(keyPair.toHexAddress())
        val wallet = Wallet(
            address = addressBytes,
            private = Private(keyPair.rawPair.privateKey.encoded)
        )
        walletRepository.save(wallet)
        addressCacheService.register(addressBytes)
        return keyPair.toBase58CheckAddress()
    }

    @Throws(WalletException::class)
    fun getAssets(address: String): Sequence<WalletAssets> {
        val wallet = this.getWallet(address)
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
    private fun getWallet(base58: String): Wallet {
        val addressBytes = TronAddressUtil.decodeBase58Check(base58)
        val opt = walletRepository.findById(addressBytes)
        if (opt.isEmpty) {
            throw WalletNotFoundException("No wallet found with address $base58")
        }

        return opt.get()
    }
}
