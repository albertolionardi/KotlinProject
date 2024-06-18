package de.unisaarland.cs.se.selab.events

import de.unisaarland.cs.se.selab.ids.EventID
import de.unisaarland.cs.se.selab.ids.StreetID
import de.unisaarland.cs.se.selab.mapping.Finder

/**
 * A traffic jam is essentially the same as the rush hour event except that it only
 * 10 affects one specified road. For example, road A in village 1 gets a factor on its weight
 * 11 for the duration of the event.
 */
class TrafficJamEvent(
    id: EventID,
    duration: Int,
    startTick: Int,
    val street: StreetID,
    private var weightFactor: Int,
    private var finder: Finder
) : Event(id, duration, startTick) {

    override fun apply(): Boolean {
        val actualStreet = finder.getStreetByID(street)

        if (!actualStreet.isAffectedByEvent()) {
            actualStreet.weight *= weightFactor
            state = EventState.RUNNING
            actualStreet.associatedEvent = id
            return true
        }
        return false
    }

    override fun undo() {
        val actualStreet = finder.getStreetByID(street)

        if (actualStreet.isAffectedByEvent()) {
            actualStreet.weight /= weightFactor
            state = EventState.ENDED
            actualStreet.associatedEvent = null
        }
    }

    override fun suspend() {
        throw UnsupportedOperationException("won't be implemented")
    }

    override fun resume() = Unit
}
