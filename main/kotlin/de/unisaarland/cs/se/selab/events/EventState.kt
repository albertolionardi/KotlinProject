package de.unisaarland.cs.se.selab.events

/**
 * state of  event help us when updating the event  in update phase
 */
enum class EventState {
    INACTIVE,
    RUNNING,
    SUSPENDED,
    ENDED,
}
