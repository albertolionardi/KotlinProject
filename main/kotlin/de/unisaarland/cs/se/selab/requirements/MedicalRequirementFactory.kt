package de.unisaarland.cs.se.selab.requirements

import de.unisaarland.cs.se.selab.vehicles.VehicleType

/**
 * A factory for creating medical-related requirements with different severity levels.
 */
object MedicalRequirementFactory : RequirementFactory {
    private const val PATIENTS_LOW = 0
    private const val PATIENTS_MEDIUM = 2
    private const val PATIENTS_HIGH = 5

    /**
     * Create a low-severity medical requirement.
     *
     * @return A low-severity medical requirement.
     */
    override fun createLowSeverityRequirement(): Requirement {
        val medical = mutableListOf(VehicleType.AMBULANCE)

        return Requirement(
            mutableListOf(),
            mutableListOf(),
            medical,
            0,
            0,
            PATIENTS_LOW,
            0
        )
    }

    /**
     * Create a medium-severity medical requirement.
     *
     * @return A medium-severity medical requirement.
     */
    override fun createMediumSeverityRequirement(): Requirement {
        val medical = mutableListOf(
            VehicleType.AMBULANCE,
            VehicleType.AMBULANCE,
            VehicleType.EMERGENCY_DOCTOR_CAR,
        )

        return Requirement(
            mutableListOf(),
            mutableListOf(),
            medical,
            0,
            0,
            PATIENTS_MEDIUM,
            0
        )
    }

    /**
     * Create a high-severity medical requirement.
     *
     * @return A high-severity medical requirement.
     */
    override fun createHighSeverityRequirement(): Requirement {
        val fire = mutableListOf(
            VehicleType.FIRE_TRUCK_TECHNICAL,
            VehicleType.FIRE_TRUCK_TECHNICAL
        )
        val medical = mutableListOf(
            VehicleType.AMBULANCE,
            VehicleType.AMBULANCE,
            VehicleType.AMBULANCE,
            VehicleType.AMBULANCE,
            VehicleType.AMBULANCE,
            VehicleType.EMERGENCY_DOCTOR_CAR,
            VehicleType.EMERGENCY_DOCTOR_CAR,
        )

        return Requirement(
            mutableListOf(),
            fire,
            medical,
            0,
            0,
            PATIENTS_HIGH,
            0
        )
    }
}
