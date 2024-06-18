package de.unisaarland.cs.se.selab.ids

/**
 * *EmergencyID*.
 */
@JvmInline
value class EmergencyID(private val id: Int) : Comparable<EmergencyID> {
    override fun compareTo(other: EmergencyID): Int {
        return when {
            this.id < other.id -> -1
            this.id > other.id -> 1
            else -> 0
        }
    }

    override fun toString(): String {
        return id.toString()
    }
}
