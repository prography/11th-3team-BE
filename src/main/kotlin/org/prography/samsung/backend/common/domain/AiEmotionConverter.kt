package org.prography.samsung.backend.common.domain

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class AiEmotionConverter : AttributeConverter<AiEmotion, String> {
    override fun convertToDatabaseColumn(attribute: AiEmotion?): String? = attribute?.value

    override fun convertToEntityAttribute(dbData: String?): AiEmotion? = dbData?.let { AiEmotion.fromValue(it) }
}
