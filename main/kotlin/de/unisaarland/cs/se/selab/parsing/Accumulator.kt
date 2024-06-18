package de.unisaarland.cs.se.selab.parsing

import de.unisaarland.cs.se.selab.baseStations.BaseStation
import de.unisaarland.cs.se.selab.baseStations.FireStation
import de.unisaarland.cs.se.selab.baseStations.MedicalStation
import de.unisaarland.cs.se.selab.baseStations.PoliceStation
import de.unisaarland.cs.se.selab.emergencies.Emergency
import de.unisaarland.cs.se.selab.emergencies.EmergencySeverity
import de.unisaarland.cs.se.selab.emergencies.EmergencyType
import de.unisaarland.cs.se.selab.events.ConstructionSiteEvent
import de.unisaarland.cs.se.selab.events.Event
import de.unisaarland.cs.se.selab.events.RoadClosureEvent
import de.unisaarland.cs.se.selab.events.RushHourEvent
import de.unisaarland.cs.se.selab.events.TrafficJamEvent
import de.unisaarland.cs.se.selab.events.VehicleUnavailableEvent
import de.unisaarland.cs.se.selab.ids.BaseStationID
import de.unisaarland.cs.se.selab.ids.EmergencyID
import de.unisaarland.cs.se.selab.ids.EventID
import de.unisaarland.cs.se.selab.ids.StreetID
import de.unisaarland.cs.se.selab.ids.VehicleID
import de.unisaarland.cs.se.selab.ids.VertexID
import de.unisaarland.cs.se.selab.mapping.County
import de.unisaarland.cs.se.selab.mapping.Finder
import de.unisaarland.cs.se.selab.mapping.Street
import de.unisaarland.cs.se.selab.mapping.StreetDirection
import de.unisaarland.cs.se.selab.mapping.StreetPrimaryType
import de.unisaarland.cs.se.selab.mapping.StreetSecondaryType
import de.unisaarland.cs.se.selab.mapping.Vertex
import de.unisaarland.cs.se.selab.requirements.AccidentRequirementFactory
import de.unisaarland.cs.se.selab.requirements.CrimeRequirementFactory
import de.unisaarland.cs.se.selab.requirements.FireRequirementFactory
import de.unisaarland.cs.se.selab.requirements.MedicalRequirementFactory
import de.unisaarland.cs.se.selab.requirements.Requirement
import de.unisaarland.cs.se.selab.requirements.RequirementFactory
import de.unisaarland.cs.se.selab.vehicles.FireVehicle
import de.unisaarland.cs.se.selab.vehicles.MedicalVehicle
import de.unisaarland.cs.se.selab.vehicles.PoliceVehicle
import de.unisaarland.cs.se.selab.vehicles.Vehicle
import de.unisaarland.cs.se.selab.vehicles.VehicleType

/**
 * The `Accumulator` class is responsible for managing various maps and data related to a simulation scenario.
 * It stores information about streets, vehicles, base stations, emergencies, events, and more.
 *
 * @property streetMap stores information about streets, with `StreetID` as the key and `Street` objects as values.
 * @property vehicleMap stores information about vehicles, with `VehicleID` as the key and `Vehicle` objects as values.
 * @property stationMap  stores information about base stations
 * @property emergencyTickMap stores emergencies indexed by tick, and a list of `Emergency` objects as values.
 * @property eventTickMap events indexed by tick,  and a list of `Event` objects as values.
 * @property vertexConnectionsMap vertex and it's connected streets
 * @property vehicleToStationMap maps vehicles to their respective base stations
 * @property roadNameToStreetIDMap maps pairs of village and road names to `StreetID`.
 * @property stationToVertexMap maps base stations to vertex IDs.
 * @property finder A reference to the `Finder` class used for searching within the simulation scenario.
 */
