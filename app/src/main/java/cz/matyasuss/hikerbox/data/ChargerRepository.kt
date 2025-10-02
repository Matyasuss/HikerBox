package cz.matyasuss.hikerbox.data

import cz.matyasuss.hikerbox.model.Charger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

object ChargerRepository {
    private const val CHARGERS_URL = "https://data.matyasuss.cz/hikerbox/chargers.json"

    suspend fun loadChargers(): List<Charger> = withContext(Dispatchers.IO) {
        val json = URL(CHARGERS_URL).readText()
        val jsonArray = JSONArray(json)
        val chargers = mutableListOf<Charger>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            chargers.add(
                Charger(
                    id = obj.getString("id"),
                    nazev = obj.getString("nazev"),
                    latitude = obj.getString("latitude").toDouble(),
                    longitude = obj.getString("longitude").toDouble(),
                    typ_spec = obj.getString("typ_spec"),
                    popis = obj.getString("popis"),
                    link = obj.getString("link"),
                    note_color = if (obj.isNull("note_color")) null else obj.getString("note_color"),
                    note = if (obj.isNull("note")) null else obj.getString("note")
                )
            )
        }
        chargers
    }
}