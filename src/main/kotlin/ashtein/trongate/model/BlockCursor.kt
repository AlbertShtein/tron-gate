package ashtein.trongate.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "block_cursor")
class BlockCursor(
    @Id
    val id: String = "scanner",

    @Column(name = "block_number", nullable = false)
    var blockNumber: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
