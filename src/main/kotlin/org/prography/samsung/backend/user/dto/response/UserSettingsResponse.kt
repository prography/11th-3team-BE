package org.prography.samsung.backend.user.dto.response

import org.prography.samsung.backend.curriculum.dto.response.CurriculumChipResponse
import org.prography.samsung.backend.user.dto.response.UserScheduleResponse

data class UserSettingsResponse(val curriculum: CurriculumChipResponse, val schedule: UserScheduleResponse)
