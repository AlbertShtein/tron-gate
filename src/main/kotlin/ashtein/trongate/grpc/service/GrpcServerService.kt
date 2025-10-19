package ashtein.trongate.grpc.service

import ashtein.trongate.proto.AddressResponseDto
import ashtein.trongate.proto.AssetsResponseDto
import ashtein.trongate.proto.EmptyDto
import ashtein.trongate.proto.TronGateGrpc
import ashtein.trongate.proto.WalletIdDto
import ashtein.trongate.proto.WalletResponseDto
import ashtein.trongate.service.wallet.WalletException
import ashtein.trongate.service.wallet.WalletNotFoundException
import ashtein.trongate.service.wallet.WalletService
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService
import java.lang.Exception

@GrpcService
class GrpcServerService(private val walletService: WalletService): TronGateGrpc.TronGateImplBase() {

    override fun createWallet(request: EmptyDto, response: StreamObserver<WalletResponseDto>) {
        try {
            val id = walletService.create()

            val dto = WalletResponseDto
                .newBuilder()
                .setId(id)
                .build()

            response.onNext(dto)
            response.onCompleted()
        } catch (e: WalletException) {
            response.onError(StatusRuntimeException(Status.ABORTED.withDescription(e.message)))
        } catch (e: Exception) {
            response.onError(StatusRuntimeException(Status.INTERNAL.withDescription(e.message)))
        }
    }

    override fun getAddress(request: WalletIdDto, response: StreamObserver<AddressResponseDto>) {
        try {
            val address = walletService.getWalletAddress(request.id)

            val dto = AddressResponseDto
                .newBuilder()
                .setHex(address.hex)
                .setBase58Check(address.base58Check)
                .build()

            response.onNext(dto)
            response.onCompleted()
        } catch (e: WalletNotFoundException) {
            response.onError(StatusRuntimeException(Status.NOT_FOUND.withDescription(e.message)))
        } catch (e: WalletException) {
            response.onError(StatusRuntimeException(Status.UNKNOWN.withDescription(e.message)))
        } catch (e: Exception) {
            response.onError(StatusRuntimeException(Status.INTERNAL.withDescription(e.message)))
        }
    }

    override fun getAssets(request: WalletIdDto, response: StreamObserver<AssetsResponseDto>) {
        try {
            val assets = walletService.getAssets(request.id)

            for (asset in assets) {
                response.onNext(
                    AssetsResponseDto
                        .newBuilder()
                        .setType(asset.type)
                        .setBalance(asset.balance)
                        .build()
                )
            }

            response.onCompleted()
        } catch (e: WalletNotFoundException) {
            response.onError(StatusRuntimeException(Status.NOT_FOUND.withDescription(e.message)))
        } catch (e: WalletException) {
            response.onError(StatusRuntimeException(Status.UNKNOWN.withDescription(e.message)))
        } catch (e: Exception) {
            response.onError(StatusRuntimeException(Status.INTERNAL.withDescription(e.message)))
        }
    }
}
