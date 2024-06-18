package de.unisaarland.cs.se.selab.events

import de.unisaarland.cs.se.selab.ids.EventID
import de.unisaarland.cs.se.selab.vehicles.Vehicle
import de.unisaarland.cs.se.selab.vehicles.VehicleState

/**
 * This is the notification of the emergency services that certain vehicles are not
 * available for the duration and can not be used for emergencies. If the
 * resource is currently not at its base, it becomes unavailable for the duration once it
 * returns to the base. The vehicle can finish the emergency it is currently assigned
 * to.
 */
class VehicleUnavailableEvent(
    id: EventID,
    duration: Int,
    startTick: Int,
    private var vehicle: Vehicle,

) : Event(id, startTick, duration) {

    override fun apply(): Boolean {
        return when (vehicle.state) {
            VehicleState.AVAILABLE -> {
                vehicle.state = VehicleState.UNAVAILABLE
                vehicle.scheduledForUnavailable = false
                this.state = EventState.RUNNING
                true
            }

            // already affected by another event
            VehicleState.UNAVAILABLE -> { false }

            // currently outside the station
            else -> {
                if (!vehicle.scheduledForUnavailable) {
                    vehicle.scheduledForUnavailable = true
                }
                false
            }
        }
    }

    override fun undo() {
        vehicle.state = VehicleState.AVAILABLE
        state = EventState.ENDED
    }

    override fun suspend() {
        throw UnsupportedOperationException("won't be implemented")
    }

    override fun resume() = Unit
}
