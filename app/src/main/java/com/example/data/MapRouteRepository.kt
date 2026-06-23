package com.example.data

import kotlinx.coroutines.flow.Flow

class MapRouteRepository(private val dao: MapRouteDao) {
    val allPins: Flow<List<SavedPin>> = dao.getAllPins()
    val allRoutes: Flow<List<SavedRoute>> = dao.getAllRoutes()

    suspend fun insertPin(pin: SavedPin): Long = dao.insertPin(pin)

    suspend fun deletePinById(id: Int) = dao.deletePinById(id)

    suspend fun clearPins() = dao.clearAllPins()

    suspend fun insertRoute(route: SavedRoute): Long = dao.insertRoute(route)

    suspend fun deleteRouteById(id: Int) = dao.deleteRouteById(id)

    suspend fun updateRouteFavorite(id: Int, isFavorite: Boolean) = dao.updateRouteFavorite(id, isFavorite)
}
