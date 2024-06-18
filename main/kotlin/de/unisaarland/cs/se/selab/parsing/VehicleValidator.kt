package de.unisaarland.cs.se.selab.parsing

import org.json.JSONObject

/**
 * Validates the station data.
 * @param parser the parser to get the station data from
 * @constructor creates a new station validator
 */
class VehicleValidator(private val parser: JsonParser) : Validator, JsonValidationHelper() {
    private val vehicleIds: MutableSet<Int> = mutableSetOf()

    /**
     * Validates the vehicle data and adds it to the accumulator.
     * @param accumulator the accumulator to add the vehicle data to
     * @return the accumulator with the vehicle data added, null if the data is invalid
     */
    override fun validateData(accumulator: Accumulator): Boolean {
        this.parser.jsonObject?.getJSONArray("vehicles")?.forEach {
            val vehicle = it as JSONObject
            val vehicleId = getAssertedInt(vehicle, "id")
            val baseId = getAssertedInt(vehicle, "baseID")
            val vehicleType = vehicle.getString("vehicleType")
            val vehicleHeight = vehicle.getInt("vehicleHeight")
            val staffCapacity = vehicle.getInt("staffCapacity")
            var criminalCapacity = 0
            var waterCapacity = 0
            var ladderLength = 0
            val objPres = getObjPresence(vehicle, listOf("criminalCapacity", "waterCapacity", "ladderLength"))
            val objPresCorrect: Boolean
            when (vehicleType) {
                // police vehicles
                "POLICE_CAR" -> {
                    criminalCapacity = getAssertedInt(vehicle, "criminalCapacity")
                    objPresCorrect = objPres == listOf(true, false, false)
                }
                // fire vehicles
                "FIRE_TRUCK_WATER" -> {
                    waterCapacity = getAssertedInt(vehicle, "waterCapacity")
                    objPresCorrect = objPres == listOf(false, true, false)
                }
                // medical vehicles
                "FIRE_TRUCK_LADDER" -> {
                    ladderLength = getAssertedInt(vehicle, "ladderLength")
                    objPresCorrect = objPres == listOf(false, false, true)
                }
                // all other cars
                else -> {
                    objPresCorrect = objPres == listOf(false, false, false)
                }
            }
            // if not ints, return false & check that correct obj presence
            if (intInvalid || !objPresCorrect) {
                return false
            }
            // validate parameters
            if (!validateIdAndUpdateVehicleIds(vehicleId) ||
                !accumulator.checkBase(baseId, staffCapacity, vehicleType)
            ) {
                return false
            }

            // add station to accumulator
            accumulator.addVehicle(
                vehicleId,
                baseId,
                vehicleType,
                vehicleHeight,
                staffCapacity,
                criminalCapacity,
                waterCapacity,
                ladderLength
            )
        }
        return accumulator.everyBaseHasVehicle()
    }

    /**
     * Validates the vehicle id being unique and adds it to the set of vehicle ids.
     * @param vehicleId the vehicle id to validate
     * @return true if the vehicle id is unique, false otherwise
     */
    private fun validateIdAndUpdateVehicleIds(vehicleId: Int): Boolean {
        // check if id is in vehicle ids
        if (this.vehicleIds.contains(vehicleId)) {
            return false
        }
        // add id to vehicle ids
        this.vehicleIds.add(vehicleId)
        return true
    }
}
