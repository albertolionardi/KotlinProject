package de.unisaarland.cs.se.selab.parsing

import de.unisaarland.cs.se.selab.getSchema
import io.github.oshai.kotlinlogging.KotlinLogging
import org.everit.json.schema.Schema
import org.everit.json.schema.ValidationException
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

/**
 * Parses json files.
 */
class JsonParser : Parser {
    var jsonObject: JSONObject? = null

    private val logger = KotlinLogging.logger {}

    /**
     * Parses json file.
     * @param filepath path to json file
     * @return true if json file was parsed successfully
     */

    override fun parse(filepath: String, schemaPath: String): Boolean {
        val file: File

        try {
            file = File(filepath)
        } catch (e: FileNotFoundException) {
            logger.error(e) { "file '$filepath' does not exist" }
            return false
        }

        try {
            this.jsonObject = JSONObject(file.readText(Charsets.UTF_8))
        } catch (e: JSONException) {
            logger.error(e) { "Error while parsing json file" }
            return false
        }

        return checkSchema(schemaPath)
    }

    /**
     * Checks if json matches provided schema.
     * @param schemaPath path to schema file
     * @return true if json matches schema
     */
    private fun checkSchema(schemaPath: String): Boolean {
        val schema: Schema = getSchema(this.javaClass, schemaPath) ?: error("Schema not found")

        return try {
            schema.validate(this.jsonObject)
            true
        } catch (e: ValidationException) {
            logger.error(e) { "Error while validating json file" }
            false
        }
    }
}
