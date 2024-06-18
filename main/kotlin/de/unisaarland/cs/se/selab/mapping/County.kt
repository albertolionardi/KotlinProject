package de.unisaarland.cs.se.selab.mapping

import de.unisaarland.cs.se.selab.baseStations.BaseStation
import de.unisaarland.cs.se.selab.baseStations.FireStation
import de.unisaarland.cs.se.selab.baseStations.MedicalStation
import de.unisaarland.cs.se.selab.baseStations.PoliceStation
import de.unisaarland.cs.se.selab.ids.BaseStationID
import de.unisaarland.cs.se.selab.ids.StreetID
import de.unisaarland.cs.se.selab.ids.VertexID
import kotlin.reflect.KClass

/**
 * class county which stores all maps related to vertices, streets and base stations
 */
class County(
    vertexList: List<Vertex>,
    streetList: List<Street>,
    stationList: List<BaseStation>,
    private val stationToVertexMap: Map<BaseStationID, VertexID>
) {
    private val vertexMap: Map<VertexID, Vertex>
    private val streetMap: Map<StreetID, Street>
    private val baseStationMap: Map<BaseStationID, BaseStation>
    private val streetTypeMap: Map<StreetPrimaryType, List<Street>>
    private val vertexToStationMap: Map<VertexID, BaseStationID>

    private val fireStations: List<FireStation> = stationList.filterIsInstance<FireStation>()
    private val policeStations: List<PoliceStation> = stationList.filterIsInstance<PoliceStation>()
    private val medicalStations: List<MedicalStation> = stationList.filterIsInstance<MedicalStation>()

    init {
        val typeToStreets = mutableMapOf<StreetPrimaryType, List<Street>>()
        val vertexToStation = mutableMapOf<VertexID, BaseStationID>()

        typeToStreets[StreetPrimaryType.MAINSTREET] = streetList.filter {
            it.pType == StreetPrimaryType.MAINSTREET
        }
        typeToStreets[StreetPrimaryType.SIDESTREET] = streetList.filter {
            it.pType == StreetPrimaryType.SIDESTREET
        }
        typeToStreets[StreetPrimaryType.COUNTYROAD] = streetList.filter {
            it.pType == StreetPrimaryType.COUNTYROAD
        }

        stationToVertexMap.forEach { (bId, vId) -> vertexToStation[vId] = bId }

        vertexMap = vertexList.associateBy { vertex -> vertex.id }
        streetMap = streetList.associateBy { street -> street.id }
        baseStationMap = stationList.associateBy { station -> station.id }
        streetTypeMap = typeToStreets
        vertexToStationMap = vertexToStation
    }

    /**
     * returns the vertex or null depending on the given vertexId
     */
    fun getVertexByID(vId: VertexID): Vertex {
        return vertexMap.getOrElse(vId) {
            throw NoSuchElementException("no vertex associated with ID $vId")
        }
    }

    /**
     * returns the station or null depending on the given baseStationId
     */
    fun getStationByID(bId: BaseStationID): BaseStation {
        return baseStationMap.getOrElse(bId) {
            throw NoSuchElementException("no baseStation associated with ID $bId")
        }
    }

    /**
     * Return the list of all stations of the specified type.
     */
    fun <T : BaseStation> getStationsByType(stationType: KClass<T>): List<BaseStation> {
        return when (stationType) {
            FireStation::class -> fireStations
            PoliceStation::class -> policeStations
            MedicalStation::class -> medicalStations
            else -> error("unknown station type requested")
        }
    }

    /**
     * returns the baseStationID or null depending on the given vertexId
     */
    fun getStationByVertexID(vId: VertexID): BaseStationID? {
        return vertexToStationMap[vId]
    }

    /**
     * returns the vertexId or null depending on the given baseStationId
     */
    fun getVertexByStationID(bId: BaseStationID): VertexID {
        return stationToVertexMap.getOrElse(bId) {
            throw NoSuchElementException("no vertexID associated with ID $bId")
        }
    }

    /**
     * returns the street or null depending on the given streetId
     */
    fun getStreetByID(sId: StreetID): Street {
        return streetMap.getOrElse(sId) {
            throw NoSuchElementException("no street associated with ID $sId")
        }
    }

    /**
     * returns a list of streets depending on the given streetPrimaryType
     */
    fun getStreetsByType(type: StreetPrimaryType): List<Street> {
        return streetTypeMap[type].orEmpty()
    }
}