class Accumulator {
    // ID maps
    // val vertexMap = mutableMapOf<VertexID, Vertex>()
    private val streetMap = mutableMapOf<StreetID, Street>()
    val vehicleMap = mutableMapOf<VehicleID, Vehicle>()
    private val stationMap = mutableMapOf<BaseStationID, BaseStation>()

    val emergencyTickMap = mutableMapOf<Int, MutableList<Emergency>>()
    val eventTickMap = mutableMapOf<Int, MutableList<Event>>()

    private val vertexConnectionsMap = mutableMapOf<VertexID, MutableList<StreetID>>()
    private val villageMap = mutableMapOf<String, MutableMap<String, StreetID>>()
    val vehicleToStationMap = mutableMapOf<VehicleID, BaseStationID>()

    // pair of village name & road name
    private val roadNameToStreetIDMap = mutableMapOf<Pair<String, String>, StreetID>()
    private val stationToVertexMap = mutableMapOf<BaseStationID, VertexID>()

    // finder var
    var finder: Finder? = null

    /**
     * Adds a new vertex with the given ID.
     *
     * @param id The ID of the new vertex to be added.
     */
    fun addVertex(id: String) {
        vertexConnectionsMap[VertexID(id)] = mutableListOf()
    }

    private fun stringToPrimaryType(s: String): StreetPrimaryType {
        return when (s) {
            "mainStreet" -> StreetPrimaryType.MAINSTREET
            "sideStreet" -> StreetPrimaryType.SIDESTREET
            "countyRoad" -> StreetPrimaryType.COUNTYROAD
            else -> {
                throw IllegalArgumentException("primaryType is not valid")
            }
        }
    }

    /**
     * Adds a new street with the provided details.
     *
     * @param source The ID of the source vertex.
     * @param target The ID of the target vertex.
     * @param village The name of the village to which the street belongs.
     * @param name The name of the street.
     * @param heightLimit The height limit of the street.
     * @param weight The weight of the street.
     * @param primaryType The primary type of the street.
     * @param secondaryType The secondary type of the street.
     */
    fun addStreet(
        source: String,
        target: String,
        village: String,
        name: String,
        heightLimit: String,
        weight: String,
        primaryType: String,
        secondaryType: String
    ) {
        // Create a unique ID for the street using the source and
        // target vertices
        val sID = StreetID("$source -> $target")

        // Create VertexID instances for the source and target vertices
        val sourceID = VertexID(source)
        val targetID = VertexID(target)

        // Map primary and secondary street types to enum values
        val streetPrimaryType = stringToPrimaryType(primaryType)
        val streetSecondaryType = when (secondaryType) {
            "oneWayStreet" -> StreetSecondaryType.ONEWAYSTREET
            "tunnel" -> StreetSecondaryType.TUNNEL
            "none" -> StreetSecondaryType.NONE
            else ->
                throw IllegalArgumentException("secondaryType is not valid")
        }

        roadNameToStreetIDMap[Pair(village, name)] = sID

        // create a new Street object with the specified properties
        val station = Street(
            sID,
            sourceID,
            targetID,
            weight.toInt(),
            name,
            heightLimit.toInt(),
            streetPrimaryType,
            streetSecondaryType
        )

        // add the new street to the street map
        this.streetMap[sID] = station

        // add the street ID to the village map under the village
        // and street name
        if (this.villageMap[village] == null) {
            this.villageMap[village] = mutableMapOf()
        }
        this.villageMap[village]?.put(name, sID)

        // update connections
        vertexConnectionsMap[sourceID]?.add(sID)
        vertexConnectionsMap[targetID]?.add(sID)
    }

