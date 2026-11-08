package com.dk.focusguard.domain.model

/**
 * Domain model representing fine-grained guard rules (e.g. YouTube Shorts, Instagram Reels, Adult 18+ Web).
 */
data class ProtectionRule(
    val id: String,
    val title: String,
    val description: String,
    val isEnabled: Boolean,
    val category: AppCategory
)

