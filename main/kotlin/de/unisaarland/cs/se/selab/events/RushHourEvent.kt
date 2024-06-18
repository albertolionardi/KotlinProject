package de.unisaarland.cs.se.selab.events

import de.unisaarland.cs.se.selab.ids.EventID
import de.unisaarland.cs.se.selab.ids.StreetID
import de.unisaarland.cs.se.selab.mapping.Finder
import de.unisaarland.cs.se.selab.mapping.StreetPrimaryType

/**
 * A rush hour can affect certain street types (e.g., county roads). For as long
 *  as this event is active, the weight of all roads of the specified types is changed by the
 *  factor. For example, you have a road with weight 2 and the factor is 3 then the weight
 *  of the road changes to 6 for the duration of the event and back to 2 once the event is
 *  over
 */
class RushHourEvent(
    id: EventID,
    duration: Int,
    startTick: Int,
    private val streetType: List<StreetPrimaryType>,
    private val weightFactor: Int,
    private val finder: Finder
) : Event(id, duration, startTick) {
    val appliedStreets = mutableListOf<StreetID>()

    override fun apply(): Boolean {
        var anyAffected = false
        streetType.forEach {
            val streetList = finder.getStreetsByType(it)
            for (street in streetList) {
                if (!street.isAffectedByEvent()) {
                    street.weight *= weightFactor
                    state = EventState.RUNNING
                    street.associatedEvent = id
                    appliedStreets.add(street.id)
                    anyAffected = true
                }
            }
        }
        return anyAffected
    }

    override fun undo() {
        for (street in appliedStreets) {
            val actualStreet = finder.getStreetByID(street)

            if (actualStreet.isAffectedByEvent()) {
                actualStreet.weight /= weightFactor
                state = EventState.ENDED
                actualStreet.associatedEvent = null
            }
        }

        appliedStreets.removeAll(appliedStreets)
    }

    override fun suspend() {
        throw UnsupportedOperationException("won't be implemented")
    }

    override fun resume() = Unit
}
