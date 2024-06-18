package de.unisaarland.cs.se.selab.mapping

import de.unisaarland.cs.se.selab.baseStations.BaseStation
import de.unisaarland.cs.se.selab.emergencies.Emergency
import de.unisaarland.cs.se.selab.ids.BaseStationID
import de.unisaarland.cs.se.selab.ids.StreetID
import de.unisaarland.cs.se.selab.ids.VertexID
import kotlin.reflect.KClass

/**
 * Provide the means for accessing and querying the map.
 * @param county County instance
 */
class Finder(private val county: County) {
    /**
     * pass `stationType` as (e.g.) `PoliceStation::class`
     */
    fun <T : BaseStation> getClosestStation(emergency: Emergency, stationType: KClass<T>): BaseStationID {
        val street = getStreetByID(emergency.street)

        // list of pairs for stationID and distance
        val distanceList = mutableListOf<Pair<BaseStationID, Int>>()

        // loop over all stations of the specified type
        val matchingStations = county.getStationsByType(stationType)
        for (station in matchingStations) {
            // calculate path
            val vertexID = getVertexByStationID(station.id)
            val path = getPath(vertexID, street.id)

            distanceList.add(Pair(station.id, path.remainingLength))
        }

        // get the station with the lowest distance
        // if there's a draw, take the station with the lower id
        // second call to sortBy() takes precedence
        distanceList.sortBy { it.first }
        distanceList.sortBy { it.second }

        return distanceList.first().first
    }

    /**
     * pass `stationType` as (e.g.) `PoliceStation::class`
     */
    fun <T : BaseStation> getClosestStation(
        stationID: BaseStationID,
        stationType: KClass<T>,
        visitedStations: Set<BaseStationID>
    ): BaseStationID? {
        val vertexID = getVertexByStationID(stationID)

        val terminationChecker: (VertexID) -> Boolean = { vID ->
            val sID = county.getStationByVertexID(vID)
            // the starting station should not be returned either
            if (sID != null && !visitedStations.contains(sID) && sID != stationID) {
                val station = getStationByID(sID)
                station::class == stationType
            } else {
                false
            }
        }

        val path = dijkstra(vertexID, terminationChecker)
        return if (path != null) {
            county.getStationByVertexID(path.target)
                ?: error("${path.target} not associated with station")
        } else {
            null
        }
    }

    /**
     * Return the path from source to target.
     * Two paths will be calculated, the shorter one will be returned.
     * @param source id of the starting vertex
     * @param target street with two vertices
     * @param height height restriction, 0 to disable
     *
     * @throws IllegalStateException if no path found
     */
    fun getPath(source: VertexID, target: StreetID, height: Int = 0): Path {
        val street = getStreetByID(target)

        // check if the source vertex already equals the source or target
        // vertex of the street we're trying to reach
        when (source) {
            street.source -> {
                return Path(
                    listOf(Pair(street, StreetDirection.SOURCE_TARGET)),
                    0
                )
            }
            street.target -> {
                return Path(
                    listOf(Pair(street, StreetDirection.SOURCE_TARGET)),
                    street.weight
                )
            }
            else -> {}
        }

        val pathSource = dijkstra(source, { it == street.source }, height)
        val pathTarget = dijkstra(source, { it == street.target }, height)

        requireNotNull(pathSource) { "no path from $source to ${street.source} found" }
        requireNotNull(pathTarget) { "no path from $source to ${street.target} found" }

        return minOf(pathSource, pathTarget)
    }

    /**
     * Return the shortest path from source to target.
     *
     * If it exists, it will not be empty.
     * @param source id of the starting vertex
     * @param target id of the target vertex
     * @param height height restriction, 0 to disable
     *
     * @throws IllegalStateException if no path found
     */
    fun getPath(source: VertexID, target: VertexID, height: Int = 0): Path {
        return dijkstra(source, { it == target }, height)
            ?: error("no path from $source to $target found")
    }

