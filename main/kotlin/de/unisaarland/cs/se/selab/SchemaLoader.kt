package de.unisaarland.cs.se.selab

import io.github.oshai.kotlinlogging.KotlinLogging
import org.everit.json.schema.Schema
import org.everit.json.schema.loader.SchemaClient
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Collectors

/**
 * Loads a resource from the classpath.
 */
private fun loadResource(subclass: Class<*>, name: String?): String? {
    val logger = KotlinLogging.logger("ResourceLoader")
    logger.trace { "loading ${subclass.classLoader.getResource(name)}" }
    try {
        InputStreamReader(
            Objects.requireNonNull(subclass.classLoader.getResourceAsStream(name)),
            StandardCharsets.UTF_8
        ).use { input ->
            BufferedReader(input).use { reader ->
                return reader.lines().collect(Collectors.joining("\n"))
            }
        }
    } catch (e: IOException) {
        logger.error { e.message + e }
        return null
    }
}

/**
 * Loads a schema from the classpath.
 * @param subclass the class that is calling this function
 * @param name the name of the schema file (e.g. assets.schema)
 */
fun getSchema(subclass: Class<*>, name: String): Schema? {
    val resource = loadResource(subclass, "schema/$name")
    return resource?.let {
        SchemaLoader
            .builder()
            .schemaClient(
                SchemaClient.classPathAwareClient()
            )
            .schemaJson(JSONObject(it))
            .resolutionScope("classpath://schema/")
            .build()
            .load()
            .build()
    }
}
