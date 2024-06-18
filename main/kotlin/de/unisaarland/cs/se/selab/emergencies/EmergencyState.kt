package de.unisaarland.cs.se.selab.emergencies

/**
 *  enum class keeping track of the state of an emergency
 */
enum class EmergencyState {
    UNASSIGNED,
    ONGOING,
    BEING_RESOLVED,
    SUCCESS,
    FAILED
}