    /**
     * Return the path from source to target.
     * Four paths will be calculated, the shortest one will be returned.
     * @param source position on the map as pair of streetID and offset
     * @param target street with two vertices
     * @param height height restriction, 0 to disable
     */
    fun getPath(source: MapPos, target: StreetID, height: Int = 0, ignoreFirst: Boolean = false): Path {
        val targetStreet = getStreetByID(target)

        val path1 = getPath(source, targetStreet.source, height, ignoreFirst)
        val path2 = getPath(source, targetStreet.target, height, ignoreFirst)

        return minOf(path1, path2)
    }

    /**
     * Return the path from source to target.
     * Two paths will be calculated, the shortest one will be returned.
     * @param source position on the map as pair of streetID and offset
     * @param target target vertex to reach
     * @param height height restriction, 0 to disable
     */
    fun getPath(
        source: MapPos,
        target: VertexID,
        height: Int = 0,
        ignoreFirst: Boolean = false
    ): Path {
        val offset = source.first
        val sourceStreet = getStreetByID(source.second)

        val paths = mutableListOf<Path>()

        // if ignoreFirst, temporarily reset the effects of any event on the
        // source street
        val tempDirection = sourceStreet.direction
        val tempWeight = sourceStreet.weight
        val tempBlocked = sourceStreet.blocked
        if (ignoreFirst) {
            sourceStreet.direction = sourceStreet.originalDirection
            sourceStreet.weight = sourceStreet.originalWeight
            sourceStreet.blocked = false
        }

        /* calculate path from source
         * ================================================================== */
        // source vertex can be reached
        // possible if either
        //  - offset == 0 (current position at source)
        //  - street is traversable towards the source vertex
        val sourceVertexReachable = offset == 0 || sourceStreet.direction != StreetDirection.SOURCE_TARGET

        if (sourceVertexReachable) {
            val path1 = dijkstra(sourceStreet.source, { it == target }, height, allowEmpty = offset != 0)
            requireNotNull(path1) { "no path from ${sourceStreet.source} to $target found" }

            // the path has been calculated from a vertex, but the initial
            // position was in the middle of the street
            // thus, the path might need to be manually extended
            when {
                offset != 0 && path1.streets.isNotEmpty() && path1.streets.first().first == sourceStreet -> {
                    // the first street in the path is the source street
                    paths.add(Path(path1.streets.map { (s, _, d) -> Pair(s, d) }, offset))
                }

                offset != 0 -> {
                    // the first street in the path is not the source street,
                    // which means that the remaining path will lead away
                    // from the street
                    val newStreets = listOf(
                        Pair(sourceStreet, StreetDirection.TARGET_SOURCE)
                    ) + path1.streets.map { (s, _, d) -> Pair(s, d) }
                    val newOffset = path1.offset + sourceStreet.weight - offset
                    paths.add(Path(newStreets, newOffset))
                }

                else -> paths.add(path1)
            }
        }

        /* calculate path from target
         * ================================================================== */
        // target vertex can be reached
        // possible if street is traversable towards the target vertex
        // if offset == 0, don't calculate another path starting from the
        // target, as the current position is the source vertex and the option
        // of traversing the whole street will be checked by dijkstra anyway
        val targetVertexReachable = offset > 0 &&
            sourceStreet.direction != StreetDirection.TARGET_SOURCE

        if (targetVertexReachable) {
            // calculate paths starting from target vertex
            val path3 = dijkstra(sourceStreet.target, { it == target }, height, allowEmpty = true)
            requireNotNull(path3) { "no path from ${sourceStreet.target} to $target found" }

            // the path has been calculated from a vertex, but the initial
            // position was in the middle of the street
            // thus, the path might need to be manually extended
            if (
                path3.streets.isNotEmpty() &&
                path3.streets.first().first == sourceStreet
            ) {
                // the first street in the path is the source street
                // as the street is now traversed in direction TARGET_SOURCE,
                // invert the offset
                paths.add(
                    Path(
                        path3.streets.map { (s, _, d) -> Pair(s, d) },
                        sourceStreet.weight - offset
                    )
                )
            } else {
                // the first street in the path is not the source street,
                // which means that the remaining path will lead away
                // from the street
                val newStreets = listOf(
                    Pair(sourceStreet, StreetDirection.SOURCE_TARGET)
                ) + path3.streets.map { (s, _, d) -> Pair(s, d) }
                paths.add(Path(newStreets, offset))
            }
        }

        // unset the temporal overrides to the streets
        sourceStreet.direction = tempDirection
        sourceStreet.weight = tempWeight
        sourceStreet.blocked = tempBlocked

        return paths.min()
    }

