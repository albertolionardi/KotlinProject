package de.unisaarland.cs.se.selab.parsing

import org.json.JSONObject

/**
 * Helper class for validating json objects.
 */
open class JsonValidationHelper {
    var intInvalid = false

    /**
     * Validates the id being present or not.
     * @param jsonObj the json object to validate
     * @param keyList the list of keys to check for
     * @return list of booleans indicating presence or absence of keys
     */
    fun getObjPresence(jsonObj: JSONObject, keyList: List<String>): List<Boolean> {
        val returnList = mutableListOf<Boolean>()
        for (key in keyList) {
            returnList += jsonObj.has(key)
        }
        return returnList
    }

    /**
     * Validates the id being an integer.
     * @param jsonObj the json object to validate
     * @param key the key to check for
     * @return the id if it is an integer, -1 otherwise and sets env. flag intInvalid to true
     */
    fun getAssertedInt(jsonObj: JSONObject, key: String): Int {
        // calling .getInt() directly on jsonObj will truncate the integer
        // to within bounds and the null check will never be triggered
        val value = jsonObj.get(key).toString().toIntOrNull()
        return if (value == null) {
            intInvalid = true
            -1
        } else {
            value
        }
    }
}
