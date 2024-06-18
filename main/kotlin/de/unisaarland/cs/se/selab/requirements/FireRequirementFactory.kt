package de.unisaarland.cs.se.selab.requirements

import de.unisaarland.cs.se.selab.vehicles.VehicleType

/**
 * A factory for creating fire-related requirements with different severity levels.
 */
object FireRequirementFactory : RequirementFactory {
    private const val WATER_LOW: Int = 1200
    private const val WATER_MEDIUM: Int = 3000
    private const val WATER_HIGH: Int = 5400
    private const val LADDER_MEDIUM: Int = 30
    private const val LADDER_HIGH: Int = 40

    /**
     * Create a low-severity fire requirement.
     *
     * @return A low-severity fire requirement.
     */
    override fun createLowSeverityRequirement(): Requirement {
        val fire = mutableListOf(
            VehicleType.FIRE_TRUCK_WATER,
            VehicleType.FIRE_TRUCK_WATER
        )

        return Requirement(
            mutableListOf(),
            fire,
            mutableListOf(),
            0,
            0,
            0,
            WATER_LOW
        )
    }

    /**
     * Create a medium-severity fire requirement.
     *
     * @return A medium-severity fire requirement.
     */
    override fun createMediumSeverityRequirement(): Requirement {
        val fire = mutableListOf(
            VehicleType.FIRE_TRUCK_WATER,
            VehicleType.FIRE_TRUCK_WATER,
            VehicleType.FIRE_TRUCK_WATER,
            VehicleType.FIRE_TRUCK_WATER,
            VehicleType.FIRE_TRUCK_LADDER,
            VehicleType.FIREFIGHTER_TRANSPORTER
        )
        val medical = mutableListOf(VehicleType.AMBULANCE)

        return Requirement(
            mutableListOf(),
            fire,
            medical,
            LADDER_MEDIUM,
            0,
            1,
            WATER_MEDIUM
        )
    }

    /**
     * Create a high-severity fire requirement.
     *
     * @return A high-severity fire requirement.
     */
    override fun createHighSeverityRequirement(): Requirement {
        val police = mutableListOf<VehicleType>()
        val fire = mutableListOf(
            VehicleType.FIRE_TRUCK_WATER,
            VehicleType.FIRE_TRUCK_WATER,
            VehicleType.FIRE_TRUCK_WATER,
            VehicleType.FIRE_TRUCK_WATER,
            VehicleType.FIRE_TRUCK_WATER,
            VehicleType.FIRE_TRUCK_WATER,
            VehicleType.FIRE_TRUCK_LADDER,
            VehicleType.FIRE_TRUCK_LADDER,
            VehicleType.FIREFIGHTER_TRANSPORTER,
            VehicleType.FIREFIGHTER_TRANSPORTER
        )
        val medical = mutableListOf(
            VehicleType.AMBULANCE,
            VehicleType.AMBULANCE,
            VehicleType.EMERGENCY_DOCTOR_CAR
        )

        return Requirement(
            police,
            fire,
            medical,
            LADDER_HIGH,
            0,
            2,
            WATER_HIGH
        )
    }
}