    /**
     * Version of Dijkstra's algorithm that does not need to be initialized
     * with a distance mapping of all vertices in the county but will dynamically
     * build that map during runtime.
     * @param start vertex to start searching from
     * @param terminationChecker boolean predicate the target vertex needs to fulfill
     * @param height minimum height a street needs to support in order to be traversable
     * @param allowEmpty allow returning an empty path (a path with an empty list of streets)
     */
    private fun dijkstra(
        start: VertexID,
        terminationChecker: (VertexID) -> Boolean,
        height: Int = 0,
        allowEmpty: Boolean = false
    ): Path? {
        // initialize accumulators
        val distances = mutableMapOf(start to 0)
        val visited = mutableSetOf<VertexID>()
        val visitQueue = ArrayDeque(listOf(start))
        val predecessors = mutableMapOf<VertexID, StreetID?>(start to null)

        var target: VertexID? = null

        // algorithm
        while (visitQueue.isNotEmpty()) {
            val curPos = visitQueue.removeFirst()
            val curDistance = distances[curPos]
            requireNotNull(curDistance) { "$curPos has not been indexed before" }

            visited.add(curPos)

            if (terminationChecker(curPos)) {
                target = curPos
                break
            }

            val neighbours = getNeighbours(curPos, height)

            // update visitQueue & distances
            updateAccumulators(
                neighbours,
                visited,
                visitQueue,
                curDistance,
                distances,
                predecessors
            )

            // sort queue based on distance of the vertices
            // the closest vertex that has not been visited yet will be
            // visited next and thus will be first in line
            visitQueue.sortBy { distances[it] ?: error("$it not indexed before") }
        }

        // target not found
        if (target == null) {
            return null
        }

        // if start == target, pick a 'random' street from the list of
        // connections for that vertex to construct an 'empty' path
        // that still has at least one street.
        // if returning an empty path is OK, skip this step and return the
        // (possibly empty) default path
        if (start == target && !allowEmpty) {
            val theStreetID = getVertexByID(start)
                .connections
                .first()
            val theStreet = getStreetByID(theStreetID)

            val travelDirection = when (start) {
                theStreet.source -> StreetDirection.TARGET_SOURCE
                else -> StreetDirection.SOURCE_TARGET
            }

            // the path will have both source and target set to the same
            // vertex because it is an 'empty' path after all
            // still, there will be one street to not break `getPosition`.
            return Path(
                listOf(Pair(theStreet, travelDirection)),
                theStreet.weight,
                source = start,
                target = start
            )
        } else if (start == target) {
            // although empty, it still needs a source and target set
            // to not get the constructor which expects a non-empty list
            return Path(emptyList(), 0, source = start, target = start)
        } else {
            // return the normal path
            return pathFromPredecessors(start, target, predecessors)
        }
    }

