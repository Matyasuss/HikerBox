package cz.matyasuss.hikerbox.data

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import cz.matyasuss.hikerbox.R

private const val TAG = "FirebaseAuthManager"

class FirebaseAuthManager(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var googleSignInClient: GoogleSignInClient? = null

    init {
        try {
            // Configure Google Sign-In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(context, gso)
            Log.d(TAG, "Google Sign-In client initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Google Sign-In client", e)
        }
    }

    // Get current user
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    // Check if user is logged in
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    // Email/Password Sign In
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Attempting email sign-in for: $email")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                Log.d(TAG, "Email sign-in successful: ${it.email}")
                Result.success(it)
            } ?: run {
                Log.e(TAG, "Email sign-in failed: No user returned")
                Result.failure(Exception("Sign in failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-in exception", e)
            Result.failure(e)
        }
    }

    // Email/Password Sign Up
    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Attempting email sign-up for: $email")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                Log.d(TAG, "Email sign-up successful: ${it.email}")
                Result.success(it)
            } ?: run {
                Log.e(TAG, "Email sign-up failed: No user returned")
                Result.failure(Exception("Sign up failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-up exception", e)
            Result.failure(e)
        }
    }

    // Get Google Sign-In Intent
    fun getGoogleSignInIntent(): Intent {
        if (googleSignInClient == null) {
            Log.e(TAG, "Google Sign-In client is null")
            throw IllegalStateException("Google Sign-In client not initialized")
        }

        // Sign out first to force account picker
        googleSignInClient?.signOut()

        val intent = googleSignInClient!!.signInIntent
        Log.d(TAG, "Google Sign-In intent created")
        return intent
    }

    // Handle Google Sign-In Result
    suspend fun handleGoogleSignInResult(data: Intent?): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Handling Google Sign-In result")

            if (data == null) {
                Log.e(TAG, "Google Sign-In data is null")
                return Result.failure(Exception("No data received from Google Sign-In"))
            }

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            Log.d(TAG, "Got task from intent, checking result...")

            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Got Google account: ${account.email}, idToken present: ${account.idToken != null}")

                if (account.idToken == null) {
                    Log.e(TAG, "Google account idToken is null")
                    return Result.failure(Exception("Failed to get ID token from Google"))
                }

                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.e(TAG, "ApiException during Google Sign-In: statusCode=${e.statusCode}, message=${e.message}", e)
                Result.failure(Exception("Google Sign-In failed: ${e.statusCode} - ${e.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception in handleGoogleSignInResult", e)
            Result.failure(e)
        }
    }

    // Authenticate with Firebase using Google account
    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Authenticating with Firebase using Google account")
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            Log.d(TAG, "Created Google credential, signing in to Firebase...")

            val result = auth.signInWithCredential(credential).await()
            Log.d(TAG, "Firebase sign-in completed")

            result.user?.let {
                Log.d(TAG, "Firebase authentication successful: ${it.email}")
                Result.success(it)
            } ?: run {
                Log.e(TAG, "Firebase authentication failed: No user returned")
                Result.failure(Exception("Firebase authentication failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Firebase authentication with Google", e)
            Result.failure(e)
        }
    }

    // Sign Out
    fun signOut() {
        try {
            Log.d(TAG, "Signing out")
            auth.signOut()
            googleSignInClient?.signOut()
            Log.d(TAG, "Sign out successful")
        } catch (e: Exception) {
            Log.e(TAG, "Error during sign out", e)
        }
    }

    // Get user info
    fun getUserInfo(): Triple<String?, String?, String?> {
        val user = auth.currentUser
        return Triple(
            user?.displayName,
            user?.email,
            user?.photoUrl?.toString()
        )
    }
}