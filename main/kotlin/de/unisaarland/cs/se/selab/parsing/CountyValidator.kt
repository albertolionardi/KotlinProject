package de.unisaarland.cs.se.selab.parsing

import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * A validator for the county file.
 * @param parser the county parser to read the data from
 */
class CountyValidator(private val parser: CountyParser) : Validator {
    // map of vertices to a list of vertices they are connected to
    private val vertices = mutableMapOf<String, MutableList<String>>()

    // map of vertexID to a set of villages these vertices would be associated with
    // according to the parsed file (only one village is allowed though)
    private val vertexToVillageMap = mutableMapOf<String, MutableSet<String>>()

    // map of village to a map of street name to primary type
    private val villages = mutableMapOf<String, MutableMap<String, String>>()

    private val logger = KotlinLogging.logger("CountyValidator")
    private var sideStreetPresent = false

    override fun validateData(accumulator: Accumulator): Boolean {
        // validate vertices
        if (!this.validateVertices(accumulator)) {
            logger.info { "validateVertices failed" }
            return false
        }

        // validate edges
        if (!this.validateEdges(accumulator)) {
            logger.info { "validateEdges failed" }
            return false
        }

        // do complex checks
        if (!this.sideStreetPresent ||
            !this.checkVertexConnections() ||
            !this.eachVillageHasMainStreet()
        ) {
            logger.info { "complex checks1 failed" }
            return false
        }
        if (!this.verticesUniqueToVillage() ||
            !this.noDoubleVerticesPresent() ||
            !this.villageNotCounty()
        ) {
            logger.info { "complex checks2 failed" }
            return false
        }

        return true
    }

    /**
     * Validates the vertex data and adds it to the accumulator.
     * @param accumulator the accumulator to add the vertex data to
     * @return true if the vertex data is valid, false otherwise
     */
    private fun validateVertices(accumulator: Accumulator): Boolean {
        for (vertexId in parser.vertices) {
            // check if vertex id is present already
            if (vertexId in this.vertices) {
                return false
            }

            // add vertex to internals
            this.vertices[vertexId] = mutableListOf()
            this.vertexToVillageMap[vertexId] = mutableSetOf()

            // add vertex to accumulator
            accumulator.addVertex(vertexId)
        }
        return true
    }

    /**
     * Validates the edge data and adds it to the accumulator.
     * @param accumulator the accumulator to add the edge data to
     * @return true if the edge data is valid, false otherwise
     */
    private fun validateEdges(accumulator: Accumulator): Boolean {
        for (edge in parser.edges) {
            // get edge parameters
            // at this stage, all of them can be safely assumed to be
            // present in `parser.edges`
            val source = checkNotNull(edge["source"])
            val target = checkNotNull(edge["target"])
            val village = checkNotNull(edge["village"])
            val name = checkNotNull(edge["name"])
            val heightLimit = checkNotNull(edge["heightLimit"])
            val weight = checkNotNull(edge["weight"])
            val primaryType = checkNotNull(edge["primaryType"])
            val secondaryType = checkNotNull(edge["secondaryType"])

            // check if edge is valid
            if (!this.validateStreetVerticesExistAndAreUnique(source, target) ||
                !this.checkStreetPrimTypeRelations(primaryType)
            ) {
                return false
            }

            if (!this.validateHeight(heightLimit, secondaryType) ||
                weight.toInt() < 1 ||
                this.roadExistsInVillage(name, village)
            ) {
                return false
            }

            // add edge to internals
            this.addEdgeToInternals(source, target, village, name, primaryType)

            // add edge to accumulator
            accumulator.addStreet(
                source,
                target,
                village,
                name,
                heightLimit,
                weight,
                primaryType,
                secondaryType
            )
        }
        return true
    }

    /**
     * Adds an edge to the internal data structures.
     * @param source the source vertex id
     * @param target the target vertex id
     * @param village the village the street is in
     * @param name the name of the street
     * @param primaryType the primary type of the street
     */
    private fun addEdgeToInternals(
        source: String,
        target: String,
        village: String,
        name: String,
        primaryType: String
    ) {
        // add list of connection and village to vertices
        this.vertices[source]?.add(target) ?: error("$source not in vertices")
        this.vertices[target]?.add(source) ?: error("$target not in vertices")

        // add village to verticesToVillage if village not county
        if (primaryType != COUNTY_ROAD) {
            this.vertexToVillageMap[source]?.add(village) ?: error("$source not in verticesToVillages")
            this.vertexToVillageMap[target]?.add(village) ?: error("$target not in verticesToVillages")
        }

        // add village if not present
        this.villages.putIfAbsent(village, mutableMapOf())

        // add street that maps to primary type, to village
        this.villages[village]?.set(name, primaryType) ?: error("$village not in villages")
    }

