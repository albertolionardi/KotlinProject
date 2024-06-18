package de.unisaarland.cs.se.selab.logging

import de.unisaarland.cs.se.selab.emc.Statistics
import de.unisaarland.cs.se.selab.emergencies.EmergencyState
import de.unisaarland.cs.se.selab.emergencies.Request
import de.unisaarland.cs.se.selab.ids.BaseStationID
import de.unisaarland.cs.se.selab.ids.EmergencyID
import de.unisaarland.cs.se.selab.ids.EventID
import de.unisaarland.cs.se.selab.ids.VehicleID
import de.unisaarland.cs.se.selab.ids.VertexID
import java.io.PrintWriter

/**
 * Logger class, responsible for logging everything
 */
class Logger(
    private val outputHandle: PrintWriter,
    private val separator: String = ":"
) {
    private fun log(logType: String, message: String) {
        outputHandle.println(
            when {
                logType == "" -> message
                else -> "$logType$separator $message"
            }
        )
        outputHandle.flush()
    }

    /**
     * logs the successful initialization if valid is true, otherwise the
     * unsuccessful initialization
     */
    fun initInfo(fileName: String, valid: Boolean) {
        if (valid) {
            log(
                "Initialization Info",
                "$fileName successfully parsed and validated"
            )
        } else {
            log("Initialization Info", "$fileName invalid")
        }
    }

    /**
     * logs the start of the simulation
     */
    fun simulationStart() = log("", "Simulation starts")

    /**
     * logs the end of the simulation
     */
    fun simulationEnd() = log("", "Simulation End")

    /**
     * logs the current tick
     */
    fun simulationTick(tick: Int) {
        log("Simulation Tick", "$tick")
    }

    /**
     * logs the assignment of an emergency to a baseStation
     */
    fun emergencyAssignment(e: EmergencyID, b: BaseStationID) {
        log("Emergency Assignment", "$e assigned to $b")
    }

    /**
     * logs the allocation of an asset to an emergency as well as the amount
     * of ticks it needs to arrive there
     */
    fun assetAllocation(asset: VehicleID, e: EmergencyID, tickToArrive: Int) {
        log(
            "Asset Allocation",
            "$asset allocated to $e; $tickToArrive ticks to arrive."
        )
    }

    /**
     * logs the request that was sent to a base for an emergency
     */
    fun assetRequest(request: Request) {
        val requestID = checkNotNull(request.id) { "request without an ID found" }

        log(
            "Asset Request",
            "$requestID sent to ${request.receiver} for ${request.emergency}."
        )
    }

    /**
     * logs the reallocation from an asset to a new emergency
     */
    fun assetReallocation(asset: VehicleID, e: EmergencyID) {
        log("Asset Reallocation", "$asset reallocated to $e.")
    }

    /**
     * logs that an emergency has failed
     */
    fun requestFail(request: Request) {
        log("Request Failed", "${request.emergency} failed.")
    }

    /**
     * logs that an asset arrived at a vertex
     */
    fun assetArrival(asset: VehicleID, destination: VertexID) {
        log("Asset Arrival", "$asset arrived at $destination.")
    }

    /**
     * logs depending on the state, either if an emergency failed,
     * was successful or if it is being resolved
     */
    fun emergencyStatusUpdate(eID: EmergencyID, state: EmergencyState) {
        when (state) {
            EmergencyState.SUCCESS -> {
                log("Emergency Resolved", "$eID resolved.")
            }
            EmergencyState.FAILED -> {
                log("Emergency Failed", "$eID failed.")
            }
            EmergencyState.BEING_RESOLVED -> {
                log(
                    "Emergency Handling Start",
                    "$eID handling started."
                )
            }
            else -> {
                throw IllegalArgumentException("$state doesn't need a log")
            }
        }
    }

    /**
     * logs that an event was triggered
     */
    fun eventTriggered(e: EventID) {
        log("Event Triggered", "$e triggered.")
    }

    /**
     * logs that an event has ended
     */
    fun eventEnded(e: EventID) {
        log("Event Ended", "$e ended.")
    }

    /**
     * logs that an asset has been rerouted
     */
    fun assetsRerouted(rerouted: Int) {
        log("Assets Rerouted", "$rerouted")
    }

    /**
     * logs the statistics
     */
    fun statistics(stat: Statistics) {
        logStatisticEntry("${stat.reroutedAssets} assets rerouted.")
        logStatisticEntry("${stat.receivedEmergencies} received emergencies.")
        logStatisticEntry("${stat.ongoingEmergencies} ongoing emergencies.")
        logStatisticEntry("${stat.failedEmergencies} failed emergencies.")
        logStatisticEntry("${stat.resolvedEmergencies} resolved emergencies.")
    }

    private fun logStatisticEntry(entry: String) {
        log("Simulation Statistics", entry)
    }
}
