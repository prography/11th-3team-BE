package org.prography.samsung.backend.curriculum.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.prography.samsung.backend.common.entity.BaseEntity

@Entity
@Table(name = "curriculums")
class Curriculum(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 64)
    val code: String,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(name = "chapter_label", nullable = false, length = 50)
    val chapterLabel: String,

    @Column(name = "session_title_template", nullable = false, length = 100)
    val sessionTitleTemplate: String,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,
) : BaseEntity()
