package com.dk.focusguard.services

/**
 * High-level status for the protection engine service.
 */
enum class ServiceStatus {
    STOPPED,
    STARTING,
    RUNNING,
    PAUSED,
    ERROR_PERMISSION_MISSING
}

