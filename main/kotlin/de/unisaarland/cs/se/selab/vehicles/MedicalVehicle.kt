package de.unisaarland.cs.se.selab.vehicles

import de.unisaarland.cs.se.selab.ids.VehicleID

/**
 * Represents a medical vehicle, a specific type of vehicle for medical operations.
 *
 * @param id The unique identifier of the medical vehicle.
 * @param staffCapacity The maximum number of staff members the vehicle can carry.
 * @param height The height of the medical vehicle.
 * @param type The type of medical vehicle (e.g., AMBULANCE, EMERGENCY_DOCTOR_CAR, etc.).
 */
class MedicalVehicle(
    id: VehicleID,
    type: VehicleType,
    staffCapacity: Int,
    height: Int,
) : Vehicle(id, staffCapacity, height, type) {

    var doctorCapacity: Boolean = type == VehicleType.EMERGENCY_DOCTOR_CAR
    private var patient: Boolean = false

    /**
     * Check if the medical vehicle currently has a patient on board.
     *
     * @return `true` if the vehicle has a patient; `false` otherwise.
     */
    fun hasPatient(): Boolean {
        return patient
    }

    /**
     * Set the vehicle to have a patient on board.
     */
    fun setPatient() {
        this.patient = true
    }

    /**
     * Unset the presence of a patient on the vehicle.
     */
    fun unsetPatient() {
        this.patient = false
    }
}
