package de.unisaarland.cs.se.selab.vehicles

/**
 * Enum class representing different states of a vehicle.
 */
enum class VehicleState {
    AVAILABLE,
    ALLOCATED,
    DISPATCHED,
    WAITING,
    RETURNING,
    IN_PREPARATION,
    UNAVAILABLE,
}
