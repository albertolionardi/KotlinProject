package de.unisaarland.cs.se.selab.emc

import de.unisaarland.cs.se.selab.logging.Logger
import de.unisaarland.cs.se.selab.mapping.Finder
import de.unisaarland.cs.se.selab.parsing.Accumulator
import de.unisaarland.cs.se.selab.parsing.CountyParser
import de.unisaarland.cs.se.selab.parsing.CountyValidator
import de.unisaarland.cs.se.selab.parsing.EmergencyValidator
import de.unisaarland.cs.se.selab.parsing.EventValidator
import de.unisaarland.cs.se.selab.parsing.JsonParser
import de.unisaarland.cs.se.selab.parsing.StationValidator
import de.unisaarland.cs.se.selab.parsing.VehicleValidator
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter

/**
 * Builder for the class [CoreSimulation].
 * After parsing the configuration files, a call to [build] will return
 * an instance of CoreSimulation.
 */
class CoreSimulationBuilder(out: String, log: Boolean = true) {
    private val logger: Logger
    init {
        logger = Logger(
            if (log) {
                when (out) {
                    "", "/dev/stdout" -> PrintWriter(System.out)
                    else -> File(out).printWriter()
                }
            } else {
                // let all logs go to untracked output stream
                PrintWriter(ByteArrayOutputStream())
            }
        )
    }

    val accumulator = Accumulator()

    // boolean flag to make sure build() is not called before successfully
    // parsing the configuration
    private var parsingSuccessful = false

    /**
     * Parse and validate the three configuration files and create intermediary
     * representations of the data needed to build the CoreSimulation.
     * @param countyPath path to county dot file
     * @param assetsPath path to assets JSON file
     * @param scenarioPath path to scenario JSON file
     * @return boolean flag indicating success of parsing
     */
    fun parseConfig(
        countyPath: String,
        assetsPath: String,
        scenarioPath: String
    ): Boolean {
        // parse & validate county
        val countyParser = CountyParser()
        if (
            !countyParser.parse(countyPath, "address.schema") ||
            !CountyValidator(countyParser).validateData(this.accumulator)
        ) {
            logger.initInfo(countyPath, false)
            return false
        }
        logger.initInfo(countyPath, true)

        // parse & validate assets
        val assetParser = JsonParser()
        if (
            !assetParser.parse(assetsPath, "assets.schema") ||
            !StationValidator(assetParser).validateData(this.accumulator) ||
            !VehicleValidator(assetParser).validateData(this.accumulator)
        ) {
            logger.initInfo(assetsPath, false)
            return false
        }
        logger.initInfo(assetsPath, true)

        // parse & validate scenario
        val scenarioParser = JsonParser()
        if (
            !scenarioParser.parse(scenarioPath, "simulation.schema") ||
            !EmergencyValidator(scenarioParser).validateData(this.accumulator) ||
            !EventValidator(scenarioParser).validateData(this.accumulator)
        ) {
            logger.initInfo(scenarioPath, false)
            return false
        }
        logger.initInfo(scenarioPath, true)

        this.parsingSuccessful = true
        return true
    }

    /**
     * Return an instance of [CoreSimulation].
     * @throws IllegalStateException if the configuration has not successfully been parsed yet.
     */
    fun build(): CoreSimulation {
        if (!parsingSuccessful) {
            error("config file data was not parsed successfully")
        }
        val (finder, emc) = getFinderAndEMC()
        return CoreSimulation(emc, finder, this.logger)
    }

    /**
     * Return instances of [EMC] and [Finder] to circumvent the creation of a
     * CoreSimulation if only those classes are of interest.
     */
    fun getFinderAndEMC(): Pair<Finder, EMC> {
        val finder = accumulator.returnFinder()
        val emc = EMC(
            accumulator.emergencyTickMap,
            accumulator.eventTickMap,
            accumulator.vehicleToStationMap,
            accumulator.vehicleMap
        )
        return Pair(finder, emc)
    }
}
