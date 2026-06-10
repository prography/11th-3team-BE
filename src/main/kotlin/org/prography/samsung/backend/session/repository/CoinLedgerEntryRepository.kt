package org.prography.samsung.backend.session.repository

import org.prography.samsung.backend.session.entity.CoinLedgerEntry
import org.springframework.data.jpa.repository.JpaRepository

interface CoinLedgerEntryRepository : JpaRepository<CoinLedgerEntry, Long> {
    fun existsBySessionId(sessionId: String): Boolean
}
