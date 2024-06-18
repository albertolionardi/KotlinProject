package de.unisaarland.cs.se.selab.parsing

import org.json.JSONObject

/**
 * Validates the station data.
 * @param parser the parser to get the station data from
 * @constructor creates a new station validator
 */
class EmergencyValidator(private val parser: JsonParser) : Validator, JsonValidationHelper() {
    private val emergencyIds: MutableSet<Int> = mutableSetOf()

    /**
     * Validates the emergency data and adds it to the accumulator.
     * @param accumulator the accumulator to add the emergency data to
     * @return the accumulator with the emergency data added, null if the data is invalid
     */
    override fun validateData(accumulator: Accumulator): Boolean {
        this.parser.jsonObject?.getJSONArray("emergencyCalls")?.forEach {
            val emergency = it as JSONObject
            val emergencyId = getAssertedInt(emergency, "id")
            val tick = getAssertedInt(emergency, "tick")
            val emergencyType = emergency.getString("emergencyType")
            val village = emergency.getString("village")
            val roadName = emergency.getString("roadName")
            val severity = getAssertedInt(emergency, "severity")
            val handleTime = getAssertedInt(emergency, "handleTime")
            val maxDuration = getAssertedInt(emergency, "maxDuration")
            // if not ints, return false
            if (intInvalid) {
                return false
            }
            // validate parameters
            if (!validateIdAndUpdateEmergencyIds(emergencyId) ||
                maxDuration <= handleTime ||
                !accumulator.villageWithRoadExists(village, roadName)
            ) {
                return false
            }
            // add emergency to accumulator
            accumulator.addEmergency(
                emergencyId,
                tick,
                emergencyType,
                village,
                roadName,
                severity,
                handleTime,
                maxDuration
            )
        }
        return true
    }

    /**
     * Validates the emergency id being unique and adds it to the set of emergency ids.
     * @param emergencyId the emergency id to validate
     * @return true if the emergency id is unique, false otherwise
     */
    private fun validateIdAndUpdateEmergencyIds(emergencyId: Int): Boolean {
        // check if id is in station ids
        if (this.emergencyIds.contains(emergencyId)) {
            return false
        }
        // add id to station ids
        this.emergencyIds.add(emergencyId)
        return true
    }
}
