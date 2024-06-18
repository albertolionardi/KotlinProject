package de.unisaarland.cs.se.selab.simulation

import de.unisaarland.cs.se.selab.baseStations.FireStation
import de.unisaarland.cs.se.selab.baseStations.MedicalStation
import de.unisaarland.cs.se.selab.baseStations.PoliceStation
import de.unisaarland.cs.se.selab.emc.EMC
import de.unisaarland.cs.se.selab.emergencies.Emergency
import de.unisaarland.cs.se.selab.emergencies.EmergencyState
import de.unisaarland.cs.se.selab.events.Event
import de.unisaarland.cs.se.selab.events.EventState
import de.unisaarland.cs.se.selab.events.VehicleUnavailableEvent
import de.unisaarland.cs.se.selab.ids.EventID
import de.unisaarland.cs.se.selab.ids.VehicleID
import de.unisaarland.cs.se.selab.logging.Logger
import de.unisaarland.cs.se.selab.mapping.Finder
import de.unisaarland.cs.se.selab.mapping.Path
import de.unisaarland.cs.se.selab.requirements.Requirement
import de.unisaarland.cs.se.selab.vehicles.FireVehicle
import de.unisaarland.cs.se.selab.vehicles.MedicalVehicle
import de.unisaarland.cs.se.selab.vehicles.PoliceVehicle
import de.unisaarland.cs.se.selab.vehicles.Vehicle
import de.unisaarland.cs.se.selab.vehicles.VehicleState
import de.unisaarland.cs.se.selab.vehicles.VehicleType
import kotlin.math.ceil

/**
 * Class which implements update phase logic
 */