    /**
     * Checks if the vertices of a street are present in the county and unique.
     * @param source the source vertex id
     * @param target the target vertex id
     * @return true if the vertices are present and unique, false otherwise
     */
    private fun validateStreetVerticesExistAndAreUnique(
        source: String,
        target: String
    ): Boolean {
        // check if source and target vertex ids are unique
        if (source == target) {
            return false
        }

        // check if source and target vertex ids are existent
        if (!(source in this.vertices && target in this.vertices)) {
            return false
        }

        return true
    }

    /**
     * Checks if the primary type of street is valid and fulfills the relations to the county name.
     * @param primaryType the primary type of the street
     * @return true if the primary type is valid, false otherwise
     */
    private fun checkStreetPrimTypeRelations(primaryType: String): Boolean {
        return when (primaryType) {
            COUNTY_ROAD, MAIN_STREET -> true
            SIDE_STREET -> {
                this.sideStreetPresent = true
                true
            }
            else -> false
        }
    }

    /**
     * Checks if the height of a street is valid.
     * @param heightLimit the height to check
     * @param secondaryType the secondary type of the street
     * @return true if the height is valid, false otherwise
     */
    private fun validateHeight(heightLimit: String, secondaryType: String): Boolean {
        if (heightLimit.toInt() < 1) {
            return false
        }

        // if secondary type is tunnel check height limit at most 3
        return !(secondaryType == "tunnel" && heightLimit.toInt() > 3)
    }

    /**
     * Checks if a road is present in a village.
     * @param name the name of the road
     * @param village the village to check
     * @return true if the road is present in the village, false otherwise
     */
    private fun roadExistsInVillage(name: String, village: String): Boolean {
        return name in (this.villages[village] ?: return false)
    }

    /**
     * Check that every vertex has at least one connection, but no connection twice.
     * @return true if the vertices are connected correctly, false otherwise
     */
    private fun checkVertexConnections(): Boolean {
        return this.vertices.all { (_, connectionList) ->
            connectionList.isNotEmpty() &&
                connectionList.size == connectionList.toSet().size
        }
    }

    /**
     * Checks if every village has a main street.
     * @return true if every village has a main street, false otherwise
     */
    private fun eachVillageHasMainStreet(): Boolean {
        var mainStreetPresent: Boolean

        // check for all villages
        for (village in this.villages) {
            mainStreetPresent = false

            // check all streets in village for primary type main street
            for (street in village.value) {
                if (street.value == MAIN_STREET || street.value == COUNTY_ROAD) {
                    mainStreetPresent = true
                    break
                }
            }

            // if no main street is present, return false
            if (!mainStreetPresent) {
                return false
            }
        }
        return true
    }

    /**
     * Checks if the vertices are unique to a village & county.
     * @return true if the vertices are unique to a village & county, false otherwise
     */
    private fun verticesUniqueToVillage(): Boolean {
        return vertexToVillageMap.all { it.value.size <= 1 }
    }

    /**
     * Check if there are any duplicate vertices.
     * @return true if not, else false
     */
    private fun noDoubleVerticesPresent(): Boolean {
        return vertices.size == vertices.keys.toSet().size
    }

    /**
     * Check that villages only have streets and county's only have roads.
     */
    private fun villageNotCounty(): Boolean {
        var villageFlag: Boolean
        var countyFlag: Boolean

        // check that no village is also in county set
        for (village in this.villages) {
            villageFlag = false
            countyFlag = false

            // iterate over streets
            for (street in village.value) {
                // if street is county road, set county to true
                if (street.value == COUNTY_ROAD) {
                    countyFlag = true
                }
                // if street is main street or side street, set village to true
                if (street.value == MAIN_STREET || street.value == SIDE_STREET) {
                    villageFlag = true
                }
            }
            if (countyFlag && villageFlag) {
                return false
            }
        }
        return true
    }

    companion object {
        const val SIDE_STREET = "sideStreet"
        const val MAIN_STREET = "mainStreet"
        const val COUNTY_ROAD = "countyRoad"
    }
}
