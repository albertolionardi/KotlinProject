package de.unisaarland.cs.se.selab.simulation

import de.unisaarland.cs.se.selab.baseStations.BaseStation
import de.unisaarland.cs.se.selab.baseStations.FireStation
import de.unisaarland.cs.se.selab.baseStations.MedicalStation
import de.unisaarland.cs.se.selab.baseStations.PoliceStation
import de.unisaarland.cs.se.selab.emc.EMC
import de.unisaarland.cs.se.selab.emergencies.Emergency
import de.unisaarland.cs.se.selab.emergencies.Request
import de.unisaarland.cs.se.selab.ids.BaseStationID
import de.unisaarland.cs.se.selab.ids.VehicleID
import de.unisaarland.cs.se.selab.logging.Logger
import de.unisaarland.cs.se.selab.mapping.Finder
import de.unisaarland.cs.se.selab.requirements.Requirement
import de.unisaarland.cs.se.selab.vehicles.FireVehicle
import de.unisaarland.cs.se.selab.vehicles.MedicalVehicle
import de.unisaarland.cs.se.selab.vehicles.PoliceVehicle
import de.unisaarland.cs.se.selab.vehicles.Vehicle
import de.unisaarland.cs.se.selab.vehicles.VehicleState
import de.unisaarland.cs.se.selab.vehicles.VehicleType
import java.util.*

/**
 * Represents the Allocation Phase of the simulation, which handles the allocation of assets (vehicles) to emergencies.
 *
 * @param emc The Emergency Management Center responsible for tracking emergencies and resources.
 * @param logger The logger used for recording simulation events.
 * @param finder The finder used for location-based operations.
 */
