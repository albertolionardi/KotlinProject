package de.unisaarland.cs.se.selab.parsing

import org.json.JSONObject

/**
 * Validates the station data.
 * @param parser the parser to get the station data from
 * @constructor creates a new station validator
 */
class StationValidator(private val parser: JsonParser) : Validator, JsonValidationHelper() {
    private val stationIds: MutableSet<Int> = mutableSetOf()
    private val vertexToStation: MutableSet<Int> = mutableSetOf()
    private val stationTypes: MutableSet<String> = mutableSetOf()

    /**
     * Validates the station data and adds it to the accumulator.
     * @param accumulator the accumulator to add the station data to
     * @return the accumulator with the station data added, null if the data is invalid
     */
    override fun validateData(accumulator: Accumulator): Boolean {
        this.parser.jsonObject?.getJSONArray("bases")?.forEach {
            val station = it as JSONObject
            val stationId = getAssertedInt(station, "id")
            val location = getAssertedInt(station, "location")
            val staff = getAssertedInt(station, "staff")
            val baseType = station.getString("baseType")
            val objPres = getObjPresence(station, listOf("dogs", "doctors"))
            var objPresCorrect = false
            var specialStaff = 0
            // check for special staff with police/ medical stations
            when (baseType) {
                "POLICE_STATION" -> {
                    specialStaff = getAssertedInt(station, "dogs")
                    objPresCorrect = objPres == listOf(true, false)
                }
                "HOSPITAL" -> {
                    specialStaff = getAssertedInt(station, "doctors")
                    objPresCorrect = objPres == listOf(false, true)
                }
                "FIRE_STATION" -> {
                    objPresCorrect = objPres == listOf(false, false)
                }
            }
            // if not ints, return false & check that correct obj presence
            if (intInvalid || !objPresCorrect) {
                return false
            }
            // validate parameters
            if (!validateIdAndUpdateStationIds(stationId) ||
                !validateLocationAndUpdateVertexToStation(location) ||
                !accumulator.validateStationLocation(location)
            ) {
                return false
            }
            // add station to accumulator
            this.stationTypes.add(baseType)
            accumulator.addStation(stationId, location, staff, baseType, specialStaff)
        }
        return this.stationTypes.size == 3
    }

    /**
     * Validates the station id being unique and adds it to the set of station ids.
     * @param stationId the station id to validate
     * @return true if the station id is unique, false otherwise
     */
    private fun validateIdAndUpdateStationIds(stationId: Int): Boolean {
        // check if id is in station ids
        if (this.stationIds.contains(stationId)) {
            return false
        }
        // add id to station ids
        this.stationIds.add(stationId)
        return true
    }

    /**
     * Validates the location being unique and adds it to the set of vertex to station.
     * @param location the location to validate
     * @return true if the location is unique, false otherwise
     */
    private fun validateLocationAndUpdateVertexToStation(location: Int): Boolean {
        // check if location is in vertex to station
        if (this.vertexToStation.contains(location)) {
            return false
        }
        // add location to vertex to station
        this.vertexToStation.add(location)
        return true
    }
}
