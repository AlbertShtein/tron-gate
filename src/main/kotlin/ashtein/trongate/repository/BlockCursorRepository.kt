package ashtein.trongate.repository

import ashtein.trongate.model.BlockCursor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BlockCursorRepository : JpaRepository<BlockCursor, String>
