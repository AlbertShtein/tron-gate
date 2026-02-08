package ashtein.trongate.config

import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.tron.trident.core.ApiWrapper
import org.tron.trident.core.key.KeyPair

@Configuration
class TronClientConfig(
    @Value("\${parameters.node.base}") private val node: String,
    @Value("\${parameters.node.solidity}") private val nodeSolidity: String
) {
    private var apiWrapper: ApiWrapper? = null

    @Bean
    fun readOnlyApiWrapper(): ApiWrapper {
        val dummyKey = KeyPair.generate().toPrivateKey()
        val wrapper = ApiWrapper(node, nodeSolidity, dummyKey)
        apiWrapper = wrapper
        return wrapper
    }

    @PreDestroy
    fun shutdown() {
        apiWrapper?.let {
            it.channel.shutdownNow()
            it.channelSolidity.shutdownNow()
        }
    }
}
