package ashtein.trongate.service.wallet.adapter

import ashtein.trongate.service.wallet.AssetsAdapterInterface
import org.springframework.stereotype.Component
import org.tron.trident.abi.FunctionEncoder
import org.tron.trident.abi.FunctionReturnDecoder
import org.tron.trident.abi.TypeReference
import org.tron.trident.abi.datatypes.Address
import org.tron.trident.abi.datatypes.Function
import org.tron.trident.abi.datatypes.generated.Uint256
import org.tron.trident.core.ApiWrapper
import org.tron.trident.utils.Numeric

@Component
class UsdtAdapter: AssetsAdapterInterface {
    private val contractAddress = "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf"

    override fun getType(): String {
        return "USDT"
    }

    override fun getBalance(client: ApiWrapper): Double {
        val address = client.keyPair.toHexAddress()

        val function = Function(
            "balanceOf",
            listOf(Address(address)),
            listOf(object: TypeReference<Uint256>() {})
        )

        val encodedHex = FunctionEncoder.encode(function)
        val txnExt = client.triggerConstantContract(
            address,
            contractAddress,
            encodedHex
        )

        val result = Numeric.toHexString(txnExt.getConstantResult(0).toByteArray())
        val supply = FunctionReturnDecoder.decode(result, function.outputParameters).first().value

        return supply.toString().toDouble() / 1_000_000.0
    }
}