package ashtein.trongate.service.scanner

import ashtein.trongate.model.BlockCursor
import ashtein.trongate.model.WalletEvent
import ashtein.trongate.repository.BlockCursorRepository
import ashtein.trongate.repository.WalletEventRepository
import org.bouncycastle.util.encoders.Hex
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.tron.trident.core.ApiWrapper
import org.tron.trident.proto.Chain
import org.tron.trident.proto.Contract.TransferContract
import org.tron.trident.proto.Contract.TriggerSmartContract
import org.tron.trident.proto.Response.TransactionInfo
import java.math.BigInteger
import java.time.Instant

@Service
class BlockScannerService(
    private val apiWrapper: ApiWrapper,
    private val blockCursorRepository: BlockCursorRepository,
    private val walletEventRepository: WalletEventRepository,
    private val addressCacheService: AddressCacheService,
    private val eventBroadcastService: EventBroadcastService
) {
    private val log = LoggerFactory.getLogger(BlockScannerService::class.java)

    companion object {
        const val CURSOR_ID = "scanner"
        val TRANSFER_EVENT_SIGNATURE_BYTES: ByteArray = Hex.decode(
            "ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"
        )
    }

    @Scheduled(fixedDelayString = "\${parameters.scanner.interval}")
    fun scan() {
        try {
            doScan()
        } catch (e: Exception) {
            log.error("Block scan error: {}", e.message, e)
        }
    }

    private fun doScan() {
        val cursor = blockCursorRepository.findById(CURSOR_ID).orElse(null)
        val lastScanned = cursor?.blockNumber

        val latestBlock = apiWrapper.getNowBlock2()
        val latestBlockNum = latestBlock.blockHeader.rawData.number

        val startBlock = if (lastScanned != null) lastScanned + 1 else latestBlockNum
        if (startBlock > latestBlockNum) return

        for (blockNum in startBlock..latestBlockNum) {
            processBlock(blockNum)
            saveCursor(blockNum)
        }
    }

    private fun processBlock(blockNum: Long) {
        val block = apiWrapper.getBlockByNum(blockNum)
        val blockTimestamp = block.blockHeader.rawData.timestamp

        val txInfoList = apiWrapper.getTransactionInfoByBlockNum(blockNum)
        val txInfoMap = mutableMapOf<String, TransactionInfo>()
        for (txInfo in txInfoList.transactionInfoList) {
            val txHash = Hex.toHexString(txInfo.id.toByteArray())
            txInfoMap[txHash] = txInfo
        }

        for (txExt in block.transactionsList) {
            val tx = txExt.transaction
            if (!tx.hasRawData() || tx.rawData.contractCount == 0) continue

            val txHash = Hex.toHexString(txExt.txid.toByteArray())
            val contract = tx.rawData.getContract(0)
            val txInfo = txInfoMap[txHash]

            when (contract.type) {
                Chain.Transaction.Contract.ContractType.TransferContract -> {
                    processTransferContract(contract, txHash, blockNum, blockTimestamp)
                }
                Chain.Transaction.Contract.ContractType.TriggerSmartContract -> {
                    processTriggerSmartContract(contract, txInfo, txHash, blockNum, blockTimestamp)
                }
                else -> {
                    processGenericContract(contract, txHash, blockNum, blockTimestamp)
                }
            }
        }
    }

    private fun processTransferContract(
        contract: Chain.Transaction.Contract,
        txHash: String,
        blockNum: Long,
        blockTimestamp: Long
    ) {
        val transfer = contract.parameter.unpack(TransferContract::class.java)
        val fromBytes = transfer.ownerAddress.toByteArray()
        val toBytes = transfer.toAddress.toByteArray()
        val amount = BigInteger.valueOf(transfer.amount)

        if (addressCacheService.contains(fromBytes)) {
            val fromHex = Hex.toHexString(fromBytes)
            val toHex = Hex.toHexString(toBytes)
            saveEvent(fromHex, txHash, blockNum, blockTimestamp,
                WalletEvent.EventType.TRX, WalletEvent.Direction.OUT,
                fromHex, toHex, amount, null)
        }
        if (addressCacheService.contains(toBytes)) {
            val fromHex = Hex.toHexString(fromBytes)
            val toHex = Hex.toHexString(toBytes)
            saveEvent(toHex, txHash, blockNum, blockTimestamp,
                WalletEvent.EventType.TRX, WalletEvent.Direction.IN,
                fromHex, toHex, amount, null)
        }
    }

    private fun processTriggerSmartContract(
        contract: Chain.Transaction.Contract,
        txInfo: TransactionInfo?,
        txHash: String,
        blockNum: Long,
        blockTimestamp: Long
    ) {
        val trigger = contract.parameter.unpack(TriggerSmartContract::class.java)
        val callerBytes = trigger.ownerAddress.toByteArray()

        if (txInfo != null) {
            for (eventLog in txInfo.logList) {
                if (eventLog.topicsCount < 3) continue

                val topic0 = eventLog.getTopics(0).toByteArray()
                if (!topic0.contentEquals(TRANSFER_EVENT_SIGNATURE_BYTES)) continue

                val fromRaw = eventLog.getTopics(1).toByteArray()
                val toRaw = eventLog.getTopics(2).toByteArray()

                val fromAddrBytes = byteArrayOf(0x41) + fromRaw.copyOfRange(12, 32)
                val toAddrBytes = byteArrayOf(0x41) + toRaw.copyOfRange(12, 32)

                val amountData = eventLog.data.toByteArray()
                val amount = if (amountData.isNotEmpty()) BigInteger(1, amountData) else BigInteger.ZERO

                val logAddrBytes = eventLog.address.toByteArray()
                val tokenAddr = if (logAddrBytes.size == 20) {
                    "41" + Hex.toHexString(logAddrBytes)
                } else {
                    Hex.toHexString(logAddrBytes)
                }

                if (addressCacheService.contains(fromAddrBytes)) {
                    val fromAddr = Hex.toHexString(fromAddrBytes)
                    val toAddr = Hex.toHexString(toAddrBytes)
                    saveEvent(fromAddr, txHash, blockNum, blockTimestamp,
                        WalletEvent.EventType.TRC20, WalletEvent.Direction.OUT,
                        fromAddr, toAddr, amount, tokenAddr)
                }
                if (addressCacheService.contains(toAddrBytes)) {
                    val fromAddr = Hex.toHexString(fromAddrBytes)
                    val toAddr = Hex.toHexString(toAddrBytes)
                    saveEvent(toAddr, txHash, blockNum, blockTimestamp,
                        WalletEvent.EventType.TRC20, WalletEvent.Direction.IN,
                        fromAddr, toAddr, amount, tokenAddr)
                }
            }
        }

        if (addressCacheService.contains(callerBytes)) {
            val callerHex = Hex.toHexString(callerBytes)
            val contractAddrHex = Hex.toHexString(trigger.contractAddress.toByteArray())
            if (!walletEventRepository.existsByTxHashAndWalletAddressAndDirection(
                    txHash, callerHex, WalletEvent.Direction.OUT
                )
            ) {
                saveEvent(callerHex, txHash, blockNum, blockTimestamp,
                    WalletEvent.EventType.CONTRACT, WalletEvent.Direction.OUT,
                    callerHex, contractAddrHex,
                    BigInteger.valueOf(trigger.callValue),
                    contractAddrHex)
            }
        }
    }

    private fun processGenericContract(
        contract: Chain.Transaction.Contract,
        txHash: String,
        blockNum: Long,
        blockTimestamp: Long
    ) {
        val ownerBytes = extractOwnerAddress(contract.parameter.value.toByteArray()) ?: return
        if (!addressCacheService.contains(ownerBytes)) return

        val ownerHex = Hex.toHexString(ownerBytes)
        saveEvent(ownerHex, txHash, blockNum, blockTimestamp,
            WalletEvent.EventType.CONTRACT, WalletEvent.Direction.OUT,
            ownerHex, "", BigInteger.ZERO, null)
    }

    private fun extractOwnerAddress(data: ByteArray): ByteArray? {
        if (data.size < 23) return null
        if (data[0] != 0x0a.toByte()) return null
        val length = data[1].toInt() and 0xFF
        if (length != 21 || data.size < 2 + length) return null
        return data.copyOfRange(2, 2 + length)
    }

    private fun saveEvent(
        walletAddress: String,
        txHash: String,
        blockNumber: Long,
        blockTimestamp: Long,
        eventType: WalletEvent.EventType,
        direction: WalletEvent.Direction,
        fromAddress: String,
        toAddress: String,
        amount: BigInteger,
        tokenAddress: String?
    ) {
        if (walletEventRepository.existsByTxHashAndWalletAddressAndDirection(txHash, walletAddress, direction)) {
            return
        }

        val event = WalletEvent(
            walletAddress = walletAddress,
            txHash = txHash,
            blockNumber = blockNumber,
            blockTimestamp = blockTimestamp,
            eventType = eventType,
            direction = direction,
            fromAddress = fromAddress,
            toAddress = toAddress,
            amount = amount,
            tokenAddress = tokenAddress
        )
        walletEventRepository.save(event)
        eventBroadcastService.broadcast(event)

        log.info("Event: {} {} wallet={} tx={} block={}",
            direction, eventType, walletAddress, txHash, blockNumber)
    }

    private fun saveCursor(blockNum: Long) {
        val cursor = blockCursorRepository.findById(CURSOR_ID)
            .orElse(BlockCursor(id = CURSOR_ID))
        cursor.blockNumber = blockNum
        cursor.updatedAt = Instant.now()
        blockCursorRepository.save(cursor)
    }
}
