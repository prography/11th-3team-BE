package org.prography.samsung.backend.curriculum

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.prography.samsung.backend.support.IntegrationTestSupport
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@DisplayName("커리큘럼 API 통합 테스트")
class CurriculumApiIntegrationTest : IntegrationTestSupport() {
    @Test
    @DisplayName("활성 커리큘럼 8개를 displayOrder 순으로 반환한다")
    fun shouldReturnEightActiveCurriculumsOrderedByDisplayOrder() {
        val deviceId = newDeviceId()

        expectApiSuccess(get("/curriculum", deviceId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].displayOrder").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].code").value("INTEGERS_RATIONALS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[2].code").value("FRACTION_CALC"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[2].name").value("분수의 계산"))
    }
}
