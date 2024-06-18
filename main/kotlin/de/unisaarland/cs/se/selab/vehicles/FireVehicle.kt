package de.unisaarland.cs.se.selab.vehicles

import de.unisaarland.cs.se.selab.ids.VehicleID

/**
 * Represents a fire vehicle, a specific type of vehicle for firefighting operations.
 *
 * @param id The unique identifier of the fire vehicle.
 * @param staffCapacity The maximum number of staff members the vehicle can carry.
 * @param height The height of the fire vehicle.
 * @param totalWaterCapacity The total water capacity of the fire vehicle.
 * @param type The type of fire vehicle (e.g., TRUCK_WATER, TRUCK_TECHNICAL, etc.).
 * @param ladderLength The length of the ladder on the fire vehicle (optional).
 */
class FireVehicle(
    id: VehicleID,
    type: VehicleType,
    staffCapacity: Int,
    height: Int,
    val maxWaterCapacity: Int,
    val ladderLength: Int
) : Vehicle(id, staffCapacity, height, type) {
    var currentWaterCapacity: Int = maxWaterCapacity

    /**
     * Check if the fire vehicle needs refilling of water.
     *
     * @return `true` if the fire vehicle needs refilling; `false` otherwise.
     */
    fun needsRefilling(): Boolean {
        return currentWaterCapacity != maxWaterCapacity
    }

    /**
     * send the amount of water required to fulfill
     */
    fun requiredWaterToTotal(): Int {
        return this.maxWaterCapacity - this.currentWaterCapacity
    }

    /**
     * reset the water capacity to maximum possible
     */
    fun resetWaterCapacity() {
        this.currentWaterCapacity = this.maxWaterCapacity
    }

    companion object {
        // TODO(should not be stored in this class)
        const val WATER_PER_TICK: Int = 300
    }
}
