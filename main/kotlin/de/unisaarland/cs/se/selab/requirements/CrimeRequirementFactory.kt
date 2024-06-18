package de.unisaarland.cs.se.selab.requirements

import de.unisaarland.cs.se.selab.vehicles.VehicleType

/**
 * A factory for creating crime-related requirements with different severity levels.
 */
object CrimeRequirementFactory : RequirementFactory {
    private const val PATIENTS = 1
    private const val CRIMINALS_LOW = 1
    private const val CRIMINALS_MEDIUM: Int = 4
    private const val CRIMINALS_HIGH: Int = 8

    /**
     * Create a low-severity crime requirement.
     *
     * @return A low-severity crime requirement.
     */
    override fun createLowSeverityRequirement(): Requirement {
        val police = mutableListOf(VehicleType.POLICE_CAR)

        return Requirement(
            police,
            mutableListOf(),
            mutableListOf(),
            0,
            CRIMINALS_LOW,
            0,
            0
        )
    }

    /**
     * Create a medium-severity crime requirement.
     *
     * @return A medium-severity crime requirement.
     */
    override fun createMediumSeverityRequirement(): Requirement {
        val police = mutableListOf(
            VehicleType.POLICE_CAR,
            VehicleType.POLICE_CAR,
            VehicleType.POLICE_CAR,
            VehicleType.POLICE_CAR,
            VehicleType.POLICE_K9_CAR
        )
        val medical = mutableListOf(VehicleType.AMBULANCE)

        return Requirement(
            police,
            mutableListOf(),
            medical,
            0,
            CRIMINALS_MEDIUM,
            0,
            0
        )
    }

    /**
     * Create a high-severity crime requirement.
     *
     * @return A high-severity crime requirement.
     */
    override fun createHighSeverityRequirement(): Requirement {
        val police = mutableListOf(
            VehicleType.POLICE_CAR,
            VehicleType.POLICE_CAR,
            VehicleType.POLICE_CAR,
            VehicleType.POLICE_CAR,
            VehicleType.POLICE_CAR,
            VehicleType.POLICE_CAR,
            VehicleType.POLICE_MOTORCYCLE,
            VehicleType.POLICE_MOTORCYCLE,
            VehicleType.POLICE_K9_CAR,
            VehicleType.POLICE_K9_CAR
        )
        val fire = mutableListOf(VehicleType.FIREFIGHTER_TRANSPORTER)
        val medical = mutableListOf(
            VehicleType.AMBULANCE,
            VehicleType.AMBULANCE
        )

        return Requirement(
            police,
            fire,
            medical,
            0,
            CRIMINALS_HIGH,
            PATIENTS,
            0
        )
    }
}
