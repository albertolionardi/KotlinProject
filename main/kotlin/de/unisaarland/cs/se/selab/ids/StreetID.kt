package de.unisaarland.cs.se.selab.ids

/**
 * *StreetID*.
 */
@JvmInline
value class StreetID(private val id: String) : Comparable<StreetID> {
    override fun compareTo(other: StreetID): Int {
        return when {
            this.id < other.id -> -1
            this.id > other.id -> 1
            else -> 0
        }
    }

    override fun toString(): String {
        return id
    }
}
