package de.unisaarland.cs.se.selab.vehicles

import de.unisaarland.cs.se.selab.ids.VehicleID

/**
 * Represents a police vehicle, a specific type of vehicle for law enforcement operations.
 *
 * @param id The unique identifier of the police vehicle.
 * @param staffCapacity The maximum number of staff members the vehicle can carry.
 * @param height The height of the police vehicle.
 * @param type The type of police vehicle (e.g., CAR, K9_CAR, MOTORCYCLE, etc.).
 * @param criminalCapacity The total criminal capacity of the police vehicle.
 * @param dogCapacity `true` if the vehicle can carry dogs; `false` otherwise.
 */
class PoliceVehicle(
    id: VehicleID,
    type: VehicleType,
    staffCapacity: Int,
    height: Int,
    criminalCapacity: Int,
    var dogCapacity: Boolean
) : Vehicle(id, staffCapacity, height, type) {

    val maxCriminalCapacity: Int = criminalCapacity
    private var currentCriminalCapacity: Int = 0
    val remainingCriminalCapacity: Int
        get() = maxCriminalCapacity - currentCriminalCapacity

    /**
     * Check if the police vehicle currently has a criminal in custody.
     *
     * @return `true` if the vehicle has a criminal; `false` otherwise.
     */
    fun hasCriminal(): Boolean {
        return currentCriminalCapacity != 0
    }

    /**
     * add the crimminals to currentCriminalCapacity
     */
    fun addCriminals(criminals: Int) {
        this.currentCriminalCapacity += criminals
    }

    /**
     * Reset the criminal capacity to 0 in the update phase
     */
    fun resetCriminalCapacity() {
        this.currentCriminalCapacity = 0
    }
}
