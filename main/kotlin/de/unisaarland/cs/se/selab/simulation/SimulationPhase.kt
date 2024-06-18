package de.unisaarland.cs.se.selab.simulation

import de.unisaarland.cs.se.selab.emc.EMC
import de.unisaarland.cs.se.selab.logging.Logger
import de.unisaarland.cs.se.selab.mapping.Finder
import kotlin.math.ceil

/**
 * abstract class simulaton phase which all phases inherit from
 * @param emc an emc to access everything
 * @param logger a logger to log everything
 * @param finder a finder to use the algorithm
 */
abstract class SimulationPhase {
    protected abstract val emc: EMC
    protected abstract val logger: Logger
    protected abstract val finder: Finder

    /**
     * uses the weight to calculate how many ticks it needs to process the amount of ticks
     */
    fun weightToTick(weight: Int): Int {
        return ceil(weight.toFloat() / CONVERSION_FACTOR).toInt()
    }

    /**
     * uses the tick to calculate how much weight it has
     */
    fun tickToWeight(tick: Int): Int = tick * CONVERSION_FACTOR

    companion object {
        const val CONVERSION_FACTOR = 10
    }
}
