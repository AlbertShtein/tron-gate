package ashtein.trongate.service.wallet

import org.tron.trident.core.ApiWrapper

interface AssetsAdapterInterface: AdapterInterface {
    fun getBalance(client: ApiWrapper): Double
}