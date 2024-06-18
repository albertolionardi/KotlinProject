package de.unisaarland.cs.se.selab.ids

/**
 * *RequestID*.
 */
@JvmInline
value class RequestID(private val id: Int) : Comparable<RequestID> {
    override fun compareTo(other: RequestID): Int {
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
