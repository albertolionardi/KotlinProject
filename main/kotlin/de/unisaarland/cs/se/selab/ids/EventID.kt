package de.unisaarland.cs.se.selab.ids

/**
 * *EventID*.
 */
@JvmInline
value class EventID(private val id: Int) : Comparable<EventID> {
    override fun compareTo(other: EventID): Int {
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
