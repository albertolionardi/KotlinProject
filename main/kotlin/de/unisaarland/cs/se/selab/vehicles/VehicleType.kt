package de.unisaarland.cs.se.selab.vehicles

/**
 * Enum representing the types of vehicles available in the simulation.
 */
enum class VehicleType {
    // fire
    FIRE_TRUCK_WATER,
    FIRE_TRUCK_TECHNICAL,
    FIRE_TRUCK_LADDER,
    FIREFIGHTER_TRANSPORTER,

    // police
    POLICE_CAR,
    POLICE_K9_CAR,
    POLICE_MOTORCYCLE,

    // medical
    AMBULANCE,
    EMERGENCY_DOCTOR_CAR;

    /**
     * Check if the vehicle type belongs to the police category.
     *
     * @return `true` if the vehicle type is a police type; otherwise, `false`.
     */
    fun isPoliceType(): Boolean {
        return when (this) {
            POLICE_CAR,
            POLICE_K9_CAR,
            POLICE_MOTORCYCLE -> true
            else -> false
        }
    }

    /**
     * Check if the vehicle type belongs to the fire category.
     *
     * @return `true` if the vehicle type is a fire type; otherwise, `false`.
     */
    fun isFireType(): Boolean {
        return when (this) {
            FIRE_TRUCK_WATER,
            FIRE_TRUCK_TECHNICAL,
            FIRE_TRUCK_LADDER,
            FIREFIGHTER_TRANSPORTER -> true
            else -> false
        }
    }

    /**
     * Check if the vehicle type belongs to the medical category.
     *
     * @return `true` if the vehicle type is a medical type; otherwise, `false`.
     */
    fun isMedicalType(): Boolean {
        return when (this) {
            AMBULANCE,
            EMERGENCY_DOCTOR_CAR -> true
            else -> false
        }
    }
}
