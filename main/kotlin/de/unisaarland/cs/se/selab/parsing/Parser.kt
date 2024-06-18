package de.unisaarland.cs.se.selab.parsing

/**
 * Parser Interface.
 */
interface Parser {
    /**
     * Parses file.
     * @param filepath path to file
     * @param schemaPath path to schema file
     * @return true if file was parsed successfully
     */
    fun parse(filepath: String, schemaPath: String): Boolean
}
