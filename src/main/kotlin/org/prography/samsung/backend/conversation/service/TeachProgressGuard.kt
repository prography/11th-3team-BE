package org.prography.samsung.backend.conversation.service

import org.prography.samsung.backend.common.domain.AiEmotion
import org.prography.samsung.backend.conversation.dto.AiTurnResponse
import org.springframework.stereotype.Component

/**
 * Pure, deterministic guard for teach turn progress (covered, missing, focus, sessionDone, correction).
 * Speak quality is enforced via semantic retry in validator + prompt.
 * Fallback redirect only on final attempt in service if needed.
 */
@Component
class TeachProgressGuard(private val validator: AiResponseValidator) {

    fun normalize(
        userText: String,
        accumulatedCovered: List<String>,
        conceptOrder: List<String>,
        repeatedFocusCount: Int,
        raw: AiTurnResponse,
    ): AiTurnResponse {
        val isAffirm = validator.isPureAffirmation(userText)

        // Claimed this turn: affirm teaches nothing
        val claimedThisTurn = if (isAffirm) emptyList() else raw.covered

        val mergedCovered = validator.mergeCovered(accumulatedCovered, claimedThisTurn)
        val missing = validator.resolveMissing(conceptOrder, mergedCovered)
        val sessionDone = missing.isEmpty()

        // Correction stage: force 4 only on repeated, but reset if this turn taught something new
        val addedSomething = claimedThisTurn.isNotEmpty()
        val correctionStage = when {
            addedSomething -> 0 // successful explanation this turn clears the stuck state
            repeatedFocusCount >= 3 -> 4
            else -> raw.correctionStage.coerceIn(0, 4)
        }

        // Final focus uses post-merge missing
        val focusConcept = validator.resolveFocusConcept(conceptOrder, missing, raw.focusConcept)

        return raw.copy(
            speak = raw.speak, // speak from LLM/prompt or fallback later
            covered = mergedCovered,
            missing = missing,
            correctionStage = correctionStage,
            focusConcept = focusConcept,
            sessionDone = sessionDone,
            emotion = if (sessionDone) AiEmotion.HAPPY else raw.emotion,
        )
    }
}
