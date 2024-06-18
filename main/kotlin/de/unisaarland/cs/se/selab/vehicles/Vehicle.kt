package de.unisaarland.cs.se.selab.vehicles

import de.unisaarland.cs.se.selab.ids.VehicleID
import de.unisaarland.cs.se.selab.mapping.Path

/**
 * Abstract base class representing a vehicle.
 *
 * @property id The unique identifier of the vehicle.
 * @property staffCapacity The maximum number of staff members the vehicle can carry.
 * @property height The height of the vehicle.
 */
sealed class Vehicle(
    val id: VehicleID,
    var staffCapacity: Int,
    val height: Int,
    val type: VehicleType
) {
    var waitingCounter: Int = 0
    var state: VehicleState = VehicleState.AVAILABLE

    // remember that a vehicle is scheduled for a VehicleUnavailableEvent
    // so that we don't reallocate it
    var scheduledForUnavailable = false

    private var travelPath: Path? = null

    /**
     * Set a new path for the vehicle
     */
    fun setPath(newPath: Path?) {
        travelPath = newPath
    }

    /**
     * Get the vehicle's path.
     * @throws IllegalStateException if no path set
     */
    fun getPath(): Path {
        return checkNotNull(travelPath) { "path of vehicle $id is null" }
    }

    /**
     * Check if the vehicle is available.
     *
     * @return `true` if the vehicle is available; `false` otherwise.
     */
    fun isAvailable(): Boolean {
        return state == VehicleState.AVAILABLE
    }

    /**
     * Check if the vehicle is in a state where re-allocation would in theory
     * be allowed.
     */
    fun isReAllocatable(): Boolean {
        return (
            state == VehicleState.DISPATCHED ||
                state == VehicleState.RETURNING
            ) && !scheduledForUnavailable
    }
}