    /**
     * Adds a new base station with the provided details.
     *
     * @param stationID The ID of the base station.
     * @param location The ID of the vertex where the base station is located.
     * @param staff The staff capacity of the base station.
     * @param baseType The type of the base station (e.g., POLICE_STATION, HOSPITAL).
     * @param specialStaff The number of special staff members at the base station (default is 0).
     */
    fun addStation(
        stationID: Int,
        location: Int,
        staff: Int,
        baseType: String,
        specialStaff: Int = 0
    ) {
        stationToVertexMap[BaseStationID(stationID)] =
            VertexID(location.toString())

        when (baseType) {
            "POLICE_STATION" -> {
                val station = PoliceStation(
                    BaseStationID(stationID),
                    staff,
                    mutableListOf(),
                    dogs = specialStaff
                )
                this.stationMap[BaseStationID(stationID)] = station
            }
            "HOSPITAL" -> {
                val station = MedicalStation(
                    BaseStationID(stationID),
                    staff,
                    mutableListOf(),
                    doctors = specialStaff
                )
                this.stationMap[BaseStationID(stationID)] = station
            }
            "FIRE_STATION" -> {
                val station = FireStation(
                    BaseStationID(stationID),
                    staff,
                    mutableListOf()
                )
                this.stationMap[BaseStationID(stationID)] = station
            }
            else -> throw IllegalArgumentException("baseType is not valid")
        }
    }

    /**
     * Checks if certain conditions are met for a base station with the given ID to be considered valid.
     *
     * @param baseIDInt The ID of the base station to check.
     * @param staffCapInt The required staff capacity.
     * @param vehicleTypeString The type of vehicle associated with the base station.
     * @return `true` if the base station is valid, `false` otherwise.
     */
    fun checkBase(
        baseIDInt: Int,
        staffCapInt: Int,
        vehicleTypeString: String
    ): Boolean {
        var baseOK = false
        // station exists
        val station = this.stationMap[BaseStationID(baseIDInt)] ?: return false
        // station is correct type for vehicle type
        when (vehicleTypeString) {
            POLICE_CAR,
            K9_POLICE_CAR, POLICE_MOTORCYCLE -> {
                baseOK = station is PoliceStation
            }
            FIRE_TRUCK_WATER,
            FIRE_TRUCK_TECHNICAL,
            FIRE_TRUCK_LADDER,
            FIREFIGHTER_TRANSPORTER -> {
                baseOK = station is FireStation
            }
            AMBULANCE,
            EMERGENCY_DOCTOR_CAR -> {
                baseOK = station is MedicalStation
            }
        }
        // special staff is present if needed
        when (vehicleTypeString) {
            K9_POLICE_CAR, EMERGENCY_DOCTOR_CAR -> {
                baseOK = baseOK && checkSpecialStaffPresent(baseIDInt)
            }
        }

        return baseOK && station.getAvailableStaff() >= staffCapInt
    }

    /**
     * Checks if a special staff is present at a base station.
     * @param baseIDInt the id of the base station
     * @return true if the base station has a special staff, false otherwise
     */
    private fun checkSpecialStaffPresent(baseIDInt: Int): Boolean {
        val station: BaseStation = this.stationMap[BaseStationID(baseIDInt)]
            ?: throw IllegalArgumentException("base station does not exist")

        // test for station type
        return when (station) {
            is MedicalStation -> station.getAvailableDoctors() > 0
            is PoliceStation -> station.getAvailableDogs() > 0
            else -> false
        }
    }

    /**
     * Checks if a vertex with a given id exists (and is in int number range).
     * @param location the int id of the vertex
     * @return true if the vertex exists, false otherwise
     */
    fun validateStationLocation(location: Int): Boolean {
        return vertexConnectionsMap.contains(VertexID(location.toString()))
    }

    /**
     * Adds a new vehicle with the provided details.
     *
     * @param vehicleID The ID of the vehicle.
     * @param baseID The ID of the base station to which the vehicle belongs.
     * @param vehicleType The type of the vehicle (e.g., POLICE_CAR, FIRE_TRUCK_WATER).
     * @param vehicleHeight The height of the vehicle.
     * @param staffCapacity The staff capacity of the vehicle.
     * @param criminalCapacity The criminal capacity of the vehicle (default is 0).
     * @param waterCapacity The water capacity of the vehicle (default is 0).
     * @param ladderLength The ladder length of the vehicle (default is 0).
     */

