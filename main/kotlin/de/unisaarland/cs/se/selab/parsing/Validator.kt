package de.unisaarland.cs.se.selab.parsing

/**
 * This is the validator interface.
 */
interface Validator {
    /**
     * All validators have to expose a validateData function, to validate data.
     * @param accumulator is an Accumulator object for testing and data storing
     * @return the modified accumulator or null if something went wrong
     */
    fun validateData(accumulator: Accumulator): Boolean
}
