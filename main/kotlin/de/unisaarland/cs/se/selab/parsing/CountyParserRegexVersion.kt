package de.unisaarland.cs.se.selab.parsing

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.IOException

/**
 * Parser for county files.
 * A beautiful regex version that does not run on their servers.
 * @property vertices list of vertices
 * @property edges list of edges
 */
class CountyParserRegexVersion : Parser {
    val vertices = mutableListOf<String>()
    val edges = mutableListOf<Map<String, String>>()

    private val logger = KotlinLogging.logger {}

    /**
     * @param filepath the path to the file to parse
     * @param schemaPath the path to the schema file
     * @return true if the file was parsed correctly, false otherwise
     */
    override fun parse(filepath: String, schemaPath: String): Boolean {
        // read file
        val fileString: String = readFile(filepath) ?: return false

        // check file
        if (!parseFile(fileString)) {
            return false
        }

        // ensure file was read correctly
        return !(this.vertices.isEmpty() || this.edges.isEmpty())
    }

    private fun readFile(filePath: String): String? {
        return try {
            File(filePath).readText(Charsets.UTF_8)
        } catch (e: IOException) {
            logger.error { "Unable to open file $filePath, error: $e" }
            null
        }
    }

    /**
     * Check if the given file contents are valid.
     * @param content file content to check
     * @return true if the file string is valid, false otherwise
     */
    private fun parseFile(content: String): Boolean {
        // massive regex to rule them all
        val result = regexWithWhitespace(DOT_FILE).matchEntire(content)
            ?: return false

        // validate ID after `digraph`
        val digraphID = result.groups["digraphID"]?.value
        requireNotNull(digraphID)
        if (!(isValidNumberID(digraphID) || isValidStringID(digraphID))) {
            return false
        }

        /* collect vertices
         * =================================================================  */
        val vertexList = result
            .groups["vertices"]
            ?.value
            ?.filterNot { it.isWhitespace() } // remove all whitespace
            ?.trim(';') // remove trailing ';' to avoid empty list item
            ?.split(';')
        requireNotNull(vertexList)
        if (vertexList.any { !isValidNumberID(it) }) {
            return false
        }
        this.vertices.addAll(vertexList)

        /* collect edges
         * =================================================================  */
        // split & prepare edges
        val edgeList = result
            .groups["edges"]
            ?.value
            ?.filterNot { it.isWhitespace() }
            ?.split("];")
            ?.toMutableList()
        requireNotNull(edgeList)
        edgeList.removeLast() // empty last item because of splitting
        edgeList.replaceAll { "$it];" }
        // ^ lost during splitting but required for regex

        // extract attributes
        for (edge in edgeList) {
            val attributes = regexWithWhitespace(EDGE).matchEntire(edge)
                ?.groups
                ?.drop(1) // not interested in complete match, only groups
                ?.filterNotNull() // trick kotlin in smart-casting
                ?.map { it.value }
            requireNotNull(attributes)
            require(attributes.size == EDGE_ATTRIBUTE_COUNT)

            // validate attributes & add to map
            if (!checkAttributes(attributes)) {
                return false
            }
        }

        return true
    }

    private fun checkAttributes(attributes: List<String>): Boolean {
        val source = attributes.component1()
        val target = attributes.component2()
        val village = attributes.component3()
        val name = attributes.component4()
        val heightLimit = attributes.component5()
        val weight = attributes[attributes.size - 3]
        val primaryType = attributes[attributes.size - 2]
        val secondaryType = attributes.last()

        val valid = when {
            !isValidNumberID(source) -> false
            !isValidNumberID(target) -> false
            !isValidStringID(village) -> false
            !isValidStringID(name) -> false
            !isValidNumberID(heightLimit) -> false
            !isValidNumberID(weight) -> false
            else -> true
        }
        if (!valid) {
            return false
        }

        edges.add(
            mutableMapOf(
                "source" to source,
                "target" to target,
                "village" to village,
                "name" to name,
                "heightLimit" to heightLimit,
                "weight" to weight,
                "primaryType" to primaryType,
                "secondaryType" to secondaryType,
            )
        )
        return true
    }

    private fun isValidNumberID(id: String): Boolean {
        return NUMBER_ID.toRegex().matches(id) &&
            id.toIntOrNull() != null
    }

    private fun isValidStringID(id: String): Boolean {
        return STRING_ID.toRegex().matches(id)
    }

    /**
     * For better readability, the whitespace in the regex strings in the
     * companion object is represented as simple spaces.
     * In order to use those strings as regexes, the spaces have to be
     * replaced by `\s*`, as that is what the documentation specifies.
     */
    private fun regexWithWhitespace(string: String): Regex {
        return string.replace(" ", "\\s*").toRegex()
    }

    companion object {
        private const val NUMBER_ID = "(?:0|[1-9][0-9]*)"
        private const val STRING_ID = "(?:[a-zA-Z][a-zA-Z_]*)"
        private const val REGEX_ID = "(?:$NUMBER_ID|$STRING_ID)"

        private const val PRIMARY_TYPE = "(?:mainStreet|sideStreet|countyRoad)"
        private const val SECONDARY_TYPE = "(?:oneWayStreet|tunnel|none)"

        private const val ATTRIBUTES =
            "village = ($STRING_ID) ; " +
                "name = ($STRING_ID) ; " +
                "heightLimit = ($NUMBER_ID) ; " +
                "weight = ($NUMBER_ID) ; " +
                "primaryType = ($PRIMARY_TYPE) ; " +
                "secondaryType = ($SECONDARY_TYPE) ;"

        private const val EDGE = """($REGEX_ID) -> ($REGEX_ID) \[ $ATTRIBUTES \] ;"""
        private const val EDGES = "($EDGE )+"

        private const val VERTEX = "$REGEX_ID ;"
        private const val VERTICES = "($VERTEX )+"

        private const val DOT_FILE = """digraph (?<digraphID>$REGEX_ID) \{ (?<vertices>$VERTICES) (?<edges>$EDGES) \}"""

        private const val EDGE_ATTRIBUTE_COUNT = 8
    }
}
