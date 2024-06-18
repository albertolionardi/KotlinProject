package de.unisaarland.cs.se.selab.requirements

import de.unisaarland.cs.se.selab.vehicles.VehicleType

/**
 * A factory for creating accident-related requirements with different severity levels.
 */
object AccidentRequirementFactory : RequirementFactory {
    private const val PATIENTS_LOW = 0
    private const val PATIENTS_MEDIUM = 1
    private const val PATIENTS_HIGH = 2

    /**
     * Create a low-severity accident requirement.
     *
     * @return A low-severity accident requirement.
     */
    override fun createLowSeverityRequirement(): Requirement {
        return Requirement(
            mutableListOf(),
            mutableListOf(VehicleType.FIRE_TRUCK_TECHNICAL),
            mutableListOf(),
            0,
            0,
            PATIENTS_LOW,
            0
        )
    }

    /**
     * Create a medium-severity accident requirement.
     *
     * @return A medium-severity accident requirement.
     */
    override fun createMediumSeverityRequirement(): Requirement {
        val police = mutableListOf(
            VehicleType.POLICE_MOTORCYCLE,
            VehicleType.POLICE_CAR,
        )
        val fire = mutableListOf(
            VehicleType.FIRE_TRUCK_TECHNICAL,
            VehicleType.FIRE_TRUCK_TECHNICAL
        )
        val medical = mutableListOf(VehicleType.AMBULANCE)

        return Requirement(
            police,
            fire,
            medical,
            0,
            0,
            PATIENTS_MEDIUM,
            0
        )
    }

    /**
     * Create a high-severity accident requirement.
     *
     * @return A high-severity accident requirement.
     */
    override fun createHighSeverityRequirement(): Requirement {
        val police = mutableListOf(
            VehicleType.POLICE_MOTORCYCLE,
            VehicleType.POLICE_MOTORCYCLE,
            VehicleType.POLICE_CAR,
            VehicleType.POLICE_CAR,
            VehicleType.POLICE_CAR,
            VehicleType.POLICE_CAR
        )
        val fire = mutableListOf(
            VehicleType.FIRE_TRUCK_TECHNICAL,
            VehicleType.FIRE_TRUCK_TECHNICAL,
            VehicleType.FIRE_TRUCK_TECHNICAL,
            VehicleType.FIRE_TRUCK_TECHNICAL
        )
        val medical = mutableListOf(
            VehicleType.AMBULANCE,
            VehicleType.AMBULANCE,
            VehicleType.AMBULANCE,
            VehicleType.EMERGENCY_DOCTOR_CAR,
        )

        return Requirement(
            police,
            fire,
            medical,
            0,
            0,
            PATIENTS_HIGH,
            0
        )
    }
}