class UpdatePhase(
    override val emc: EMC,
    override val logger: Logger,
    override val finder: Finder
) : SimulationPhase() {
    val observers: MutableList<VehicleID> = mutableListOf()
    private val changedEmergencies: MutableList<Emergency> = mutableListOf()

    /**
     * Update function which call specific private methods to update vehicles, emergencies, events
     * @param curTick: the tick of the current round
     * @param newObservers: A list of new dispatched vehicles
     */
    fun update(curTick: Int, newObservers: List<VehicleID>) {
        newObservers.forEach {
            if (it !in observers) {
                observers.add(it)
            }
        }
        observers.sortBy { vehicleID -> vehicleID }
        updateVehicles()
        updateEmergencies(curTick)
        updateEvents(curTick)
    }

    private fun returnStaffToStation(vehicle: Vehicle) {
        val station = this.finder.getStationByID(this.emc.getStationByVehicleID(vehicle.id))
        station.returnStaffToBase(vehicle.staffCapacity)
        when (station) {
            is PoliceStation -> {
                if ((vehicle as PoliceVehicle).type == VehicleType.POLICE_K9_CAR) {
                    station.returnDogToStation(vehicle.dogCapacity)
                }
            }

            is FireStation -> {
            }

            is MedicalStation -> {
                if ((vehicle as MedicalVehicle).type == VehicleType.EMERGENCY_DOCTOR_CAR) {
                    station.returnDoctorsToStation(vehicle.doctorCapacity)
                }
            }
        }
    }

    private fun calculateWaitingTime(vehicle: Vehicle) {
        this.returnStaffToStation(vehicle)
        when (vehicle) {
            is FireVehicle -> {
                if (vehicle.type == VehicleType.FIRE_TRUCK_WATER && vehicle.needsRefilling()) {
                    val requiredTicks: Double =
                        vehicle.requiredWaterToTotal().toDouble() / FireVehicle.WATER_PER_TICK
                    vehicle.waitingCounter =
                        ceil(requiredTicks)
                            .toInt()
                    vehicle.resetWaterCapacity()
                }
            }

            is PoliceVehicle -> {
                if (vehicle.type == VehicleType.POLICE_CAR && vehicle.hasCriminal()) {
                    vehicle.waitingCounter = 2
                    vehicle.resetCriminalCapacity()
                }
            }

            is MedicalVehicle -> {
                if (vehicle.type == VehicleType.AMBULANCE && vehicle.hasPatient()) {
                    vehicle.waitingCounter = 1
                    vehicle.unsetPatient()
                }
            }
        }
        vehicle.state =
            if (vehicle.waitingCounter > 0) VehicleState.IN_PREPARATION else VehicleState.AVAILABLE
    }

    private fun updateVehicles() {
        val iterator = observers.iterator()
        while (iterator.hasNext()) {
            val it = iterator.next()
            val vehicle: Vehicle = this.emc.getVehicleByID(it)

            when (vehicle.state) {
                VehicleState.DISPATCHED -> {
                    val path: Path = vehicle.getPath()

                    path.advance(tickToWeight(1))
                    if (path.hasArrived()) {
                        vehicle.state = VehicleState.WAITING

                        this.logger.assetArrival(vehicle.id, path.target)
                    }
                }

                VehicleState.RETURNING -> {
                    val path: Path = vehicle.getPath()

                    path.advance(tickToWeight(1))
                    if (path.hasArrived()) {
                        this.logger.assetArrival(it, path.target)
                        this.calculateWaitingTime(vehicle)
                    }
                }

                VehicleState.IN_PREPARATION -> {
                    vehicle.waitingCounter--
                    if (vehicle.waitingCounter == 0) {
                        vehicle.state = VehicleState.AVAILABLE
                        iterator.remove()
                    }
                }

                VehicleState.ALLOCATED -> {
                    vehicle.state = VehicleState.DISPATCHED
                }

                else -> {
                }
            }
        }
    }

    /**
     * function which takes the requirement and takes it from the vehicle
     */
    private fun useAssets(emergency: Emergency) {
        val requirement = emergency.requirement
        val vehicles = emc.getVehiclesForEmergency(emergency.id).sorted()
        vehicles.forEach {
            val vehicle = this.emc.getVehicleByID(it)
            when (vehicle.type) {
                VehicleType.FIRE_TRUCK_WATER -> {
                    vehicle as FireVehicle
                    if (vehicle.currentWaterCapacity < requirement.totalWater) {
                        requirement.totalWater -= vehicle.currentWaterCapacity
                        vehicle.currentWaterCapacity = 0
                    } else if (requirement.totalWater != 0) {
                        vehicle.currentWaterCapacity -= requirement.totalWater
                        requirement.totalWater = 0
                    }
                }

                VehicleType.AMBULANCE -> {
                    vehicle as MedicalVehicle
                    if (requirement.totalPatients > 0) {
                        vehicle.setPatient()
                        requirement.totalPatients--
                    }
                }

                VehicleType.POLICE_CAR -> {
                    vehicle as PoliceVehicle
                    if (vehicle.remainingCriminalCapacity < requirement.totalCriminals) {
                        requirement.totalCriminals -= vehicle.remainingCriminalCapacity
                        vehicle.addCriminals(vehicle.remainingCriminalCapacity)
                    } else if (requirement.totalCriminals != 0) {
                        vehicle.addCriminals(requirement.totalCriminals)
                        requirement.totalCriminals = 0
                    }
                }

                else -> {}
            }
        }
    }

    /**
     * update emergencies with status Ongoing
     * This function exists because of Detekt error
     */
    private fun updateOngoingEmergency(emergency: Emergency, curTick: Int): Boolean {
        val requirement: Requirement = emergency.requirement
        if (requirement.isFulfilled()) {
            val assignedVehicleList: List<VehicleID> = this.emc.getVehiclesForEmergency(emergency.id)
            var allVehicleArrived = if (assignedVehicleList.isEmpty()) false else true
            assignedVehicleList.forEach {
                val vehicle: Vehicle = this.emc.getVehicleByID(it)
                if (vehicle.state == VehicleState.DISPATCHED) {
                    allVehicleArrived = false
                }
            }
            if (allVehicleArrived) {
                emergency.state = EmergencyState.BEING_RESOLVED
                this.changedEmergencies.add(emergency)
                return true
            } else {
                if (emergency.getTicksLeft(curTick) == emergency.handleTime) {
                    return false
                }
            }
        } else if (emergency.getTicksLeft(curTick) == emergency.handleTime) return false
        return true
    }

    /**
     * update emergencies with status Being Resolved
     * This function exists because of Detekt error
     * @return true if the emergency is solved and false otherwise
     */
    private fun updateResolvingEmergency(emergency: Emergency): Boolean {
        emergency.handleTime--
        if (emergency.handleTime == 0) {
            emergency.state = EmergencyState.SUCCESS
            this.changedEmergencies.add(emergency)
            this.emc.statistics.resolvedEmergencies++
            this.useAssets(emergency)
            this.processEndedEmergency(emergency)
            return true
        }
        return false
    }

    private fun processEndedEmergency(emergency: Emergency) {
        this.finder.getStreetByID(emergency.street).emergencyCounter--
        this.emc.getVehiclesForEmergency(emergency.id).forEach {
            val vehicle = this.emc.getVehicleByID(it)
            val path: Path = vehicle.getPath()

            // get home base of vehicle
            val stationID = emc.getStationByVehicleID(vehicle.id)
            val stationVertex = finder.getVertexByStationID(stationID)

            vehicle.setPath(this.finder.getPath(path.target, stationVertex, vehicle.height))
            vehicle.state = VehicleState.RETURNING
            emc.unAssignVehicle(it, emergency.id)
        }
    }

    private fun updateEmergencies(curTick: Int) {
        val iterator = this.emc.ongoingEmergencies.iterator()
        while (iterator.hasNext()) {
            val it = iterator.next()
            if (it.state == EmergencyState.ONGOING) {
                val notFailedEmergency = this.updateOngoingEmergency(it, curTick)
                if (!notFailedEmergency || (it.getTicksLeft(curTick) == 0 && it.handleTime > 1)) {
                    it.state = EmergencyState.FAILED
                    iterator.remove()
                    this.changedEmergencies.add(it)
                    this.emc.statistics.failedEmergencies++
                    this.processEndedEmergency(it)
                }
            } else if (it.state == EmergencyState.BEING_RESOLVED) {
                val resolved = this.updateResolvingEmergency(it)
                if (resolved) iterator.remove()
            }
        }
        this.changedEmergencies.sortWith(compareBy({ it.state }, { it.id }))
        for (emergency in this.changedEmergencies) {
            this.logger.emergencyStatusUpdate(emergency.id, emergency.state)
        }
        this.changedEmergencies.clear()
    }

    /**
     * reroute the assets
     * This function exists because of Detekt error
     */
    private fun addNewEvents(eventList: List<Event>): Pair<Boolean, List<EventID>> {
        var recalculate = false
        val appliedEvents = mutableListOf<EventID>()
        for (event in eventList) {
            // try to apply the event
            if (!event.apply()) {
                emc.rescheduleEvent(event)
            }

            if (event.state == EventState.RUNNING) {
                this.emc.ongoingEvents.add(event)
                appliedEvents.add(event.id)
                this.logger.eventTriggered(event.id)

                recalculate = event !is VehicleUnavailableEvent || recalculate
            }
        }
        return Pair(recalculate, appliedEvents)
    }

    private fun rerouteAssets(appliedEvents: List<EventID>) {
        var reroutedAssets = 0
        this.observers.forEach {
            var ignoreFlag = false

            val vehicle = this.emc.getVehicleByID(it)

            if (
                vehicle.state == VehicleState.DISPATCHED ||
                vehicle.state == VehicleState.RETURNING
            ) {
                val path = vehicle.getPath()
                val curPosition = path.getPosition()
                if (this.finder.getStreetByID(curPosition.second).associatedEvent in appliedEvents) {
                    ignoreFlag = true
                }
                val target: Any
                val newPath: Path
                when (vehicle.state) {
                    VehicleState.DISPATCHED -> {
                        target = this.emc.getEmergencyByVehicle(it).street
                        newPath = this.finder.getPath(
                            curPosition,
                            target,
                            vehicle.height,
                            ignoreFlag
                        )
                    }

                    else -> {
                        target = this.finder.getVertexByStationID(this.emc.getStationByVehicleID(it))
                        newPath = this.finder.getPath(
                            curPosition,
                            target,
                            vehicle.height
                        )
                    }
                }

                if (!newPath.isSameRouteAs(path)) {
                    vehicle.setPath(newPath)
                    reroutedAssets++
                }
            }
        }
        if (reroutedAssets > 0) {
            this.logger.assetsRerouted(reroutedAssets)
            this.emc.statistics.reroutedAssets += reroutedAssets
        }
    }

    private fun updateEvents(curTick: Int) {
        this.emc.ongoingEvents.sortBy { it.id }
        val iterator = this.emc.ongoingEvents.iterator()
        var recalculateFlag = false
        while (iterator.hasNext()) {
            val it = iterator.next()
            it.update()
            when (it.state) {
                EventState.ENDED -> {
                    iterator.remove()
                    this.logger.eventEnded(it.id)
                    recalculateFlag = recalculateFlag || it !is VehicleUnavailableEvent
                }

                EventState.SUSPENDED -> {
                    it.resume()
                    if (it.state == EventState.RUNNING) {
                        recalculateFlag = recalculateFlag || it !is VehicleUnavailableEvent
                    }
                }

                else -> {}
            }
        }
        // iterate over new copy of list to avoid
        // ConcurrentModificationException
        val eventList = emc.getEventsForTick(curTick).toList().sortedBy { it.id }
        val (eventAdded, appliedEvents) = this.addNewEvents(eventList)
        recalculateFlag = eventAdded || recalculateFlag

        if (recalculateFlag) {
            this.rerouteAssets(appliedEvents)
        }
    }
}
