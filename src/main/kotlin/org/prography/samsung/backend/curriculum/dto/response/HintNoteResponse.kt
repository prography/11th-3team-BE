package org.prography.samsung.backend.curriculum.dto.response

data class HintNoteHeaderResponse(val chapter: String, val title: String)

data class HintNoteSectionResponse(val id: String, val title: String?, val bodyHtml: String, val highlight: Boolean)

data class HintNoteResponse(val header: HintNoteHeaderResponse, val sections: List<HintNoteSectionResponse>)
