package de.unisaarland.cs.se.selab.requirements

import de.unisaarland.cs.se.selab.baseStations.BaseStation
import de.unisaarland.cs.se.selab.baseStations.FireStation
import de.unisaarland.cs.se.selab.baseStations.MedicalStation
import de.unisaarland.cs.se.selab.baseStations.PoliceStation
import de.unisaarland.cs.se.selab.vehicles.VehicleType

/**
 * Represents a requirement with specific criteria.
 *
 * @param police The list of required police vehicle types.
 * @param fire The list of required fire vehicle types.
 * @param medical The list of required medical vehicle types.
 * @param ladderLength The required ladder length (if applicable).
 * @param criminals The number of required criminals to handle.
 * @param patients The number of required patients to treat.
 * @param water The required amount of water (if applicable).
 */
class Requirement(
    val police: MutableList<VehicleType>,
    val fire: MutableList<VehicleType>,
    val medical: MutableList<VehicleType>,
    var ladderLength: Int,
    criminals: Int,
    patients: Int,
    water: Int
) {
    var totalCriminals = criminals
    var remainingCriminals = criminals

    var totalPatients = patients
    var remainingPatients = patients

    var totalWater = water
    var remainingWater = water

    /**
     * Check if the requirement is fulfilled based on the specified criteria.
     */
    fun isFulfilled(): Boolean {
        return !(requiresPolice() || requiresFire() || requiresMedical())
    }

    /**
     * Check if there are any open requirements for the police service.
     */
    fun requiresPolice(): Boolean {
        if (police.isEmpty() && remainingCriminals > 0) {
            error("no police vehicles required anymore but criminal capacity not satisfied")
        }
        return !(police.isEmpty() && remainingCriminals == 0)
    }

    /**
     * Check if there are any open requirements for the medical service.
     */
    fun requiresMedical(): Boolean {
        if (medical.isEmpty() && remainingPatients > 0) {
            error("no medical vehicles required anymore but patient capacity not satisfied")
        }
        return !(medical.isEmpty() && remainingPatients == 0)
    }

    /**
     * Check if there are any open requirements for the fire service.
     */
    fun requiresFire(): Boolean {
        if (fire.isEmpty() && remainingWater > 0) {
            error("no fire vehicles required anymore but water capacity not satisfied")
        }
        return !(fire.isEmpty() && remainingWater == 0)
    }

    /**
     * Add a vehicle type to the appropriate requirement list based on its type.
     *
     * @param vehicleType The type of the vehicle to be added.
     */

    fun addVehicleType(vehicleType: VehicleType) {
        when {
            vehicleType.isFireType() -> this.fire.add(vehicleType)
            vehicleType.isPoliceType() -> this.police.add(vehicleType)
            vehicleType.isMedicalType() -> this.police.add(vehicleType)
        }
    }

    /**
     * Return the relevant sublist of required vehicle types for the
     * specified station.
     */
    fun getVehicleTypesForStation(station: BaseStation): MutableList<VehicleType> {
        return when (station) {
            is FireStation -> fire
            is PoliceStation -> police
            is MedicalStation -> medical
        }
    }
}