class AllocationPhase(
    override val emc: EMC,
    override val logger: Logger,
    override val finder: Finder
) : SimulationPhase() {
    // queue for emergencies to check
    // ongoingEmergencies will be automatically sorted by ID (emergencies
    // implement the Comparable interface)
    // private val newRequests = ArrayDeque<Request>()
    private val ongoingEmergencies = PriorityQueue<Emergency>()

    private var curTick: Int = -1

    // at least one asset was found that fulfilled all requirements
    // but could not reach the emergency in time
    // if no assets have been (re-)allocated in this round and this flag is set,
    // a station will not request assets of its own type from other bases
    private var assetDistanceTooBig = false

    /**
     * Combined allocation phase & request phase.
     */
    fun allocateAssets(curTick: Int): List<VehicleID> {
        this.curTick = curTick

        val allocatedAssets = mutableListOf<VehicleID>()
        val openRequests = ArrayDeque<Request>()

        /* Initialize queue of emergencies to handle.
         * We use a queue because of the special case where an emergency's
         * requirement becomes unfulfilled again after one of the allocated
         * vehicles is reallocated to another emergency.
         * In this case, the emergency should be re-handled in the same tick
         * and thus needs to be added to the queue.
         * After adding an emergency, the queue needs to be sorted again
         * based on severity, then id (will be done automatically be the
         * priority queue)
         * TODO(is this correct?)
         */
        ongoingEmergencies.addAll(emc.ongoingEmergencies)

        /* allocate assets
         * ================================================================== */
        while (ongoingEmergencies.peek() != null) {
            val emergency = ongoingEmergencies.poll()

            val stationID = emc.getAssignedStation(emergency.id)
            val station = finder.getStationByID(stationID)

            // allocate
            val allocations = handleAllocationForEmergency(emergency, station)
            allocatedAssets.addAll(allocations)

            // reallocate
            // if there are still open requirements for this type of station
            // after the allocation phase
            var reAllocations = emptyList<VehicleID>()
            val requiredVehicleTypes = emergency
                .requirement
                .getVehicleTypesForStation(station)
            if (requiredVehicleTypes.isNotEmpty()) {
                reAllocations = handleAllocationForEmergency(
                    emergency,
                    station,
                    reallocation = true
                )
            }

            // make and send requests
            // if not a single asset was (re-)allocated but at least one asset
            // fulfilled all requirements except the distance to the emergency,
            // don't request assets of the stations own type
            val requestOwnType = if (allocations.isEmpty() && reAllocations.isEmpty()) {
                !this.assetDistanceTooBig
            } else {
                true
            }

            val newRequests = makeRequests(station, emergency, requestOwnType)
            openRequests.addAll(newRequests)

            // clear 'global' variable
            this.assetDistanceTooBig = false
        }

        /* handle requests
         * ================================================================== */
        while (openRequests.size > 0) {
            val curRequest = openRequests.removeFirst()
            val (allocations, newRequest) = handleRequest(curRequest)

            if (newRequest != null) {
                openRequests.add(newRequest)
            }

            allocatedAssets.addAll(allocations)
        }

        return allocatedAssets
    }

    private fun handleAllocationForEmergency(
        emergency: Emergency,
        station: BaseStation,
        reallocation: Boolean = false,
    ): List<VehicleID> {
        val requirement = emergency.requirement
        if (requirement.isFulfilled()) {
            // ongoing does not necessarily mean that it needs allocation
            return emptyList()
        }

        // get vehicles of assigned station
        val stationVehicles = station.vehicles.map { v -> emc.getVehicleByID(v) }

        if (!reallocation) {
            /* allocation
             * ============================================================== */
            // get the set of all vehicles in the station that -- individually --
            // could get allocated to the emergency
            val individuallyAllocatableVehicles = stationVehicles.filter { v ->
                // only check vehicles at the station
                v.isAvailable() && isAllocatableIndividually(v, station, emergency)
            }.toSet()

            // get the largest subset with the smallest ids
            val allocatableSubset: List<Vehicle> = getLargestAllocatableSubset(
                individuallyAllocatableVehicles,
                allocationCondition(requirement, station)
            )

            // actually allocate and log vehicles
            processAllocations(allocatableSubset, emergency, station)

            return allocatableSubset.map { it.id }
        } else {
            /* re-allocation
             *=============================================================== */
            // get the set of all vehicles currently allocated to an emergency
            // with lower severity OR returning from a previous emergency
            val individuallyReAllocatableVehicles = stationVehicles.filter { v ->
                v.isReAllocatable() && isAllocatableIndividually(v, station, emergency)
            }.toSet()

            // get the largest subset with the smallest ids
            val reAllocatableSubset = getLargestAllocatableSubset(
                individuallyReAllocatableVehicles,
                allocationCondition(requirement, station)
            )

            // actually re-allocate and log vehicles
            processAllocations(reAllocatableSubset, emergency, station, reallocate = true)

            return emptyList()
        }
    }

    /**
     * Process each asset that was collected to be allocated
     * by assigning the path, changing the state, assigning it the emergency
     * and logging.
     */
    private fun processAllocations(
        assets: List<Vehicle>,
        emergency: Emergency,
        station: BaseStation,
        reallocate: Boolean = false
    ) {
        val sortedAssets = assets.sortedBy { v -> v.id }
        for (vehicle in sortedAssets) {
            if (reallocate) {
                /* only when re-allocating */
                // set path
                val newPath = when (vehicle.state) {
                    VehicleState.DISPATCHED,
                    VehicleState.RETURNING ->
                        finder.getPath(
                            source = vehicle.getPath().getPosition(),
                            target = emergency.street,
                            height = vehicle.height
                        )

                    else -> finder.getPath(
                        source = finder.getVertexByStationID(station.id),
                        target = emergency.street,
                        height = vehicle.height
                    )
                }
                vehicle.setPath(newPath)

                // un-assign vehicle from old emergency
                // vehicles that are returning have already been unassigned
                // in the update phase
                if (vehicle.state != VehicleState.RETURNING) {
                    val oldEmergency = emc.getEmergencyByVehicle(vehicle.id)
                    emc.unAssignVehicle(vehicle.id, oldEmergency.id)
                    val oldRequiredTypes = oldEmergency.requirement.getVehicleTypesForStation(station)
                    oldRequiredTypes.add(vehicle.type)
                    recalculateRequirement(oldEmergency)

                    // add old emergency to queue of emergencies to be handled
                    this.ongoingEmergencies.add(oldEmergency)
                }

                // set state
                vehicle.state = VehicleState.DISPATCHED

                // log
                logger.assetReallocation(
                    vehicle.id,
                    emergency.id
                )
            } else {
                /* only when allocating */
                // staff vehicle
                station.staffVehicle(vehicle)

                // set state
                vehicle.state = VehicleState.ALLOCATED

                // set path
                val path = finder.getPath(
                    finder.getVertexByStationID(station.id),
                    emergency.street,
                    vehicle.height
                )
                vehicle.setPath(path)

                // log
                logger.assetAllocation(
                    vehicle.id,
                    emergency.id,
                    maxOf(this.weightToTick(path.remainingLength), 1)
                )
            }

            // assign to emergency
            this.emc.assignVehicleToEmergency(vehicle.id, emergency)

            // remove type and capacities from requirement
            val requirement = emergency.requirement
            val requiredVehicleTypes = requirement.getVehicleTypesForStation(station)
            requiredVehicleTypes.remove(vehicle.type)
            allocateVehicleReallocationHelper(vehicle, requirement)
        }
    }

    private fun recalculateRequirement(oldEmergency: Emergency) {
        val requirement = oldEmergency.requirement

        // get allocated vehicles
        val allocatedVehicles = emc.getVehiclesForEmergency(oldEmergency.id).map {
            emc.getVehicleByID(it)
        }
        val allocatedFireVehicles = allocatedVehicles.filterIsInstance<FireVehicle>()
        val allocatedPoliceVehicle = allocatedVehicles.filterIsInstance<PoliceVehicle>()
        val allocatedMedicalVehicles = allocatedVehicles.filterIsInstance<MedicalVehicle>()

        // calculate currently allocated capacity over all allocated vehicles
        val allocatedWaterCapacity = allocatedFireVehicles.sumOf { it.currentWaterCapacity }
        val allocatedCriminalCapacity = allocatedPoliceVehicle.sumOf { it.remainingCriminalCapacity }
        val allocatedPatientCapacity = allocatedMedicalVehicles.count {
            it.type == VehicleType.EMERGENCY_DOCTOR_CAR &&
                !it.hasPatient()
        }

        requirement.remainingWater = requirement.totalWater - allocatedWaterCapacity
        requirement.remainingCriminals = requirement.totalCriminals - allocatedCriminalCapacity
        requirement.remainingPatients = requirement.totalPatients - allocatedPatientCapacity
    }

    private fun allocateVehicleReallocationHelper(vehicle: Vehicle, requirement: Requirement) {
        when (vehicle.type) {
            VehicleType.POLICE_CAR -> requirement.remainingCriminals = maxOf(
                0,
                requirement.remainingCriminals - (vehicle as PoliceVehicle).remainingCriminalCapacity
            )
            VehicleType.FIRE_TRUCK_WATER -> requirement.remainingWater = maxOf(
                0,
                requirement.remainingWater - (vehicle as FireVehicle).currentWaterCapacity
            )
            VehicleType.AMBULANCE -> requirement.remainingPatients = maxOf(
                0,
                requirement.remainingPatients - 1
            )
            else -> {}
        }
    }

    /**
     * Return a function that is able to check whether a set of vehicles
     * is allowed to be allocated based on the specific requirement and station.
     */
    private fun allocationCondition(
        requirement: Requirement,
        station: BaseStation,
        reallocate: Boolean = false
    ): (Set<Vehicle>) -> Boolean {
        fun myFun(vehicleList: Set<Vehicle>): Boolean {
            // check staff
            // we know at this point that all vehicles individually can be staffed,
            // but we need to check if the station can handle staffing them all
            // at once.
            val vehicleStaff = vehicleList.sumOf { v -> if (v.isAvailable()) v.staffCapacity else 0 }
            if (!reallocate && vehicleStaff > station.getAvailableStaff()) {
                return false
            }

            // check that the number of occurrences of each vehicle type in the
            // vehicleList is <= the number of occurrences of the same type in
            // the list of required vehicle types
            val typeCountsVehicles = vehicleList
                .groupBy { v -> v.type }
                .mapValues { it.value.size }
            val requiredVehicleTypes = requirement.getVehicleTypesForStation(station)
            for ((vehicleType, count) in typeCountsVehicles) {
                if (requiredVehicleTypes.count { it == vehicleType } < count) {
                    return false
                }
            }

            return checkSpecialConstraints(requirement, vehicleList)
        }

        return ::myFun
    }

    private fun checkSpecialConstraints(requirement: Requirement, vehicleList: Set<Vehicle>): Boolean {
        // check special requirements for water capacity
        val waterTruckRequiredCount = requirement.fire.count { it == VehicleType.FIRE_TRUCK_WATER }
        val waterTruckPresentCount = vehicleList.count { it.type == VehicleType.FIRE_TRUCK_WATER }
        if (waterTruckRequiredCount > 0 && waterTruckPresentCount == waterTruckRequiredCount) {
            // if the number of water trucks present equals the maximum number
            // of water trucks we're allowed to allocate, check that they
            // fulfill or exceed the remaining water capacity
            val availableWaterCapacity = vehicleList.sumOf {
                (it as FireVehicle).currentWaterCapacity
            }
            if (availableWaterCapacity < requirement.remainingWater) {
                return false
            }
        }

        // check special requirements for criminal capacity
        val policeCarRequiredCount = requirement.police.count { it == VehicleType.POLICE_CAR }
        val policeCarPresentCount = vehicleList.count { it.type == VehicleType.POLICE_CAR }
        if (policeCarRequiredCount > 0 && policeCarPresentCount == policeCarRequiredCount) {
            // if the number of police cars present equals the maximum number
            // of police cars we're allowed to allocate, check that they
            // fulfill or exceed the remaining criminal capacity
            val availableCriminalCapacity = vehicleList.sumOf {
                (it as PoliceVehicle).remainingCriminalCapacity
            }
            if (availableCriminalCapacity < requirement.remainingCriminals) {
                return false
            }
        }

        return true
    }

    /**
     * Check whether the vehicle fits the requirement on its own,
     * regardless of the combination of vehicles that would be sent out later.
     *
     * Does not enforce a specific vehicle state (can be used both when
     * allocating and reallocating).
     */
    private fun isAllocatableIndividually(
        vehicle: Vehicle,
        station: BaseStation,
        emergency: Emergency
    ): Boolean {
        val requirement = emergency.requirement

        // check if vehicle type is required
        val requiredVehicleTypes = requirement.getVehicleTypesForStation(station)
        if (vehicle.type !in requiredVehicleTypes) {
            return false
        }

        // check state
        var isAllocatable: Boolean = when (vehicle.state) {
            // when the vehicle is available (currently at a station), check
            // if the station can staff it (else assume it to be staffed already)
            VehicleState.AVAILABLE -> station.canStaffVehicle(vehicle)

            // when the vehicle is dispatched, check if the emergency it has been
            // dispatched too is of lower severity
            VehicleState.DISPATCHED -> {
                val dispatchedEmergency = emc.getEmergencyByVehicle(vehicle.id)
                dispatchedEmergency.severity < emergency.severity
            }

            // returning vehicles are staffed and not assigned to an emergency,
            // so there is no special check required here

            else -> true
        }
        if (!isAllocatable) {
            return false
        }

        // check if the vehicle has the needed capacity
        isAllocatable = when (vehicle.type) {
            // check length of ladder
            VehicleType.FIRE_TRUCK_LADDER ->
                (vehicle as FireVehicle).ladderLength >= requirement.ladderLength

            // check water capacity
            VehicleType.FIRE_TRUCK_WATER ->
                (vehicle as FireVehicle).currentWaterCapacity != 0

            // check criminal capacity
            VehicleType.POLICE_CAR ->
                (vehicle as PoliceVehicle).remainingCriminalCapacity != 0

            // check patient capacity
            VehicleType.AMBULANCE -> !(vehicle as MedicalVehicle).hasPatient()

            else -> true
        }
        if (!isAllocatable) {
            return false
        }

        // check if vehicle can make it in time
        val travelPath = when (vehicle.state) {
            VehicleState.DISPATCHED,
            VehicleState.RETURNING ->
                finder.getPath(
                    source = vehicle.getPath().getPosition(),
                    target = emergency.street,
                    height = vehicle.height
                )

            else -> finder.getPath(
                source = finder.getVertexByStationID(station.id),
                target = emergency.street,
                height = vehicle.height
            )
        }

        val travelTicks = this.weightToTick(travelPath.remainingLength)
        val canMakeIt: Boolean = travelTicks <= emergency.ticksLeftForArrival(this.curTick)

        // trick detekt: do not change 'or' to '||'
        this.assetDistanceTooBig = this.assetDistanceTooBig or !canMakeIt
        return canMakeIt
    }

    /**
     * Generate allocatable subsets for the [allocatableVehicles] that
     * fulfill the condition [cond] and return the one with the largest
     * amount of vehicles.
     * In case of a draw, return the one with the smallest IDs.
     *
     * @return a list of vehicles sorted by ID
     */
    private fun getLargestAllocatableSubset(
        allocatableVehicles: Set<Vehicle>,
        cond: (Set<Vehicle>) -> Boolean
    ): List<Vehicle> {
        val allocatableSubsets = getAllocatableSubsets(allocatableVehicles, cond)
        if (allocatableSubsets.isEmpty()) {
            return emptyList()
        }

        // filter out all but the ones with the biggest size
        val maxSize = allocatableSubsets.maxOf { it.size }
        val largestSubsets = allocatableSubsets.filter { it.size == maxSize }
        if (largestSubsets.size == 1) {
            return largestSubsets.first().sortedBy { v -> v.id }
        }

        /* sort the rest in ascending order by ids
         * ==================================== */
        // from here on, all subsets (sublists) have the same size

        // convert all sets to lists and sort them internally by id
        val largestSublists = largestSubsets.map { it.sortedBy { v -> v.id } }

        // sort outer list and return first element
        return largestSublists
            .sortedWith(VehicleListIDComparator)
            .first()
    }

    /**
     * Investigate subsets of [lst] recursively until a subset is found
     * that fulfills the condition [cond].
     * With each iteration, the subsets will get gradually smaller.
     */
    private fun getAllocatableSubsets(
        lst: Set<Vehicle>,
        cond: (Set<Vehicle>) -> Boolean
    ): List<Set<Vehicle>> {
        if (lst.isEmpty()) {
            // if list empty, return
            return emptyList()
        } else if (cond(lst)) {
            // complete set fulfills condition -> no need to search further
            return listOf(lst)
        }

        val acc = mutableListOf<Set<Vehicle>>()

        val subsets: List<Set<Vehicle>> = lst.map {
            val newSet = lst.toMutableSet()
            newSet.remove(it)
            newSet
        }
        for (subset in subsets) {
            acc.addAll(getAllocatableSubsets(subset, cond))
        }

        return acc
    }

    private fun makeRequests(
        station: BaseStation,
        emergency: Emergency,
        requestOwnType: Boolean
    ): List<Request> {
        val requirement = emergency.requirement

        val constructedRequests = mutableListOf<Request>()
        var receiver: BaseStationID? = null

        if (requirement.requiresFire() && (station !is FireStation || requestOwnType)) {
            receiver = finder.getClosestStation(
                station.id,
                FireStation::class,
                setOf(station.id)
            )
        }
        if (receiver != null) {
            val request = Request(station.id, receiver, emergency.id)
            constructedRequests.add(request)
        }
        receiver = null

        if (requirement.requiresPolice() && (station !is PoliceStation || requestOwnType)) {
            receiver = finder.getClosestStation(
                station.id,
                PoliceStation::class,
                setOf(station.id)
            )
        }
        if (receiver != null) {
            val request = Request(station.id, receiver, emergency.id)
            constructedRequests.add(request)
        }
        receiver = null

        if (requirement.requiresMedical() && (station !is MedicalStation || requestOwnType)) {
            receiver = finder.getClosestStation(
                station.id,
                MedicalStation::class,
                setOf(station.id)
            )
        }
        if (receiver != null) {
            val request = Request(station.id, receiver, emergency.id)
            constructedRequests.add(request)
        }
        // receiver = null

        // sort requests in ascending order according to receiver id
        constructedRequests.sortBy { it.receiver }

        // log requests
        constructedRequests.forEach {
            it.id = emc.newRequestID()
            logger.assetRequest(it)
        }
        return constructedRequests
    }

    /**
     * Request phase.
     */
    private fun handleRequest(request: Request): Pair<List<VehicleID>, Request?> {
        // get station
        val stationID = request.receiver
        val station = finder.getStationByID(stationID)

        // get emergency
        val emergency = emc.getEmergencyByID(request.emergency)

        // try to allocate vehicles & log successful allocations
        val allocations = handleAllocationForEmergency(emergency, station)

        // check if a new request is necessary
        //  - new station is searched starting from original sender
        //  - in the request phase, there is no need to check if the current
        //    station was able to allocate any vehicle at all
        val requiredVehicleTypes = emergency
            .requirement
            .getVehicleTypesForStation(station)
        // return if all required vehicles have been allocated
        if (requiredVehicleTypes.isEmpty()) {
            return Pair(allocations, null)
        }

        // else, find next station for request
        val receiver = finder.getClosestStation(
            request.sender,
            when (station) {
                is FireStation -> FireStation::class
                is PoliceStation -> PoliceStation::class
                is MedicalStation -> MedicalStation::class
            },
            request.visitedStations
        )

        return if (receiver == null) {
            // no further stations can be found -> request fails
            this.logger.requestFail(request)
            Pair(allocations, null)
        } else {
            val newRequest = Request(receiver, request)
            newRequest.id = emc.newRequestID()
            this.logger.assetRequest(newRequest)
            Pair(allocations, newRequest)
        }
    }
}

/**
 * Custom comparator for comparing lists of vehicles (used by
 * [AllocationPhase.getLargestAllocatableSubset] above).
 * If the sizes differ, the list with the smaller size will be sorted
 * first.
 * Else, do a lexical comparison on the IDs of the respective entries.
 */
object VehicleListIDComparator : Comparator<List<Vehicle>> {
    override fun compare(list1: List<Vehicle>, list2: List<Vehicle>): Int {
        // make sure lengths are equal
        if (list1.size != list2.size) {
            return list1.size - list2.size
        }

        for (i in list1.indices) {
            // return cannot be lifted out, do not trust IntelliJ
            when {
                list1[i].id < list2[i].id -> return -1
                list1[i].id > list2[i].id -> return 1
                else -> continue
            }
        }

        return 0
    }
}
