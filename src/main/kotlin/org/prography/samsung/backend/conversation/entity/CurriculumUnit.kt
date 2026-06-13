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
import org.prography.samsung.backend.curriculum.entity.Curriculum

@Entity
@Table(name = "curriculum_units")
class CurriculumUnit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "unit_id", nullable = false, unique = true, length = 50)
    val unitId: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_id", nullable = false)
    val curriculum: Curriculum,

    @Column(name = "unit_json", nullable = false, columnDefinition = "TEXT")
    val unitJson: String,

    @Column(name = "system_prompt_template", nullable = false, columnDefinition = "TEXT")
    val systemPromptTemplate: String,
) : BaseEntity()
