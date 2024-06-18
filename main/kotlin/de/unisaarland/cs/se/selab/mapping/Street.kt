package de.unisaarland.cs.se.selab.mapping

import de.unisaarland.cs.se.selab.ids.EventID
import de.unisaarland.cs.se.selab.ids.StreetID
import de.unisaarland.cs.se.selab.ids.VertexID

/**
 * class street
 * @param id the street id
 * @param source id of the source vertex
 * @param target id of the target vertex
 * @param weight weight of the street
 * @param name name of the street
 * @param height max height of vehicles that can drive on this street
 * @param pType primary type
 * @param sType secondary type
 */
class Street(
    val id: StreetID,
    val source: VertexID,
    val target: VertexID,
    var weight: Int,
    val name: String,
    var height: Int,
    val pType: StreetPrimaryType,
    sType: StreetSecondaryType,
) {

    var direction: StreetDirection = if (sType == StreetSecondaryType.ONEWAYSTREET) {
        StreetDirection.SOURCE_TARGET
    } else {
        StreetDirection.BIDIRECTIONAL
    }

    var associatedEvent: EventID? = null
    var blocked: Boolean = false

    // a street keeps track of the number of emergencies happening on it,
    // so that an event knows when to re-apply themselves
    var emergencyCounter: Int = 0

    // the street needs to always have access to its original weight and direction,
    // because when rerouting, the finder needs to know that
    val originalWeight = weight
    val originalDirection = direction

    /**
     * check if the street has an associated event
     */
    fun isAffectedByEvent(): Boolean = associatedEvent != null
}
