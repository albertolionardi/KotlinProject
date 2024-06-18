
package de.unisaarland.cs.se.selab.events

import de.unisaarland.cs.se.selab.ids.EventID
import de.unisaarland.cs.se.selab.ids.StreetID
import de.unisaarland.cs.se.selab.mapping.Finder
import de.unisaarland.cs.se.selab.mapping.StreetDirection

/**
 * A construction site either changes a road to a one-way street (direction
 * source to target as specified in the event) and applies a factor on the weight or just
 * applies a factor on the weight. A construction site can only change a road to a
 * one-way street if the road is not already a one-way street; however, also in this case,
 * the factor is still applied.
 */
class ConstructionSiteEvent(
    id: EventID,
    duration: Int,
    startTick: Int,
    val streetID: StreetID,
    private var weightFactor: Int,
    private var streetDirection: StreetDirection,
    private var finder: Finder
) : Event(id, duration, startTick) {

    override fun apply(): Boolean {
        val street = finder.getStreetByID(streetID)

        if (!street.isAffectedByEvent()) {
            street.weight *= weightFactor
            state = EventState.RUNNING
            street.associatedEvent = id

            if (street.direction == StreetDirection.BIDIRECTIONAL) {
                street.direction = streetDirection
            }

            return true
        }
        return false
    }

    override fun undo() {
        val street = finder.getStreetByID(streetID)

        if (street.isAffectedByEvent()) {
            street.weight /= weightFactor
            street.associatedEvent = null
            this.state = EventState.ENDED

            street.direction = street.originalDirection
        }
    }

    override fun suspend() {
        throw UnsupportedOperationException("won't be implemented")
    }

    override fun resume() = Unit
}
