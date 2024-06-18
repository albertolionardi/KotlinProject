package de.unisaarland.cs.se.selab.events

import de.unisaarland.cs.se.selab.ids.EventID

/**
 * *Event*.
 *
 * abstract  class that  all events extend;
 *
 */

abstract class Event(
    val id: EventID,
    var duration: Int,
    var startTick: Int
) {

    var state: EventState = EventState.INACTIVE
        protected set
    var isSuspended: Boolean = false
        protected set

    /**
     * all event  types override this function and give their own implementation of how to apply themselves
     *
     * @return `true` if the action was successfully applied, if `false` event must be rescheduled.
     */
    abstract fun apply(): Boolean

    /**
     * all event  types override this function and give their own implementation of how to undo themselves
     */
    abstract fun undo()

    /**
     * all event  types override this function and give their own implementation of how to suspend themselves
     */
    abstract fun suspend()

    /**
     all event  types override this function and give their own implementation of how to resume themselves
     */
    abstract fun resume()

    /**
     * The method update() is shared by all events.
     */
    fun update() {
        if (state != EventState.RUNNING) {
            return
        }

        require(duration > 0) { "running event $id with zero duration" }
        duration--

        if (duration == 0) {
            undo()
        }
    }
}
