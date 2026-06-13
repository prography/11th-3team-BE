package org.prography.samsung.backend.conversation.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.prography.samsung.backend.common.entity.BaseEntity
import org.prography.samsung.backend.session.entity.TutoringSession

@Entity
@Table(name = "conversation_turns")
class ConversationTurn(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val session: TutoringSession,

    @Column(name = "turn_number", nullable = false)
    val turnNumber: Int,

    @Column(name = "user_text", nullable = false, columnDefinition = "TEXT")
    val userText: String,

    @Column(name = "ai_response_json", nullable = false, columnDefinition = "TEXT")
    val aiResponseJson: String,
) : BaseEntity()