    fun addVehicle(
        vehicleID: Int,
        baseID: Int,
        vehicleType: String,
        vehicleHeight: Int,
        staffCapacity: Int,
        criminalCapacity: Int = 0,
        waterCapacity: Int = 0,
        ladderLength: Int = 0
    ) {
        val v: Vehicle

        when (vehicleType) {
            POLICE_CAR, K9_POLICE_CAR, POLICE_MOTORCYCLE -> {
                v = PoliceVehicle(
                    VehicleID(vehicleID),
                    when (vehicleType) {
                        POLICE_CAR -> VehicleType.POLICE_CAR
                        K9_POLICE_CAR -> VehicleType.POLICE_K9_CAR
                        else -> VehicleType.POLICE_MOTORCYCLE
                    },
                    staffCapacity,
                    vehicleHeight,
                    criminalCapacity,
                    vehicleType == K9_POLICE_CAR
                )
            }
            FIRE_TRUCK_WATER,
            FIRE_TRUCK_TECHNICAL,
            FIRE_TRUCK_LADDER,
            FIREFIGHTER_TRANSPORTER -> {
                v = FireVehicle(
                    VehicleID(vehicleID),
                    when (vehicleType) {
                        FIRE_TRUCK_WATER ->
                            VehicleType.FIRE_TRUCK_WATER
                        FIRE_TRUCK_TECHNICAL ->
                            VehicleType.FIRE_TRUCK_TECHNICAL
                        FIRE_TRUCK_LADDER ->
                            VehicleType.FIRE_TRUCK_LADDER
                        else ->
                            VehicleType.FIREFIGHTER_TRANSPORTER
                    },
                    staffCapacity,
                    vehicleHeight,
                    waterCapacity,
                    ladderLength,
                )
            }
            AMBULANCE, EMERGENCY_DOCTOR_CAR -> {
                v = MedicalVehicle(
                    VehicleID(vehicleID),
                    when (vehicleType) {
                        AMBULANCE -> VehicleType.AMBULANCE
                        else -> VehicleType.EMERGENCY_DOCTOR_CAR
                    },
                    staffCapacity,
                    vehicleHeight,
                )
            }
            else ->
                throw IllegalArgumentException("vehicleType is not valid")
        }

        this.vehicleMap[VehicleID(vehicleID)] = v

        // add vehicle to station
        val station = stationMap[BaseStationID(baseID)]
        requireNotNull(station) { "no station associated with id $baseID" }

        vehicleToStationMap[VehicleID(vehicleID)] = BaseStationID(baseID)

        station.vehicles.add(VehicleID(vehicleID))
    }

    /**
     * Checks if a village with a road exists.
     * @param villageIdString the id of the village
     * @param roadIdString the id of the road
     * @return true if the village with the road exists, false otherwise
     */
    fun villageWithRoadExists(
        villageIdString: String,
        roadIdString: String
    ): Boolean {
        this.villageMap[villageIdString]?.get(roadIdString) ?: return false
        return true
    }

