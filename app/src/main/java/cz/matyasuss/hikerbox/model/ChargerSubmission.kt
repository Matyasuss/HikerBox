package cz.matyasuss.hikerbox.model

data class ChargerSubmission(
    val id: String = "",
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val typeSpec: String,
    val description: String,
    val link: String,
    val submittedBy: String, // User email
    val submittedAt: Long = System.currentTimeMillis(),
    val status: SubmissionStatus = SubmissionStatus.PENDING
)

enum class SubmissionStatus {
    PENDING,
    APPROVED,
    REJECTED
}