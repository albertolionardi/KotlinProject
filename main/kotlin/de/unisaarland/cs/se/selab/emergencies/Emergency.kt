package de.unisaarland.cs.se.selab.emergencies

import de.unisaarland.cs.se.selab.ids.EmergencyID
import de.unisaarland.cs.se.selab.ids.StreetID
import de.unisaarland.cs.se.selab.requirements.Requirement

/**
 * *Emergency*.
 *
 * This class has no useful logic;
 *
 */
class Emergency(
    val id: EmergencyID,
    val street: StreetID,
    private val startTick: Int,
    var handleTime: Int,
    private val maxDuration: Int,
    val severity: EmergencySeverity,
    val type: EmergencyType,
    val requirement: Requirement
) : Comparable<Emergency> {
    var state = EmergencyState.UNASSIGNED

    /**
     * Function getting Tick Left
     */
    fun getTicksLeft(curTick: Int): Int {
        return maxOf(maxDuration - getRunTime(curTick), 0)
    }

    /**
     * Function getting Run Time
     */
    fun getRunTime(curTick: Int): Int {
        return curTick - startTick
    }

    /**
     * Function getting Ticks Left For Arrival
     */
    fun ticksLeftForArrival(curTick: Int): Int {
        return getTicksLeft(curTick) - handleTime
    }

    override fun compareTo(other: Emergency): Int {
        /* the comparator is written such that sorting a list of emergencies
         * based on its natural order will return a list where the
         * emergency with the highest severity and the lowest id is the smallest
         * element.
         */

        // first, compare by severity
        when {
            this.severity < other.severity -> return 1
            this.severity > other.severity -> return -1
            else -> {}
        }

        // then, compare by id
        return this.id.compareTo(other.id)
    }
}
