package de.unisaarland.cs.se.selab.emergencies

import de.unisaarland.cs.se.selab.ids.BaseStationID
import de.unisaarland.cs.se.selab.ids.EmergencyID
import de.unisaarland.cs.se.selab.ids.RequestID

/**
 * Abstract *Request* Class.
 */
class Request(
    val sender: BaseStationID,
    val receiver: BaseStationID,
    val emergency: EmergencyID,
    val visitedStations: Set<BaseStationID> = setOf(sender, receiver)
) {
    var id: RequestID? = null
        set(value) {
            // there should be no need to re-assign a request ID
            if (field == null) {
                field = value
            } else {
                error("request already has been assigned the id $field")
            }
        }

    constructor(
        receiver: BaseStationID,
        oldRequest: Request,
    ) : this(
        oldRequest.sender,
        receiver,
        oldRequest.emergency,
        mutableSetOf(receiver) + oldRequest.visitedStations
    )
}
