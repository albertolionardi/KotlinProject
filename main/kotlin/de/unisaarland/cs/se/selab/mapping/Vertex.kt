package de.unisaarland.cs.se.selab.mapping

import de.unisaarland.cs.se.selab.ids.StreetID
import de.unisaarland.cs.se.selab.ids.VertexID

/**
 * data class vertex which stores its id and all the streetIds connected to it
 */
data class Vertex(val id: VertexID, val connections: List<StreetID>)
