package de.unisaarland.cs.se.selab.baseStations

import de.unisaarland.cs.se.selab.ids.BaseStationID
import de.unisaarland.cs.se.selab.ids.VehicleID
import de.unisaarland.cs.se.selab.vehicles.Vehicle

/**
 * Abstract base class representing a base station for emergency vehicles.
 *
 * @param id The unique identifier of the base station.
 * @param staff The number of staff members available at the base station.
 * @param vehicles The list of vehicle IDs associated with the base station.
 */
sealed class BaseStation(
    val id: BaseStationID,
    protected var staff: Int,
    vehicles: MutableList<VehicleID>
) {
    // vehicles should be sorted by ID
    val vehicles = vehicles
        get() {
            field.sort()
            return field
        }

    /**
     * Get the available staff count at the base station.
     */
    fun getAvailableStaff(): Int {
        return staff
    }

    /**
     * Return the staff to the station.
     */
    fun returnStaffToBase(staff: Int) {
        this.staff += staff
    }

    /**
     * Check if the base station can staff the given vehicle.
     *
     * @param vehicle The vehicle to check.
     */
    open fun canStaffVehicle(vehicle: Vehicle): Boolean {
        return staff >= vehicle.staffCapacity
    }

    /**
     * Remove the staff from the station and add it to vehicle.
     * @param vehicle the vehicle to staff
     * @throws IllegalStateException if the station does not have enough staff
     */
    open fun staffVehicle(vehicle: Vehicle) {
        if (!canStaffVehicle(vehicle)) {
            error("station ${this.id} unable to staff vehicle ${vehicle.id}")
        }

        this.staff -= vehicle.staffCapacity
    }

    /**
     * Add a vehicle ID to the base station's list of vehicles.
     *
     * @param vehicleId The ID of the vehicle to add.
     */
    fun addVehicleID(vehicleId: VehicleID) {
        vehicles.add(vehicleId)
    }
}
