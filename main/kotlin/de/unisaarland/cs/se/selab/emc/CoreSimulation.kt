package de.unisaarland.cs.se.selab.emc

import de.unisaarland.cs.se.selab.emergencies.Emergency
import de.unisaarland.cs.se.selab.emergencies.EmergencyState
import de.unisaarland.cs.se.selab.logging.Logger
import de.unisaarland.cs.se.selab.mapping.Finder
import de.unisaarland.cs.se.selab.simulation.AllocationPhase
import de.unisaarland.cs.se.selab.simulation.EmergencyPhase
import de.unisaarland.cs.se.selab.simulation.UpdatePhase

/**
 * Class which will prepare the data for simulation and will simulate every Round
 * It owns the EMC, Finder and Logger.
 */
class CoreSimulation(
    private val emc: EMC,
    finder: Finder,
    private val logger: Logger
) {
    private var maxTicks: Int = 0
    private var curTick: Int = 0

    // instantiate phases
    private val emergencyPhase = EmergencyPhase(emc, logger, finder)
    private val allocationPhase = AllocationPhase(emc, logger, finder)
    private val updatePhase = UpdatePhase(emc, logger, finder)

    /**
     * The function which simulates rounds
     */
    fun run(maxTicks: Int = 0) {
        this.maxTicks = maxTicks

        // start simulation and log
        logger.simulationStart()

        // big simulation loop
        curTick = -1
        do {
            // increase & log tick
            ++curTick
            logger.simulationTick(curTick)

            // emergency phase
            emergencyPhase.assignEmergencies(emc.getEmergenciesForTick(curTick))

            // allocation phase
            val assetsList = allocationPhase.allocateAssets(curTick)

            // update phase
            updatePhase.update(curTick, assetsList)
        } while (hasTicksLeft())

        // log end of simulation
        logger.simulationEnd()
        // check if the emergency is ongoing or processed
        fun ongoingEmergencyCheck(it: Emergency): Boolean {
            return it.state == EmergencyState.ONGOING || it.state == EmergencyState.BEING_RESOLVED
        }
        // log
        emc.statistics.ongoingEmergencies =
            emc.ongoingEmergencies.count { ongoingEmergencyCheck(it) }
        logger.statistics(emc.statistics)
    }

    /**
     * Condition that checks whether the end of the simulation has been
     * reached.
     */
    private fun hasTicksLeft(): Boolean {
        // check if there are still emergencies available and if the maximum ticks have not been reached
        return emc.hasEmergenciesAvailable(curTick) && curTick < maxTicks - 1
    }
}
