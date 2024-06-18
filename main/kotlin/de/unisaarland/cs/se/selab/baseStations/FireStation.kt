package de.unisaarland.cs.se.selab.baseStations

import de.unisaarland.cs.se.selab.ids.BaseStationID
import de.unisaarland.cs.se.selab.ids.VehicleID
import de.unisaarland.cs.se.selab.vehicles.FireVehicle
import de.unisaarland.cs.se.selab.vehicles.Vehicle

/**
 * Represents a fire station, a specific type of base station for firefighting operations.
 *
 * @param id The unique identifier of the fire station.
 * @param staff The number of staff members available at the fire station.
 * @param vehicles The list of vehicle IDs associated with the fire station.
 */
class FireStation(
    id: BaseStationID,
    staff: Int,
    vehicles: MutableList<VehicleID>
) : BaseStation(id, staff, vehicles) {

    override fun canStaffVehicle(vehicle: Vehicle): Boolean {
        require(vehicle is FireVehicle) { "cannot staff non-fire vehicles" }
        return super.canStaffVehicle(vehicle)
    }
}
