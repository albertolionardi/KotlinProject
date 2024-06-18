package de.unisaarland.cs.se.selab.parsing

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.IOException

/**
 * Parser for county files.
 * @property vertices list of vertices
 * @property edges list of edges
 */
class CountyParser : Parser {
    val vertices: MutableList<String> = mutableListOf()
    val edges: MutableList<Map<String, String>> = mutableListOf()
    private val logger = KotlinLogging.logger {}

    /**
     * Parses the given file and returns true if the file was parsed correctly, false otherwise.
     * @param filepath the path to the file to parse
     * @param schemaPath the path to the schema file
     * @return true if the file was parsed correctly, false otherwise
     */
    override fun parse(filepath: String, schemaPath: String): Boolean {
        // read file
        val fileString: String = readFile(filepath) ?: return false

        // check file
        if (!checkFile(fileString)) {
            return false
        }

        // ensure file was read correctly
        return !(
            this.vertices.isEmpty() ||
                this.edges.isEmpty()
            )
    }

    /**
     * Checks if the given file string is valid.
     * @param fileString the file string to check
     * @return true if the file string is valid, false otherwise
     */
    private fun checkFile(fileString: String): Boolean {
        var failed: Boolean
        // split file into sections
        val sections: List<String> = fileString.split("{")
        // ensure there are only two sections & first sections is long enough & check digraph id {
        val digraph = "digraph"
        val trimmedSection0 = sections[0].trim()
        if (sections.size != 2 ||
            trimmedSection0.length < SECTION_LENGTH_BOUND ||
            trimmedSection0.substring(0, digraph.length) != digraph
        ) {
            return false
        }
        // check id after digraph
        val idAfterDigraph = trimmedSection0.substring(digraph.length).trim()
        failed = !(checkStringId(idAfterDigraph) || checkNumberId(idAfterDigraph))
        // check "} empty"
        val midAndEnd = sections[1].split("}")
        // ensure there is only one mid and one end and the end is empty
        if (midAndEnd.size != 2 || midAndEnd[1].trim() != "") {
            failed = true
        }
        if (failed) {
            return false
        }
        val verticesAndEdges = midAndEnd[0].split("-", limit = 2)
        // put last id still to edges
        val verticesAndFirstEdgeId = verticesAndEdges[0].split(";")
        if (verticesAndFirstEdgeId.size < 2) {
            return false
        }
        // get vertex list to iterate over
        val vertices = verticesAndFirstEdgeId.subList(0, verticesAndFirstEdgeId.size - 1)
        // iterate over vertices
        for (vertex in vertices) {
            if (!checkVertex(vertex)) {
                failed = true
            }
        }
        val edgeString = verticesAndFirstEdgeId[verticesAndFirstEdgeId.size - 1] + "-" + verticesAndEdges[1]
        // get edge list to iterator over
        val edges = edgeString.split(Regex("]\\s*;"))
        // ensure after edges is empty
        if (edges.last().trim() != "") {
            failed = true
        }
        // iterate over edges
        for (edge in edges.subList(0, edges.size - 1)) {
            if (!checkEdge(edge)) {
                failed = true
            }
        }
        return !failed
    }

    /**
     * Checks if the given vertex string is valid and adds it to the vertices list.
     * @param vertexString the vertex string to check (empty, numId, empty)
     * @return true if the vertex string is valid, false otherwise
     */
    private fun checkVertex(vertexString: String): Boolean {
        val trimmedString = vertexString.trim()
        // check id
        if (!checkNumberId(trimmedString)) {
            return false
        }
        this.vertices.add(trimmedString)
        return true
    }

    /**
     * Checks if the given edge string is valid and adds it to the edges list.
     * @param edgeString the edge string to check (empty, numId, empty, "->", empty, numId, empty, "[", attr)
     */
    private fun checkEdge(edgeString: String): Boolean {
        val trimmedString = edgeString.trim()
        val prePost = trimmedString.split("[")
        val ids = prePost[0].split("->")
        if (prePost.size != 2 || ids.size != 2) {
            return false
        }
        val source = ids[0].trim()
        val target = ids[1].trim()
        if (!checkNumberId(source) || !checkNumberId(target)) {
            return false
        }

        val edgeMap = getAttributes(prePost[1]) ?: return false
        edgeMap["source"] = source
        edgeMap["target"] = target
        this.edges.add(edgeMap)
        return true
    }

    /**
     * Returns a map of attributes from the given attribute string or null if the string is invalid.
     * @param attrString the attribute string to check (empty, attr, empty, attr, empty, attr, empty, ...) with 6 attr
     * = attrName, empty, "=", empty, attrValue, empty, ";"
     * @return a map of attributes or null if the string is invalid
     */
    private fun getAttributes(attrString: String): MutableMap<String, String>? {
        val argList = attrString.split(";")
        val idAttributes = listOf("village", "name", "heightLimit", "weight", "primaryType", "secondaryType")
        val valueTesters = listOf(
            ::checkStringId,
            ::checkStringId,
            ::checkNumberId,
            ::checkNumberId,
            { inp: String -> setOf("mainStreet", "sideStreet", "countyRoad").contains(inp) },
            { inp: String -> setOf("oneWayStreet", "tunnel", "none").contains(inp) }
        )
        val returnMap = mutableMapOf<String, String>()
        // check for correct amount of arguments
        if (argList.size != AMOUNT_ARGS + 1 || argList.last() != "") {
            return null
        }
        // check for correct arguments
        for (i in 0 until AMOUNT_ARGS) {
            val attribute = attributeChecker(argList[i], idAttributes[i], valueTesters[i]) ?: return null
            returnMap[idAttributes[i]] = attribute
        }
        // return attributes
        return returnMap
    }

    /**
     * Checks if the given string has the given attribute and if the attribute value is valid.
     * @param wholeString the string to check
     * @param attribute the attribute to check for
     * @param checkFunction the function to check the attribute value
     * @return the value string if the attribute is valid, null otherwise
     */
    private fun attributeChecker(wholeString: String, attribute: String, checkFunction: (String) -> Boolean): String? {
        val nameAndValue = wholeString.split("=")
        // check that there are two parts and the first part is the attribute
        if (nameAndValue.size != 2 || nameAndValue[0].trim() != attribute) {
            return null
        }
        // check if value is valid
        val value = nameAndValue[1].trim()
        if (!checkFunction(value)) {
            return null
        }
        return value
    }

    /**
     * Read file and return file-string.
     * @param filepath path to file
     * @return file-string
     */
    private fun readFile(filepath: String): String? {
        // try to open file
        return try {
            val file = File(filepath)
            file.readText(Charsets.UTF_8)
        } catch (e: IOException) {
            this.logger.error { "Could not open file $filepath, so error: $e occurred" }
            null
        }
    }

    /**
     * Checks if the given number id is in Int range, positive and without leading zeros.
     * @param id the id to check
     * @return true if the id is valid, false otherwise
     */
    private fun checkNumberId(id: String): Boolean {
        return Regex("0|[1-9][0-9]*").matches(id) && id.toIntOrNull() != null
    }

    /**
     * Checks if the given string id is valid.
     * @param id the id to check
     * @return true if the id is valid, false otherwise
     */
    private fun checkStringId(id: String): Boolean {
        return Regex("[a-zA-Z][a-zA-Z_]*").matches(id)
    }

    companion object {
        const val SECTION_LENGTH_BOUND = 8
        const val AMOUNT_ARGS = 6
    }
}
