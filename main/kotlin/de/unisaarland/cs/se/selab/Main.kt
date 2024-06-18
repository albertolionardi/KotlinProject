package de.unisaarland.cs.se.selab

import de.unisaarland.cs.se.selab.emc.CoreSimulationBuilder
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required

/**
 * This is the entry point of the simulation.
 */
fun main(args: Array<String>) {
    val parser = ArgParser("WitchDoctor")
    val mapValue by parser.option(
        ArgType.String,
        fullName = "map",
        shortName = "m",
        description = "path to map"
    ).required()
    val assetValue by parser.option(
        ArgType.String,
        fullName = "assets",
        shortName = "a",
        description = "path to asset"
    ).required()
    val scenarioValue by parser.option(
        ArgType.String,
        fullName = "scenario",
        shortName = "s",
        description = "path to scenario"
    ).required()
    val ticksValue by parser.option(
        ArgType.Int,
        fullName = "ticks",
        shortName = "t",
        description = "Maximum allowed number of simulation ticks."
    ).default(0)
    val outValue by parser.option(
        ArgType.String,
        fullName = "out",
        shortName = "o",
        description = "path to output file"
    ).default("")
    // options --help & -h are generated automatically
    // see https://github.com/Kotlin/kotlinx-cli

    // parse command line arguments
    parser.parse(args)

    // build core simulation
    val csBuilder = CoreSimulationBuilder(outValue)
    if (!csBuilder.parseConfig(mapValue, assetValue, scenarioValue)) {
        return
    }
    val coreSimulation = csBuilder.build()

    // run simulation
    coreSimulation.run(maxTicks = ticksValue)
}
