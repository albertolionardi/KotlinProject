package de.unisaarland.cs.se.selab.simulation

import de.unisaarland.cs.se.selab.baseStations.FireStation
import de.unisaarland.cs.se.selab.baseStations.MedicalStation
import de.unisaarland.cs.se.selab.baseStations.PoliceStation
import de.unisaarland.cs.se.selab.emc.EMC
import de.unisaarland.cs.se.selab.emergencies.Emergency
import de.unisaarland.cs.se.selab.emergencies.EmergencyState
import de.unisaarland.cs.se.selab.emergencies.EmergencyType
import de.unisaarland.cs.se.selab.events.RoadClosureEvent
import de.unisaarland.cs.se.selab.logging.Logger
import de.unisaarland.cs.se.selab.mapping.Finder

/**
 * class that handles assigning the emergencies
 */
class EmergencyPhase(
    override val emc: EMC,
    override val logger: Logger,
    override val finder: Finder
) : SimulationPhase() {
    /**
     * assigns each new emergency to it's closest base and suspends
     * any road blocking event on the street of the emergency
     */
    fun assignEmergencies(emergencies: List<Emergency>) {
        // sort emergencies only by id, not also severity
        val newEmergencies = emergencies.sortedBy { it.id }

        // assign them
        for (emergency in newEmergencies) {
            // get the closest station
            val closestStationId = when (emergency.type) {
                EmergencyType.FIRE,
                EmergencyType.ACCIDENT -> finder.getClosestStation(emergency, FireStation::class)
                EmergencyType.MEDICAL -> finder.getClosestStation(emergency, MedicalStation::class)
                EmergencyType.CRIME -> finder.getClosestStation(emergency, PoliceStation::class)
            }

            // assign emergency
            emc.assignEmergencyToStation(emergency.id, closestStationId)
            logger.emergencyAssignment(emergency.id, closestStationId)

            // set the state of the emergency to ongoing
            emergency.state = EmergencyState.ONGOING
            emc.ongoingEmergencies.add(emergency)
            this.finder.getStreetByID(emergency.street).emergencyCounter++

            // suspend a possible RoadClosureEvent on the street the
            // emergency is happening on
            for (event in emc.ongoingEvents) {
                if (event is RoadClosureEvent && event.street == emergency.street) {
                    event.suspend()
                    break
                }
            }
        }
    }
}
