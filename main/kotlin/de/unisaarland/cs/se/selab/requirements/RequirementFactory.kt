package de.unisaarland.cs.se.selab.requirements

/**
 * Interface for creating requirements with different severity levels.
 */
interface RequirementFactory {
    /**
     * Create a low-severity requirement.
     *
     * @return A low-severity requirement.
     */
    fun createLowSeverityRequirement(): Requirement

    /**
     * Create a medium-severity requirement.
     *
     * @return A medium-severity requirement.
     */
    fun createMediumSeverityRequirement(): Requirement

    /**
     * Create a high-severity requirement.
     *
     * @return A high-severity requirement.
     */
    fun createHighSeverityRequirement(): Requirement
}
