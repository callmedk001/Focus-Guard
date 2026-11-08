package com.dk.focusguard.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dk.focusguard.domain.model.FocusSession

/**
 * Room entity representing a saved focus session.
 */
@Entity(
    tableName = "focus_sessions",
    indices = [
        Index(value = ["startTimeMillis"]),
        Index(value = ["isCompleted"])
    ]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val plannedDurationMinutes: Int,
    val actualDurationSeconds: Long,
    val startTimeMillis: Long,
    val endTimeMillis: Long?,
    val isCompleted: Boolean,
    val status: String = if (isCompleted) "COMPLETED" else "IN_PROGRESS",
    val distractionsIntercepted: Int = 0
) {
    fun toDomain(): FocusSession {
        return FocusSession(
            id = id,
            title = title,
            plannedDurationMinutes = plannedDurationMinutes,
            actualDurationSeconds = actualDurationSeconds,
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
            isCompleted = isCompleted,
            distractionsIntercepted = distractionsIntercepted
        )
    }

    companion object {
        fun fromDomain(domain: FocusSession): FocusSessionEntity {
            return FocusSessionEntity(
                id = domain.id,
                title = domain.title,
                plannedDurationMinutes = domain.plannedDurationMinutes,
                actualDurationSeconds = domain.actualDurationSeconds,
                startTimeMillis = domain.startTimeMillis,
                endTimeMillis = domain.endTimeMillis,
                isCompleted = domain.isCompleted,
                status = if (domain.isCompleted) "COMPLETED" else "IN_PROGRESS",
                distractionsIntercepted = domain.distractionsIntercepted
            )
        }
    }
}
