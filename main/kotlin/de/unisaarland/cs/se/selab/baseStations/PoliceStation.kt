package de.unisaarland.cs.se.selab.baseStations

import de.unisaarland.cs.se.selab.ids.BaseStationID
import de.unisaarland.cs.se.selab.ids.VehicleID
import de.unisaarland.cs.se.selab.vehicles.PoliceVehicle
import de.unisaarland.cs.se.selab.vehicles.Vehicle
import de.unisaarland.cs.se.selab.vehicles.VehicleType

/**
 * Represents a police station, a specific type of base station for law enforcement.
 *
 * @param id The unique identifier of the police station.
 * @param staff The number of staff members available at the police station.
 * @param vehicles The list of vehicle IDs associated with the police station.
 * @param dogs The number of available police dogs at the police station.
 */
class PoliceStation(
    id: BaseStationID,
    staff: Int,
    vehicles: MutableList<VehicleID>,
    private var dogs: Int
) : BaseStation(id, staff, vehicles) {
    /**
     * Get the number of available police dogs at the police station.
     */
    fun getAvailableDogs(): Int {
        return dogs
    }

    /**
     * getter for the number of dogs required to fulfill the car
     */
    fun returnDogToStation(dogs: Boolean) {
        if (dogs) {
            this.dogs++
        }
    }

    override fun staffVehicle(vehicle: Vehicle) {
        require(canStaffVehicle(vehicle)) { "vehicle ${vehicle.id} cannot be staffed" }

        super.staffVehicle(vehicle)
        if (vehicle.type == VehicleType.POLICE_K9_CAR) {
            (vehicle as PoliceVehicle).dogCapacity = true
            this.dogs--
        }
    }

    override fun canStaffVehicle(vehicle: Vehicle): Boolean {
        require(vehicle is PoliceVehicle) { "cannot staff non-police vehicles" }

        return when (vehicle.type) {
            VehicleType.POLICE_K9_CAR -> {
                super.canStaffVehicle(vehicle) && dogs > 0
            }
            else -> super.canStaffVehicle(vehicle)
        }
    }
}
