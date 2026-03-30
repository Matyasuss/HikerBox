package cz.matyasuss.hikerbox.data

import android.content.Context
import android.util.Log
import java.io.File

private const val TAG = "CacheManager"

class CacheManager(private val context: Context) {
    private val cacheDir = context.cacheDir
    private val chargersFile = File(cacheDir, "chargers_cache.json")

    /**
     * Uloží JSON data do cache
     */
    fun saveChargers(jsonData: String): Boolean {
        return try {
            chargersFile.writeText(jsonData)
            Log.d(TAG, "Chargers cached successfully, size: ${jsonData.length} bytes")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving chargers to cache", e)
            false
        }
    }

    /**
     * Načte JSON data z cache
     */
    fun loadChargers(): String? {
        return try {
            if (chargersFile.exists()) {
                val data = chargersFile.readText()
                Log.d(TAG, "Loaded chargers from cache, size: ${data.length} bytes")
                data
            } else {
                Log.d(TAG, "No cached chargers found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading chargers from cache", e)
            null
        }
    }

    /**
     * Vrátí čas poslední aktualizace cache
     */
    fun getLastUpdateTime(): Long {
        return if (chargersFile.exists()) {
            chargersFile.lastModified()
        } else {
            0L
        }
    }

    /**
     * Kontroluje, zda existuje validní cache
     */
    fun hasCachedData(): Boolean {
        return chargersFile.exists() && chargersFile.length() > 0
    }

    /**
     * Vymaže cache
     */
    fun clearCache(): Boolean {
        return try {
            if (chargersFile.exists()) {
                chargersFile.delete()
                Log.d(TAG, "Cache cleared")
                true
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
            false
        }
    }
}