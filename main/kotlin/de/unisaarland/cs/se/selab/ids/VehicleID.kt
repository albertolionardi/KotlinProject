package de.unisaarland.cs.se.selab.ids

/**
 * *VehicleID*.
 */
@JvmInline
value class VehicleID(private val id: Int) : Comparable<VehicleID> {
    override fun compareTo(other: VehicleID): Int {
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
