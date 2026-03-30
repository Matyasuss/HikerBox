package cz.matyasuss.hikerbox.data

import android.content.Context
import android.util.Log
import cz.matyasuss.hikerbox.model.Charger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

private const val TAG = "ChargerRepository"

class ChargerRepository(context: Context) {
    private val cacheManager = CacheManager(context)

    companion object {
        private const val CHARGERS_URL = "https://hikerbox.matyasuss.cz/api/chargers.json.php"
    }

    /**
     * Načte nabíječky - nejprve zkusí stáhnout z API, při selhání použije cache
     */
    suspend fun loadChargers(): Result<List<Charger>> = withContext(Dispatchers.IO) {
        try {
            // Pokus o stažení z API
            Log.d(TAG, "Attempting to download chargers from API")
            val json = URL(CHARGERS_URL).readText()

            // Uložení do cache
            cacheManager.saveChargers(json)
            Log.d(TAG, "Successfully downloaded and cached chargers")

            // Parsování a vrácení dat
            Result.success(parseChargers(json))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download chargers from API", e)

            // Pokus o načtení z cache
            val cachedData = cacheManager.loadChargers()
            if (cachedData != null) {
                Log.d(TAG, "Using cached chargers (offline mode)")
                try {
                    Result.success(parseChargers(cachedData))
                } catch (parseException: Exception) {
                    Log.e(TAG, "Failed to parse cached data", parseException)
                    Result.failure(Exception("Nepodařilo se načíst data z cache", parseException))
                }
            } else {
                Log.e(TAG, "No cached data available")
                Result.failure(Exception("Nepodařilo se načíst data a cache není k dispozici", e))
            }
        }
    }

    /**
     * Načte nabíječky pouze z cache (bez pokusu o stažení)
     */
    suspend fun loadChargersFromCache(): Result<List<Charger>> = withContext(Dispatchers.IO) {
        try {
            val cachedData = cacheManager.loadChargers()
            if (cachedData != null) {
                Result.success(parseChargers(cachedData))
            } else {
                Result.failure(Exception("Cache není k dispozici"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from cache", e)
            Result.failure(e)
        }
    }

    /**
     * Vrátí informaci o cache
     */
    fun getCacheInfo(): CacheInfo {
        return CacheInfo(
            hasCachedData = cacheManager.hasCachedData(),
            lastUpdateTime = cacheManager.getLastUpdateTime()
        )
    }

    /**
     * Vymaže cache
     */
    fun clearCache(): Boolean {
        return cacheManager.clearCache()
    }

    /**
     * Parsuje JSON string na seznam Charger objektů
     */
    private fun parseChargers(json: String): List<Charger> {
        val jsonArray = JSONArray(json)
        val chargers = mutableListOf<Charger>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            chargers.add(
                Charger(
                    id = obj.getString("id"),
                    name = obj.getString("nazev"),
                    latitude = obj.getString("latitude").toDouble(),
                    longitude = obj.getString("longitude").toDouble(),
                    typeSpec = obj.getString("typ_spec"),
                    description = obj.getString("popis"),
                    link = obj.getString("link"),
                    noteColor = if (obj.isNull("note_color")) null else obj.getString("note_color"),
                    note = if (obj.isNull("note")) null else obj.getString("note")
                )
            )
        }

        Log.d(TAG, "Parsed ${chargers.size} chargers")
        return chargers
    }
}

/**
 * Informace o stavu cache
 */
data class CacheInfo(
    val hasCachedData: Boolean,
    val lastUpdateTime: Long
)