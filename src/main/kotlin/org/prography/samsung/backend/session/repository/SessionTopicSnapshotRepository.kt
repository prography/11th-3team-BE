package org.prography.samsung.backend.session.repository

import org.prography.samsung.backend.session.entity.SessionTopicSnapshot
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SessionTopicSnapshotRepository : JpaRepository<SessionTopicSnapshot, Long> {
    @Query(
        """
        SELECT s FROM SessionTopicSnapshot s
        WHERE s.session.id = :sessionId
        ORDER BY s.sequence ASC
        """,
    )
    fun findAllBySessionIdOrderBySequenceAsc(@Param("sessionId") sessionId: String): List<SessionTopicSnapshot>

    @EntityGraph(attributePaths = ["lessonTopic"])
    @Query(
        """
        SELECT s FROM SessionTopicSnapshot s
        WHERE s.session.id = :sessionId AND s.sequence = :sequence
        """,
    )
    fun findBySessionIdAndSequence(
        @Param("sessionId") sessionId: String,
        @Param("sequence") sequence: Int,
    ): SessionTopicSnapshot?
}
