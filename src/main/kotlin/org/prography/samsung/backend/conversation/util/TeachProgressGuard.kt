package org.prography.samsung.backend.conversation.util

import org.prography.samsung.backend.common.domain.AiEmotion
import org.prography.samsung.backend.conversation.dto.AiTurnResponse
import org.springframework.stereotype.Component

/**
 * Pure, deterministic guard for teach turn progress (covered, missing, focus, sessionDone, correction).
 * Speak quality is enforced via semantic retry in validator + prompt.
 * Fallback redirect only on final attempt in service if needed.
 */
@Component
class TeachProgressGuard(
    private val validator: AiResponseValidator,
    private val teacherTurnClassifier: TeacherTurnClassifier,
) {

    fun normalize(
        userText: String,
        accumulatedCovered: List<String>,
        conceptOrder: List<String>,
        repeatedFocusCount: Int,
        raw: AiTurnResponse,
        unitJson: String = "",
    ): AiTurnResponse {
        val isAffirm = teacherTurnClassifier.isPureAffirmation(userText, unitJson)

        // Claimed this turn: delta only — affirm teaches nothing; explain uses curriculum-aware match
        val expected =
            teacherTurnClassifier.expectedDeltaCovered(userText, accumulatedCovered, conceptOrder, unitJson)
        val claimedThisTurn =
            when {
                isAffirm -> emptyList()
                expected.isNotEmpty() -> expected
                else -> raw.covered.filter { it !in accumulatedCovered }
            }

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

        // Speak quality for affirm is primarily driven by prompt + semantic retry in validator.
        // Generic fallback redirect (no curriculum-specific hints/keywords) to keep it general for multiple curricula.
        val finalSpeak = if (isAffirm) {
            val hasQ = raw.speak.contains('?') || raw.speak.contains('？')
            if (!hasQ) "선생님, 그건 정확히 어떻게 설명하시나요?" else raw.speak
        } else {
            raw.speak
        }

        return raw.copy(
            speak = finalSpeak,
            covered = claimedThisTurn,
            missing = missing,
            correctionStage = correctionStage,
            focusConcept = focusConcept,
            sessionDone = sessionDone,
            emotion = if (sessionDone) AiEmotion.HAPPY else raw.emotion,
        )
    }
}
