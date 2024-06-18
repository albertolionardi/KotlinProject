package de.unisaarland.cs.se.selab.emc

import de.unisaarland.cs.se.selab.emergencies.Emergency
import de.unisaarland.cs.se.selab.events.Event
import de.unisaarland.cs.se.selab.ids.BaseStationID
import de.unisaarland.cs.se.selab.ids.EmergencyID
import de.unisaarland.cs.se.selab.ids.RequestID
import de.unisaarland.cs.se.selab.ids.VehicleID
import de.unisaarland.cs.se.selab.vehicles.Vehicle

/**
 * Central data center which contains all data required for phases
 */
class EMC(
    private val emergencies: Map<Int, List<Emergency>>,
    private val events: MutableMap<Int, MutableList<Event>>,
    vehicleToStation: Map<VehicleID, BaseStationID>,
    private val vehicleMap: Map<VehicleID, Vehicle>
) {
    val ongoingEmergencies = mutableListOf<Emergency>()
    val ongoingEvents = mutableListOf<Event>()
    private var requestCount: Int = 1

    private val emergencyMap: Map<EmergencyID, Emergency>

    init {
        val tempMap = mutableMapOf<EmergencyID, Emergency>()
        emergencies.values.forEach { emList ->
            emList.forEach { em ->
                tempMap[em.id] = em
            }
        }
        emergencyMap = tempMap
    }

    private val emergencyToStationMap = mutableMapOf<EmergencyID, BaseStationID>()
    private val emergencyToVehicleMap = mutableMapOf<EmergencyID, MutableList<VehicleID>>()
    private val vehicleToEmergencyMap = mutableMapOf<VehicleID, Emergency>()
    private val vehicleToStationMap = vehicleToStation

    val statistics: Statistics = Statistics()
    val maxEmergencyTick: Int = emergencies.maxOf { it.key }

    /* getters and setters for emergencyToStationMap
     * ====================================================================== */
    /**
     * Assign an emergency to a base station.
     * @throws IllegalArgumentException if emergency already assigned
     */
    fun assignEmergencyToStation(
        emergencyID: EmergencyID,
        stationID: BaseStationID,
    ) {
        require(emergencyID !in emergencyToStationMap) {
            "emergency $emergencyID already assigned to station ${emergencyToStationMap[emergencyID]}"
        }
        emergencyToStationMap[emergencyID] = stationID
    }

    /**
     * Un-assign an emergency from whichever station it was assigned to.
     * @throws IllegalStateException if emergency not assigned
     */
    fun unAssignEmergency(emergencyID: EmergencyID) {
        emergencyToStationMap.remove(emergencyID) ?: error(
            "emergency $emergencyID not assigned to any station"
        )
    }

    /**
     * Return the assigned station for the specified emergency.
     * @throws IllegalStateException if emergency not assigned
     */
    fun getAssignedStation(emergencyID: EmergencyID): BaseStationID {
        return checkNotNull(emergencyToStationMap[emergencyID]) {
            "emergency $emergencyID not assigned to any station"
        }
    }

    /* getters and setters for emergencyToVehicleMap & vehicleToEmergencyMap
     * ====================================================================== */
    /**
     * Assign a vehicle to an emergency.
     * @throws IllegalArgumentException if vehicle already associated with emergency
     */
    fun assignVehicleToEmergency(
        vehicleID: VehicleID,
        emergency: Emergency
    ) {
        // emergencyToVehicleMap
        val vehicleList = emergencyToVehicleMap.getOrPut(emergency.id) { mutableListOf() }
        require(vehicleID !in vehicleList) {
            "vehicle $vehicleID already assigned to emergency ${emergency.id} (I)"
        }
        vehicleList.add(vehicleID)

        // vehicleToEmergencyMap
        require(vehicleID !in vehicleToEmergencyMap) {
            "vehicle $vehicleID already assigned to emergency ${vehicleToEmergencyMap[vehicleID]?.id} (II)"
        }
        vehicleToEmergencyMap[vehicleID] = emergency
    }

    /**
     * Un-assign a vehicle from an emergency.
     * @throws IllegalArgumentException if vehicle not assigned to emergency
     */
    fun unAssignVehicle(
        vehicleID: VehicleID,
        emergencyID: EmergencyID
    ) {
        // emergencyToVehicleMap
        val vehicleList = emergencyToVehicleMap.getOrDefault(emergencyID, mutableListOf())
        require(vehicleID in vehicleList) {
            "vehicle $vehicleID not assigned to emergency $emergencyID"
        }
        vehicleList.remove(vehicleID)

        // vehicleToEmergencyMap
        require(vehicleID in vehicleToEmergencyMap) {
            "vehicle $vehicleID not assigned to any emergency"
        }
        vehicleToEmergencyMap.remove(vehicleID)
    }

    /**
     * Return a copy of the list of assigned vehicles for the specified emergency.
     */
    fun getVehiclesForEmergency(emergencyID: EmergencyID): List<VehicleID> {
        val vehicleList = emergencyToVehicleMap[emergencyID].orEmpty()
        return vehicleList.toList()
    }

    /**
     * Return the emergency the specified vehicle has been assigned to.
     * @throws NoSuchElementException if vehicle unassigned
     */
    fun getEmergencyByVehicle(vehicleID: VehicleID): Emergency {
        return vehicleToEmergencyMap.getOrElse(vehicleID) {
            throw NoSuchElementException(
                "vehicle $vehicleID not allocated to any emergency"
            )
        }
    }

    /* getters for vehicleToStationMap
     * ====================================================================== */
    /**
     * Return the home base of a vehicle.
     */
    fun getStationByVehicleID(vehicleID: VehicleID): BaseStationID {
        return vehicleToStationMap.getOrElse(vehicleID) {
            throw NoSuchElementException(
                "vehicle $vehicleID not associated with any station"
            )
        }
    }

    /* miscellaneous getters
     * ====================================================================== */
    /**
     * Get the vehicle associated with the given ID.
     * @throws NoSuchElementException in case of failure
     */
    fun getVehicleByID(vehicleID: VehicleID): Vehicle {
        return vehicleMap.getOrElse(vehicleID) {
            throw NoSuchElementException(
                "id $vehicleID not associated with any vehicle"
            )
        }
    }

    /**
     * Get the emergency associated with the given ID.
     * @throws NoSuchElementException in case of failure
     */
    fun getEmergencyByID(emergencyID: EmergencyID): Emergency {
        return emergencyMap.getOrElse(emergencyID) {
            throw NoSuchElementException(
                "id $emergencyID not associated with any emergency"
            )
        }
    }

    /**
     * Return the emergencies emerging in the specified tick (empty list if none).
     */
    fun getEmergenciesForTick(tick: Int): List<Emergency> {
        val newEmergencies = emergencies[tick].orEmpty()
        this.statistics.receivedEmergencies += newEmergencies.size
        return newEmergencies
    }

    /**
     * Return the events scheduled for the specified tick (empty list if none).
     */
    fun getEventsForTick(tick: Int): List<Event> {
        return events[tick].orEmpty()
    }

    /**
     * Return a unique and consecutively numbered request id.
     */
    fun newRequestID(): RequestID {
        return RequestID(requestCount++)
    }

    /**
     * Return whether there are any ongoing or pending emergencies left
     * from the current tick onwards.
     */
    fun hasEmergenciesAvailable(curTick: Int): Boolean {
        return ongoingEmergencies.isNotEmpty() || maxEmergencyTick >= curTick
    }

    /**
     * reschedule the event if the event can't be applied at current tick
     */
    fun rescheduleEvent(event: Event) {
        // remove from old tick
        val oldEventList = this.events[event.startTick] ?: error("event not associated with its own start tick :(")
        oldEventList.remove(event)

        ++event.startTick

        // add to new tick
        val eventList = this.events.getOrPut(event.startTick) { mutableListOf() }
        eventList.add(event)
    }
}
