package de.unisaarland.cs.se.selab.mapping

import de.unisaarland.cs.se.selab.ids.StreetID
import de.unisaarland.cs.se.selab.ids.VertexID

/**
 * Composite type representing the position on the map
 * as the offset from the source vertex of a certain street.
 */
typealias MapPos = Pair<Int, StreetID>

/**
 * @author Aron Hanowski
 */
class Path(
    streets: List<Pair<Street, StreetDirection>>,
    offset: Int,
    val source: VertexID,
    val target: VertexID,
) : Comparable<Path> {
    init {
        assert(!streets.any { it.second == StreetDirection.BIDIRECTIONAL })
    }

    // 'freeze' the weight so that we actually can compare the new to the old
    // path when rerouting
    val streets = streets.map { (street, dir) -> Triple(street, street.weight, dir) }

    var offset = offset
        private set

    private val totalLength = if (this.streets.isNotEmpty()) {
        this.streets.sumOf { it.second } //  sum of the frozen weights
    } else {
        0
    }

    val remainingLength: Int
        get() {
            return this.totalLength - offset
        }

    /**
     * Constructor that will infer the source and target from the list
     * of streets.
     * @throws NoSuchElementException if the list of streets is empty
     */
    constructor(
        streets: List<Pair<Street, StreetDirection>>,
        offset: Int
    ) : this(
        streets,
        offset,
        // the error "redundant lambda call" should not be auto-fixed
        {
            val (street, direction) = streets.first()
            when (direction) {
                StreetDirection.SOURCE_TARGET -> street.source
                else -> street.target
            }
        }(),
        {
            val (street, direction) = streets.last()
            when (direction) {
                StreetDirection.SOURCE_TARGET -> street.target
                else -> street.source
            }
        }()
    )

    /**
     * Returns the current position on the path.
     */
    fun getPosition(): MapPos {
        /* Iterate over all streets, adding up their weights until a point is reached
         * where adding the weight of the next street would exceed the `offset`.
         * Then, this street must be the street of the current position. */
        var curPos = 0

        // getPosition does not work on an 'empty' path
        check(streets.isNotEmpty()) { "this path is empty" }

        for ((s, weight, dir) in streets.iterator()) {
            // not >=, as the end of one street should be interpreted as the start of the next
            if (curPos + weight > offset) {
                // calculate the un-directional offset for the MapPos
                return if (dir == StreetDirection.TARGET_SOURCE) {
                    Pair(weight - (offset - curPos), s.id)
                } else {
                    Pair(offset - curPos, s.id)
                }
            }
            curPos += weight
        }

        // path has been advanced all the way to the end
        // could be checked beforehand, but we need a return statement after the for loop
        val (last, _) = streets.last()
        return Pair(last.weight, last.id)
    }

    /**
     * Advance the current position on the path by a certain amount of weight.
     * Can never exceed the end of the path.
     */
    fun advance(weight: Int) {
        offset = minOf(offset + weight, this.totalLength)
    }

    /**
     * Check if path has been fully advanced to the end.
     */
    fun hasArrived(): Boolean = this.remainingLength == 0

    /**
     * Two paths are considered to be the same route if
     * - their remaining weights match
     * - the set of edges remaining to arrival match
     */
    fun isSameRouteAs(other: Path): Boolean {
        // check remaining weights
        if (this.remainingLength != other.remainingLength) {
            return false
        }

        /* compare remaining edges
         * ================================================================== */
        // this is done by getting the street of the current position,
        // finding its position in the list of streets and slicing
        // from this index to the end
        val thisPos: MapPos = this.getPosition()
        val thisStreet = thisPos.second
        val thisStreetIndex = this.streets.indexOfFirst { it.first.id == thisStreet }
        val thisRemainingEdges = this.streets
            .subList(
                thisStreetIndex,
                this.streets.size
            ).map { it.first }
            .toSet()

        val otherPos: MapPos = other.getPosition()
        val otherStreet = otherPos.second
        val otherStreetIndex = other.streets.indexOfFirst { it.first.id == otherStreet }
        val otherRemainingEdges = other.streets
            .subList(
                otherStreetIndex,
                other.streets.size
            ).map { it.first }
            .toSet()

        return thisRemainingEdges == otherRemainingEdges
    }

    /**
     * Paths are compared in the following order:
     *  - lengths (sum of weights)
     *  - ids of vertices in order of traversal
     *  - number of streets in path
     */
    override fun compareTo(other: Path): Int {
        // if the lengths (sums of weights) differ, comparison is easy
        if (this.remainingLength < other.remainingLength) {
            return -1
        } else if (this.remainingLength > other.remainingLength) {
            return 1
        }

        // lengths are the same
        for ((pair1, pair2) in this.streets.zip(other.streets)) {
            val (street1, _, dir1) = pair1
            val (street2, _, dir2) = pair2

            /* Streets do not convey the information on which direction they
             * will be traversed according to the path, so this information
             * has to be stored separately.
             */
            val vertex1: VertexID = when (dir1) {
                StreetDirection.SOURCE_TARGET -> street1.source
                else -> street1.target
            }
            val vertex2: VertexID = when (dir2) {
                StreetDirection.SOURCE_TARGET -> street2.source
                else -> street2.target
            }

            // compare IDs
            if (vertex1 < vertex2) return -1
            if (vertex1 > vertex2) return 1
        }

        // this code will only be reached if the lists don't have the same
        // size
        return when {
            // the path with the shorter list of streets is smaller
            this.streets.size < other.streets.size -> -1
            this.streets.size > other.streets.size -> 1
            else -> 0
        }
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Path) {
            this.compareTo(other) == 0
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
