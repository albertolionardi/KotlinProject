package de.unisaarland.cs.se.selab.baseStations

import de.unisaarland.cs.se.selab.ids.BaseStationID
import de.unisaarland.cs.se.selab.ids.VehicleID
import de.unisaarland.cs.se.selab.vehicles.MedicalVehicle
import de.unisaarland.cs.se.selab.vehicles.Vehicle
import de.unisaarland.cs.se.selab.vehicles.VehicleType

/**
 * Represents a medical station, a specific type of base station for medical emergencies.
 *
 * @param id The unique identifier of the medical station.
 * @param staff The number of staff members available at the medical station.
 * @param vehicles The list of vehicle IDs associated with the medical station.
 * @param doctors The number of available doctors at the medical station.
 */
class MedicalStation(
    id: BaseStationID,
    staff: Int,
    vehicles: MutableList<VehicleID>,
    private var doctors: Int
) : BaseStation(id, staff, vehicles) {
    /**
     * Get the number of available doctors at the medical station.
     */
    fun getAvailableDoctors(): Int {
        return doctors
    }

    /**
     * Assign back the doctors to the station.
     */
    fun returnDoctorsToStation(doctors: Boolean) {
        if (doctors) {
            this.doctors++
        }
    }
    override fun staffVehicle(vehicle: Vehicle) {
        super.staffVehicle(vehicle)
        if (vehicle.type == VehicleType.EMERGENCY_DOCTOR_CAR) {
            this.doctors--
            (vehicle as MedicalVehicle).doctorCapacity = true
        }
    }

    override fun canStaffVehicle(vehicle: Vehicle): Boolean {
        require(vehicle is MedicalVehicle) { "cannot staff non-medical vehicles" }

        return when (vehicle.type) {
            VehicleType.EMERGENCY_DOCTOR_CAR -> {
                super.canStaffVehicle(vehicle) && doctors > 0
            }
            else -> super.canStaffVehicle(vehicle)
        }
    }
}