    /**
     * Adds a new emergency with the provided details.
     *
     * @param emergencyID The ID of the emergency.
     * @param tick The tick at which the emergency occurs.
     * @param emergencyType The type of emergency (e.g., FIRE, ACCIDENT).
     * @param village The name of the village where the emergency occurs.
     * @param roadName The name of the road where the emergency occurs.
     * @param severity The severity level of the emergency.
     * @param handleTime The time required to handle the emergency.
     * @param maxDuration The maximum duration of the emergency.
     */
    fun addEmergency(
        emergencyID: Int,
        tick: Int,
        emergencyType: String,
        village: String,
        roadName: String,
        severity: Int,
        handleTime: Int,
        maxDuration: Int
    ) {
        val streetID = roadNameToStreetIDMap[Pair(village, roadName)]
            ?: throw IllegalArgumentException("road does not exist")

        val emType: EmergencyType
        val factory: RequirementFactory
        when (emergencyType) {
            "FIRE" -> {
                emType = EmergencyType.FIRE
                factory = FireRequirementFactory
            }
            "ACCIDENT" -> {
                emType = EmergencyType.ACCIDENT
                factory = AccidentRequirementFactory
            }
            "CRIME" -> {
                emType = EmergencyType.CRIME
                factory = CrimeRequirementFactory
            }
            "MEDICAL" -> {
                emType = EmergencyType.MEDICAL
                factory = MedicalRequirementFactory
            }
            else ->
                throw IllegalArgumentException("Emergency type is not valid")
        }

        val requirement: Requirement
        val emergencySeverity: EmergencySeverity
        when (severity) {
            1 -> {
                emergencySeverity = EmergencySeverity.LOW
                requirement = factory.createLowSeverityRequirement()
            }
            2 -> {
                emergencySeverity = EmergencySeverity.MEDIUM
                requirement = factory.createMediumSeverityRequirement()
            }
            3 -> {
                emergencySeverity = EmergencySeverity.HIGH
                requirement = factory.createHighSeverityRequirement()
            }
            else ->
                throw IllegalArgumentException("Emergency Severity is not valid")
        }

        val emergencyList = emergencyTickMap.getOrPut(tick) { mutableListOf() }
        emergencyList.add(
            Emergency(
                EmergencyID(emergencyID),
                streetID,
                tick,
                handleTime,
                maxDuration,
                emergencySeverity,
                emType,
                requirement
            )
        )
    }

    /**
     * Checks if a vehicle with a given id exists.
     * @param vId the id of the vehicle
     * @return true if the vehicle exists, false otherwise
     */
    fun vehicleWithIDExists(vId: Int): Boolean {
        return vehicleMap.contains(VehicleID(vId))
    }

    /**
     * Checks if a street with the given source and target vertices exists in the simulation scenario.
     *
     * @param source The ID of the source vertex.
     * @param target The ID of the target vertex.
     * @return `true` if the street exists, `false` otherwise.
     */
    fun streetWithSourceTargetExists(source: Int, target: Int): Boolean {
        val sourceStreetList =
            vertexConnectionsMap[VertexID(source.toString())] ?: return false
        val targetStreetList = vertexConnectionsMap[VertexID(target.toString())]
            ?: return false
        for (item in sourceStreetList) {
            if (targetStreetList.contains(item)) {
                return true
            }
        }
        return false
    }

    /**
     * Checks if a street with the given source and target vertices exists in the simulation scenario.
     *
     * @param source The ID of the source vertex.
     * @param target The ID of the target vertex.
     * @return `true` if the street exists, `false` otherwise.
     */
    private fun getStreetWithSourceTarget(source: Int, target: Int): StreetID {
        val streetListSource = vertexConnectionsMap[VertexID(source.toString())]
            ?: throw IllegalArgumentException("street does not exist")
        val streetListTarget = vertexConnectionsMap[VertexID(target.toString())]
            ?: throw IllegalArgumentException("street does not exist")

        for (item in streetListSource) {
            val street = streetMap[item]
            if (street != null) {
                if (street.target == VertexID(target.toString())) {
                    return item
                }
            }
        }
        for (item in streetListTarget) {
            val street = streetMap[item]
            if (street != null) {
                if (street.source == VertexID(target.toString())) {
                    return item
                }
            }
        }
        throw IllegalArgumentException("Does not exist")
    }

