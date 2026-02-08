package ashtein.trongate.service.scanner

import ashtein.trongate.model.WalletEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CopyOnWriteArrayList

@Service
class EventBroadcastService {
    private val log = LoggerFactory.getLogger(EventBroadcastService::class.java)

    fun interface EventListener {
        fun onEvent(event: WalletEvent)
    }

    private val listeners = CopyOnWriteArrayList<EventListener>()

    fun subscribe(listener: EventListener): () -> Unit {
        listeners.add(listener)
        log.info("Event subscriber added, total: {}", listeners.size)
        return {
            listeners.remove(listener)
            log.info("Event subscriber removed, total: {}", listeners.size)
        }
    }

    fun broadcast(event: WalletEvent) {
        val toRemove = mutableListOf<EventListener>()
        for (listener in listeners) {
            try {
                listener.onEvent(event)
            } catch (e: Exception) {
                log.warn("Failed to broadcast event to subscriber: {}", e.message)
                toRemove.add(listener)
            }
        }
        if (toRemove.isNotEmpty()) {
            listeners.removeAll(toRemove.toSet())
        }
    }
}
