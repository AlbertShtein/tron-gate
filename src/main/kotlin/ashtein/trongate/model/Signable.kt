package ashtein.trongate.model

interface Signable {
    var sign: ByteArray?
    fun dataToSign(): ByteArray
}