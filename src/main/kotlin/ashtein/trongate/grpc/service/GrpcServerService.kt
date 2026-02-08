package ashtein.trongate.grpc.service

import ashtein.trongate.proto.AssetsResponseDto
import ashtein.trongate.proto.EmptyDto
import ashtein.trongate.proto.SubscribeEventsRequest
import ashtein.trongate.proto.TronGateGrpc
import ashtein.trongate.proto.WalletAddressDto
import ashtein.trongate.proto.WalletEventDto
import ashtein.trongate.proto.WalletResponseDto
import ashtein.trongate.repository.WalletEventRepository
import ashtein.trongate.service.scanner.EventBroadcastService
import ashtein.trongate.service.wallet.WalletException
import ashtein.trongate.service.wallet.WalletNotFoundException
import ashtein.trongate.service.wallet.WalletService
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService

@GrpcService
class GrpcServerService(
    private val walletService: WalletService,
    private val eventBroadcastService: EventBroadcastService,
    private val walletEventRepository: WalletEventRepository
): TronGateGrpc.TronGateImplBase() {

    override fun createWallet(request: EmptyDto, response: StreamObserver<WalletResponseDto>) {
        try {
            val address = walletService.create()

            val dto = WalletResponseDto
                .newBuilder()
                .setAddress(address)
                .build()

            response.onNext(dto)
            response.onCompleted()
        } catch (e: WalletException) {
            response.onError(StatusRuntimeException(Status.ABORTED.withDescription(e.message ?: "Wallet error")))
        } catch (e: Exception) {
            response.onError(StatusRuntimeException(Status.INTERNAL.withDescription(e.message ?: "Unknown error")))
        }
    }

    override fun getAssets(request: WalletAddressDto, response: StreamObserver<AssetsResponseDto>) {
        try {
            val assets = walletService.getAssets(request.address)

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
            response.onError(StatusRuntimeException(Status.NOT_FOUND.withDescription(e.message ?: "Wallet not found")))
        } catch (e: WalletException) {
            response.onError(StatusRuntimeException(Status.UNKNOWN.withDescription(e.message ?: "Wallet error")))
        } catch (e: Exception) {
            response.onError(StatusRuntimeException(Status.INTERNAL.withDescription(e.message ?: "Unknown error")))
        }
    }

    override fun subscribeEvents(request: SubscribeEventsRequest, responseObserver: StreamObserver<WalletEventDto>) {
        try {
            if (request.lastBlockNumber >= 0) {
                val missed = walletEventRepository
                    .findByBlockNumberGreaterThanOrderByBlockNumberAsc(request.lastBlockNumber)
                for (event in missed) {
                    responseObserver.onNext(toDto(event))
                }
            }

            val unsubscribe = eventBroadcastService.subscribe { event ->
                responseObserver.onNext(toDto(event))
            }

            val serverObserver = responseObserver as? ServerCallStreamObserver<WalletEventDto>
            serverObserver?.setOnCancelHandler {
                unsubscribe()
            }
        } catch (e: Exception) {
            responseObserver.onError(
                StatusRuntimeException(Status.INTERNAL.withDescription(e.message ?: "Unknown error"))
            )
        }
    }

    private fun toDto(event: ashtein.trongate.model.WalletEvent): WalletEventDto {
        return WalletEventDto.newBuilder()
            .setWalletAddress(event.walletAddress)
            .setTxHash(event.txHash)
            .setBlockNumber(event.blockNumber)
            .setBlockTimestamp(event.blockTimestamp)
            .setEventType(event.eventType.name)
            .setDirection(event.direction.name)
            .setFromAddress(event.fromAddress)
            .setToAddress(event.toAddress)
            .setAmount(event.amount.toString())
            .setTokenAddress(event.tokenAddress ?: "")
            .setConfirmed(event.confirmed)
            .build()
    }
}
