package de.unisaarland.cs.se.selab.events

import de.unisaarland.cs.se.selab.ids.EventID
import de.unisaarland.cs.se.selab.ids.StreetID
import de.unisaarland.cs.se.selab.mapping.Finder

/**
 * A road closure closes a given road for the duration. In case an emergency
 *  happens on a currently closed road, the road immediately opens. The remaining
 *  duration of the road closure has to be resumed once this emergency is over.
 *
 */
class RoadClosureEvent(
    id: EventID,
    duration: Int,
    startTick: Int,
    val street: StreetID,
    private var finder: Finder
) : Event(id, duration, startTick) {

    override fun apply(): Boolean {
        val actualStreet = finder.getStreetByID(street)

        // if there is an emergency on the street, don't block it
        if (actualStreet.emergencyCounter > 0) {
            return false
        }

        if (!actualStreet.isAffectedByEvent()) {
            actualStreet.blocked = true
            state = EventState.RUNNING
            actualStreet.associatedEvent = id
            return true
        }
        return false
    }

    override fun undo() {
        val actualStreet = finder.getStreetByID(street)

        if (!actualStreet.isAffectedByEvent() || actualStreet.associatedEvent == id) {
            actualStreet.blocked = false
            state = EventState.ENDED
            actualStreet.associatedEvent = null
        }
    }

    override fun suspend() {
        val actualStreet = finder.getStreetByID(street)

        if (actualStreet.isAffectedByEvent()) {
            actualStreet.blocked = false
            state = EventState.SUSPENDED
            isSuspended = true
        }
    }

    override fun resume() {
        val actualStreet = finder.getStreetByID(street)

        if (actualStreet.emergencyCounter == 0) {
            actualStreet.blocked = true
            state = EventState.RUNNING
            isSuspended = false
        }
    }
}
