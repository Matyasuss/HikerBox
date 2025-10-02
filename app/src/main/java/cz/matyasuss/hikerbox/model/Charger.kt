package cz.matyasuss.hikerbox.model

data class Charger(
    val id: String,
    val nazev: String,
    val latitude: Double,
    val longitude: Double,
    val typ_spec: String,
    val popis: String,
    val link: String,
    val note_color: String?,
    val note: String?
)