    /**
     * Adds a new event to the simulation scenario with the provided details.
     *
     * @param eventId The ID of the event.
     * @param eventType The type of event (e.g., RUSH_HOUR, TRAFFIC_JAM).
     * @param tick The tick at which the event occurs.
     * @param duration The duration of the event.
     * @param roadTypes The types of roads affected by the event (optional).
     * @param attr Additional attributes specific to the event.
     * @param vehicleId The ID of the vehicle associated with the event (optional).
     */
    fun addEvent(
        eventId: Int,
        eventType: String,
        tick: Int,
        duration: Int,
        roadTypes: List<String>,
        attr: List<Int>,
        vehicleId: Int?
    ) {
        val source = attr[0]
        val target = attr[1]
        val factor = attr[2]
        val isOneWayStreet = attr[3] == 1

        val event: Event = when (eventType) {
            "RUSH_HOUR" -> {
                RushHourEvent(
                    EventID(eventId),
                    duration,
                    tick,
                    roadTypes.map { stringToPrimaryType(it) },
                    factor,
                    returnFinder()
                )
            }

            "TRAFFIC_JAM" -> {
                val streetID: StreetID = getStreetWithSourceTarget(source, target)
                TrafficJamEvent(EventID(eventId), duration, tick, streetID, factor, returnFinder())
            }

            "CONSTRUCTION_SITE" -> {
                val s: StreetID = getStreetWithSourceTarget(source, target)

                // if oneway-street, determine direction
                val dir: StreetDirection = if (isOneWayStreet) {
                    val street = streetMap[s] ?: error("street for construction site event must not be null")

                    if (street.source == VertexID(source.toString())) {
                        StreetDirection.SOURCE_TARGET
                    } else {
                        StreetDirection.TARGET_SOURCE
                    }
                } else {
                    StreetDirection.BIDIRECTIONAL
                }

                ConstructionSiteEvent(EventID(eventId), duration, tick, s, factor, dir, returnFinder())
            }

            "ROAD_CLOSURE" -> {
                val streetID: StreetID = getStreetWithSourceTarget(source, target)
                RoadClosureEvent(EventID(eventId), duration, tick, streetID, returnFinder())
            }

            "VEHICLE_UNAVAILABLE" -> {
                VehicleUnavailableEvent(
                    EventID(eventId),
                    tick,
                    duration,
                    vehicleMap[VehicleID(vehicleId ?: error("vehicleId is null"))] ?: error("vehicle does not exist")
                )
            }

            else -> error("unknown event type '$eventType'")
        }

        // add event to event map
        val eventList = this.eventTickMap.getOrPut(tick) { mutableListOf() }
        eventList.add(event)
    }

    /**
     * Retrieves the `Finder` instance used for searching within the simulation scenario.
     *
     * @return The `Finder` instance.
     */
    fun returnFinder(): Finder {
        if (this.finder == null) {
            val vertexList = mutableListOf<Vertex>()
            vertexConnectionsMap.forEach {
                vertexList.add(Vertex(it.key, it.value))
            }

            this.finder = Finder(
                County(
                    vertexList,
                    streetMap.values.toList(),
                    stationMap.values.toList(),
                    stationToVertexMap
                )
            )
        }
        return this.finder ?: error("finder is null")
    }

    /**
     * Checks if every station has at least one vehicle
     * @return true if every station has at least one vehicle, false otherwise
     */
    fun everyBaseHasVehicle(): Boolean {
        for (station in stationMap.values) {
            if (station.vehicles.isEmpty()) {
                return false
            }
        }
        return true
    }

    companion object {
        const val POLICE_CAR = "POLICE_CAR"
        const val K9_POLICE_CAR = "K9_POLICE_CAR"
        const val POLICE_MOTORCYCLE = "POLICE_MOTORCYCLE"
        const val FIRE_TRUCK_WATER = "FIRE_TRUCK_WATER"
        const val FIRE_TRUCK_TECHNICAL = "FIRE_TRUCK_TECHNICAL"
        const val FIRE_TRUCK_LADDER = "FIRE_TRUCK_LADDER"
        const val FIREFIGHTER_TRANSPORTER = "FIREFIGHTER_TRANSPORTER"
        const val AMBULANCE = "AMBULANCE"
        const val EMERGENCY_DOCTOR_CAR = "EMERGENCY_DOCTOR_CAR"
    }
}
