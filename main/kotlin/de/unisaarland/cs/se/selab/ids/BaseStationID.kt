package de.unisaarland.cs.se.selab.ids

/**
 * *BaseStationID*.
 */
@JvmInline
value class BaseStationID(private val id: Int) : Comparable<BaseStationID> {
    override fun compareTo(other: BaseStationID): Int {
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
