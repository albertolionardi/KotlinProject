package de.unisaarland.cs.se.selab.ids

/**
 * *VertexID*.
 */
@JvmInline
value class VertexID(private val id: String) : Comparable<VertexID> {
    override fun compareTo(other: VertexID): Int {
        val newThisId = removeLeadingZeros(this.id)
        val newOtherId = removeLeadingZeros(other.id)

        return when {
            newThisId < newOtherId -> -1
            newThisId > newOtherId -> 1
            else -> 0
        }
    }

    override fun toString(): String {
        return id
    }

    /**
     * *VertexID*.
     */
    private fun removeLeadingZeros(input: String): String {
        // Use a regular expression to match and replace
        // leading zeros with an empty string
        return input.replaceFirst("^0+(?!\\.)".toRegex(), "")
    }
}
