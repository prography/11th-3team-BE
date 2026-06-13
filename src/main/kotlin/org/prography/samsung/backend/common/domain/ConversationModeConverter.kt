package org.prography.samsung.backend.common.domain

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class ConversationModeConverter : AttributeConverter<ConversationMode, String> {
    override fun convertToDatabaseColumn(attribute: ConversationMode?): String? = attribute?.value

    override fun convertToEntityAttribute(dbData: String?): ConversationMode? =
        dbData?.let { ConversationMode.fromValue(it) }
}