    /**
     * Return a list of all neighbouring vertices.
     * Neighbours won't be returned if the street is blocked, or it's
     * height is too low.
     * @param vID vertex from which to search for neighbours
     * @param height minimal height that streets need to support (0 to ignore)
     */
    private fun getNeighbours(
        vID: VertexID,
        height: Int
    ): List<Pair<VertexID, Street>> {
        val vertex = getVertexByID(vID)
        val neighbours = mutableListOf<Pair<VertexID, Street>>()

        for (sID in vertex.connections) {
            val street = getStreetByID(sID)
            // street.height is the maximum height allowed for vehicles
            if (street.blocked ||
                (height > 0 && street.height < height)
            ) {
                continue
            }
            if (street.source == vID &&
                street.direction != StreetDirection.TARGET_SOURCE
            ) {
                neighbours.add(Pair(street.target, street))
            } else if (street.target == vID &&
                street.direction != StreetDirection.SOURCE_TARGET
            ) {
                neighbours.add(Pair(street.source, street))
            }
        }
        return neighbours
    }

    /**
     * Return a path based on a mapping of a vertex to the street that was
     * used to reach it.
     * @param start starting vertex of the path
     * @param target end vertex of the path
     * @param predecessors mapping
     */
    private fun pathFromPredecessors(
        start: VertexID,
        target: VertexID,
        predecessors: Map<VertexID, StreetID?>
    ): Path {
        val streets = mutableListOf<Pair<Street, StreetDirection>>()
        var curPos: VertexID = target

        /* The path is actually crawled backwards, starting from the target
         * until the start has been reached.
         * Thus, the resulting list needs to be reversed before passing it
         * to the Path.
         */
        while (curPos != start) {
            val predecessor = predecessors[curPos]
            requireNotNull(predecessor) { "$curPos has no predecessor" }

            val street: Street = getStreetByID(predecessor)
            when {
                street.source == curPos -> {
                    streets.add(Pair(street, StreetDirection.TARGET_SOURCE))
                    curPos = street.target
                }
                street.target == curPos -> {
                    streets.add(Pair(street, StreetDirection.SOURCE_TARGET))
                    curPos = street.source
                }
            }
        }

        return Path(streets.reversed(), 0)
    }

    /**
     * Update the accumulators based on the newly received information
     * about the neighbours.
     */
    private fun updateAccumulators(
        neighbours: List<Pair<VertexID, Street>>,
        visited: Set<VertexID>,
        visitQueue: ArrayDeque<VertexID>,
        curDistance: Int,
        distances: MutableMap<VertexID, Int>,
        predecessors: MutableMap<VertexID, StreetID?>
    ) {
        for ((neighbour, street) in neighbours) {
            if (visited.contains(neighbour)) continue

            if (!visitQueue.contains(neighbour)) {
                visitQueue.add(neighbour)
            }
            val newDistance = curDistance + street.weight

            if (distances.contains(neighbour)) {
                val oldDistance = distances[neighbour]
                requireNotNull(oldDistance) { "$neighbour not indexed before" }

                if (newDistance < oldDistance) {
                    distances[neighbour] = newDistance
                    predecessors[neighbour] = street.id
                }
            } else {
                distances[neighbour] = newDistance
                predecessors[neighbour] = street.id
            }
        }
    }

    /* wrapper methods for County
       ===================================================================== */
    /**
     * wrapper for [County.getVertexByID]
     */
    private fun getVertexByID(vID: VertexID): Vertex {
        return county.getVertexByID(vID)
    }

    /**
     * wrapper for [County.getStationByID]
     */
    fun getStationByID(bID: BaseStationID): BaseStation {
        return county.getStationByID(bID)
    }

    /**
     * wrapper for [County.getVertexByStationID]
     */
    fun getVertexByStationID(sID: BaseStationID): VertexID {
        return county.getVertexByStationID(sID)
    }

    /**
     * wrapper for [County.getStreetByID]
     */
    fun getStreetByID(sID: StreetID): Street {
        return county.getStreetByID(sID)
    }

    /**
     * wrapper for [County.getStreetsByType]
     */
    fun getStreetsByType(type: StreetPrimaryType): List<Street> {
        return county.getStreetsByType(type)
    }
}
