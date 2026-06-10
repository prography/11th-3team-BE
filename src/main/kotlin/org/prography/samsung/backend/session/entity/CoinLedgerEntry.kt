package org.prography.samsung.backend.session.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.prography.samsung.backend.common.domain.CoinLedgerType
import org.prography.samsung.backend.common.entity.BaseEntity
import org.prography.samsung.backend.user.entity.User

@Entity
@Table(name = "coin_ledger_entries")
class CoinLedgerEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val session: TutoringSession,

    @Column(nullable = false)
    val amount: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val type: CoinLedgerType,
) : BaseEntity()
