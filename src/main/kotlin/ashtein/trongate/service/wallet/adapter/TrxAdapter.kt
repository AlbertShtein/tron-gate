package ashtein.trongate.service.wallet.adapter

import ashtein.trongate.service.wallet.AssetsAdapterInterface
import org.springframework.stereotype.Component
import org.tron.trident.core.ApiWrapper

@Component
class TrxAdapter: AssetsAdapterInterface {
    override fun getType(): String {
        return "TRX"
    }

    override fun getBalance(client: ApiWrapper): Double {
        val balance = client
            .getAccountBalance(client.keyPair.toHexAddress())

        return balance.toDouble() / 1_000_000.0
    }
}