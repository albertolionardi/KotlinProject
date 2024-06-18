package de.unisaarland.cs.se.selab.emc

/**
 * Class that represents the statistics that are being displayed at the end of the simulation
 * @param receivedEmergencies number of received emergencies
 * @param resolvedEmergencies number of resolved emergencies
 * @param failedEmergencies number of failed emergencies
 * @param ongoingEmergencies number of still ongoing emergencies after the end of the simulation
 * @param reroutedAssets number of rerouted assets during the whole simulation
 */
class Statistics {
    var receivedEmergencies: Int = 0
    var resolvedEmergencies: Int = 0
    var failedEmergencies: Int = 0
    var ongoingEmergencies: Int = 0
    var reroutedAssets: Int = 0
}
