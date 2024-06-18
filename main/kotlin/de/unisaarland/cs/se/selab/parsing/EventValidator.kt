package de.unisaarland.cs.se.selab.parsing

import org.json.JSONObject

/**
 * Validates the station data.
 * @param parser the parser to get the station data from
 * @constructor creates a new station validator
 */
class EventValidator(private val parser: JsonParser) : Validator, JsonValidationHelper() {
    private val eventIds: MutableSet<Int> = mutableSetOf()

    /**
     * Validates the event data and adds it to the accumulator.
     * @param accumulator the accumulator to add the event data to
     * @return the accumulator with the event data added, null if the data is invalid
     */
    override fun validateData(accumulator: Accumulator): Boolean {
        this.parser.jsonObject?.getJSONArray("events")?.forEach {
            val event = it as JSONObject
            val eventId: Int = getAssertedInt(event, "id")
            val eventType: String = event.getString("type")
            val tick: Int = getAssertedInt(event, "tick")
            val duration: Int = getAssertedInt(event, "duration")
            var roadTypes = emptyList<String>()
            val additionalAttributes: MutableList<Int> = mutableListOf(-1, -1, -1, -1)

            // source = additionalAttributes[0]
            // target = additionalAttributes[1]
            // factor = additionalAttributes[2]
            // oneWayStreet = additionalAttributes[3] == 1
            var vehicleId: Int? = null
            var objCorrect = false
            val objPres =
                getObjPresence(event, listOf("roadTypes", FACTOR_KEY, "oneWayStreet", "source", "target", "vehicleID"))

            when (eventType) {
                "RUSH_HOUR" -> {
                    roadTypes = event
                        .getJSONArray("roadTypes")
                        .toList()
                        .map { newIt -> newIt.toString() }
                    additionalAttributes[2] = getAssertedInt(event, FACTOR_KEY)

                    objCorrect = objPres == listOf(true, true, false, false, false, false)
                    // check for doubles in road types, if found, return null
                    if (roadTypes.size != roadTypes.toSet().size) {
                        return false
                    }
                }
                "TRAFFIC_JAM", "CONSTRUCTION_SITE", "ROAD_CLOSURE" -> {
                    objCorrect = getValuesForEventOnStreet(accumulator, event, eventType, additionalAttributes, objPres)
                }
                "VEHICLE_UNAVAILABLE" -> {
                    vehicleId = getAssertedInt(event, "vehicleID")
                    objCorrect = objPres == listOf(false, false, false, false, false, true)
                    // check that vehicle exists
                    if (!accumulator.vehicleWithIDExists(vehicleId)) {
                        return false
                    }
                }
            }
            // if not ints, return false & check that correct obj presence
            if (intInvalid || !objCorrect) {
                return false
            }

            // validate parameters
            if (!validateIdAndUpdateEventIds(eventId)) {
                return false
            }
            // add event to accumulator
            accumulator.addEvent(
                eventId,
                eventType,
                tick,
                duration,
                convertPrimaryTypes(roadTypes),
                additionalAttributes,
                vehicleId
            )
        }
        return true
    }

    private fun convertPrimaryTypes(primTypeList: List<String>): List<String> {
        val returnList = mutableListOf<String>()
        // convert primary types
        for (primType in primTypeList) {
            when (primType) {
                "SIDE_STREET" -> returnList += "sideStreet"
                "MAIN_STREET" -> returnList += "mainStreet"
                "COUNTY_ROAD" -> returnList += "countyRoad"
            }
        }
        return returnList
    }

    private fun getValuesForEventOnStreet(
        acc: Accumulator,
        event: JSONObject,
        eventType: String,
        aa: MutableList<Int>,
        objPres: List<Boolean>
    ): Boolean {
        aa[0] = getAssertedInt(event, SOURCE_KEY)
        aa[1] = getAssertedInt(event, TARGET_KEY)
        if (!acc.streetWithSourceTargetExists(aa[0], aa[1])) {
            return false
        }
        return when (eventType) {
            "TRAFFIC_JAM" -> {
                aa[2] = getAssertedInt(event, FACTOR_KEY)
                objPres == listOf(false, true, false, true, true, false)
            }

            "CONSTRUCTION_SITE" -> {
                aa[2] = getAssertedInt(event, FACTOR_KEY)
                aa[3] = if (event.getBoolean("oneWayStreet")) 1 else 0
                objPres == listOf(false, true, true, true, true, false)
            }

            else -> {
                objPres == listOf(false, false, false, true, true, false)
            }
        }
    }

    /**
     * Validates the event id being unique and adds it to the set of event ids.
     * @param eventId the event id to validate
     * @return true if the event id is unique, false otherwise
     */
    private fun validateIdAndUpdateEventIds(eventId: Int): Boolean {
        // check if id is in station ids
        if (this.eventIds.contains(eventId)) {
            return false
        }
        // add id to station ids
        this.eventIds.add(eventId)
        return true
    }

    companion object {
        const val FACTOR_KEY = "factor"
        const val SOURCE_KEY = "source"
        const val TARGET_KEY = "target"
    }
}
