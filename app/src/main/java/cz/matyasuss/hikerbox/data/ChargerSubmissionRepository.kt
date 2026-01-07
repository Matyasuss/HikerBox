package cz.matyasuss.hikerbox.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import cz.matyasuss.hikerbox.model.ChargerSubmission
import cz.matyasuss.hikerbox.model.SubmissionStatus
import kotlinx.coroutines.tasks.await

private const val TAG = "ChargerSubmissionRepo"

class ChargerSubmissionRepository {
    private val db = FirebaseFirestore.getInstance()
    private val submissionsCollection = db.collection("charger_submissions")

    suspend fun submitCharger(submission: ChargerSubmission): Result<String> {
        return try {
            Log.d(TAG, "Submitting new charger: ${submission.name}")
            val docRef = submissionsCollection.add(submission).await()
            Log.d(TAG, "Charger submitted successfully with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting charger", e)
            Result.failure(e)
        }
    }

    suspend fun getUserSubmissions(userEmail: String): Result<List<ChargerSubmission>> {
        return try {
            Log.d(TAG, "Fetching submissions for user: $userEmail")
            val snapshot = submissionsCollection
                .whereEqualTo("submittedBy", userEmail)
                .orderBy("submittedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val submissions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ChargerSubmission::class.java)?.copy(id = doc.id)
            }
            Log.d(TAG, "Found ${submissions.size} submissions for user")
            Result.success(submissions)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user submissions", e)
            Result.failure(e)
        }
    }

    suspend fun getAllPendingSubmissions(): Result<List<ChargerSubmission>> {
        return try {
            Log.d(TAG, "Fetching all pending submissions")
            val snapshot = submissionsCollection
                .whereEqualTo("status", SubmissionStatus.PENDING.name)
                .orderBy("submittedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val submissions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ChargerSubmission::class.java)?.copy(id = doc.id)
            }
            Log.d(TAG, "Found ${submissions.size} pending submissions")
            Result.success(submissions)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching pending submissions", e)
            Result.failure(e)
        }
    }

    suspend fun updateSubmissionStatus(
        submissionId: String,
        status: SubmissionStatus
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Updating submission $submissionId to status: $status")
            submissionsCollection.document(submissionId)
                .update("status", status.name)
                .await()
            Log.d(TAG, "Submission status updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating submission status", e)
            Result.failure(e)
        }
    }
}