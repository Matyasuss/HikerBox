package cz.matyasuss.hikerbox.model

data class Charger(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val typeSpec: String,
    val description: String,
    val link: String,
    val noteColor: String?,
    val note: String?
)