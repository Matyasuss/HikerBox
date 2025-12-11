package cz.matyasuss.hikerbox.ui.screen

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cz.matyasuss.hikerbox.data.FirebaseAuthManager
import cz.matyasuss.hikerbox.data.PreferencesManager
import cz.matyasuss.hikerbox.model.LoginMethod
import kotlinx.coroutines.launch

private const val TAG = "SettingsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager? = null
) {
    val context = LocalContext.current
    val manager = preferencesManager ?: remember { PreferencesManager(context) }
    val preferences by manager.userPreferences.collectAsState()

    val authManager = remember { FirebaseAuthManager(context) }
    val scope = rememberCoroutineScope()

    var showLoginDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessingLogin by remember { mutableStateOf(false) }

    // Google Sign-In launcher - FIXED VERSION
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Google Sign-In result received: resultCode=${result.resultCode}, data=${result.data != null}")

        isProcessingLogin = true

        scope.launch {
            try {
                when (result.resultCode) {
                    Activity.RESULT_OK -> {
                        Log.d(TAG, "Processing Google Sign-In result with OK status")
                        if (result.data == null) {
                            Log.e(TAG, "Result data is null despite RESULT_OK")
                            errorMessage = "Chyba: Žádná data z Google přihlášení"
                            return@launch
                        }

                        val signInResult = authManager.handleGoogleSignInResult(result.data)

                        signInResult.onSuccess { user ->
                            Log.d(TAG, "Google Sign-In successful: ${user.email}")
                            manager.login(
                                LoginMethod.GOOGLE,
                                user.email ?: "",
                                user.displayName ?: user.email?.substringBefore("@") ?: "User"
                            )
                            errorMessage = null
                        }.onFailure { e ->
                            Log.e(TAG, "Google Sign-In failed", e)
                            errorMessage = "Google přihlášení selhalo: ${e.message}"
                        }
                    }
                    Activity.RESULT_CANCELED -> {
                        Log.d(TAG, "Google Sign-In canceled by user")
                        errorMessage = null // Don't show error for user cancellation
                    }
                    else -> {
                        Log.w(TAG, "Unexpected result code: ${result.resultCode}")
                        errorMessage = "Neočekávaný výsledek přihlášení (kód: ${result.resultCode})"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during Google Sign-In processing", e)
                errorMessage = "Chyba při zpracování přihlášení: ${e.localizedMessage ?: e.message}"
            } finally {
                isProcessingLogin = false
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Nastavení",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Processing indicator
        if (isProcessingLogin) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Zpracovávám přihlášení...",
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Error message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    IconButton(onClick = { errorMessage = null }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Zavřít",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // User Profile Section
        UserProfileCard(
            isLoggedIn = preferences.isLoggedIn,
            displayName = preferences.displayName,
            email = preferences.email,
            loginMethod = preferences.loginMethod,
            onLoginClick = { showLoginDialog = true },
            onLogoutClick = { showLogoutDialog = true }
        )

        // App Settings Section
        SettingsSectionCard(
            title = "Obecné",
            icon = Icons.Default.Settings
        ) {
            SwitchSettingItem(
                title = "Tmavý režim",
                description = "Zobrazit aplikaci v tmavém tématu",
                icon = Icons.Default.Warning,
                checked = preferences.darkMode,
                onCheckedChange = {
                    manager.updateSetting("dark_mode", it)
                }
            )

            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))

            SwitchSettingItem(
                title = "Notifikace",
                description = "Povolit oznámení z aplikace",
                icon = Icons.Default.Notifications,
                checked = preferences.notificationsEnabled,
                onCheckedChange = {
                    manager.updateSetting("notifications_enabled", it)
                }
            )
        }

        // Map Settings Section
        SettingsSectionCard(
            title = "Mapa",
            icon = Icons.Default.Place
        ) {
            SwitchSettingItem(
                title = "Zobrazit všechny stanice",
                description = "Zobrazit zamčené i odemčené stanice",
                icon = Icons.Default.Face,
                checked = preferences.showAllChargers,
                onCheckedChange = {
                    manager.updateSetting("show_all_chargers", it)
                }
            )
        }

        // About Section
        SettingsSectionCard(
            title = "O aplikaci",
            icon = Icons.Default.Info
        ) {
            InfoItem(
                label = "Verze",
                value = "0.1.1"
            )
            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            InfoItem(
                label = "Vývojář",
                value = "Matyasuss"
            )
        }
    }

    // Login Dialog
    if (showLoginDialog) {
        LoginDialog(
            onDismiss = {
                showLoginDialog = false
                errorMessage = null
            },
            onEmailLogin = { email, password ->
                scope.launch {
                    try {
                        isProcessingLogin = true
                        val result = authManager.signInWithEmail(email, password)
                        result.onSuccess { user ->
                            manager.login(
                                LoginMethod.EMAIL,
                                user.email ?: email,
                                user.displayName ?: email.substringBefore("@")
                            )
                            showLoginDialog = false
                            errorMessage = null
                        }.onFailure { e ->
                            errorMessage = "Email přihlášení selhalo: ${e.message}"
                        }
                    } finally {
                        isProcessingLogin = false
                    }
                }
            },
            onGoogleLogin = {
                try {
                    Log.d(TAG, "Initiating Google Sign-In")
                    val signInIntent = authManager.getGoogleSignInIntent()
                    googleSignInLauncher.launch(signInIntent)
                    showLoginDialog = false
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching Google Sign-In", e)
                    errorMessage = "Chyba při spuštění Google přihlášení: ${e.message}"
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Odhlásit se") },
            text = { Text("Opravdu se chcete odhlásit?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        authManager.signOut()
                        manager.logout()
                        showLogoutDialog = false
                    }
                ) {
                    Text("Odhlásit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }
}

@Composable
private fun UserProfileCard(
    isLoggedIn: Boolean,
    displayName: String?,
    email: String?,
    loginMethod: LoginMethod,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoggedIn) {
                        Text(
                            text = displayName ?: "Uživatel",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = when (loginMethod) {
                                LoginMethod.EMAIL -> "Přihlášen přes Email"
                                LoginMethod.GOOGLE -> "Přihlášen přes Google"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                        )
                    } else {
                        Text(
                            text = "Nepřihlášen",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Přihlaste se pro plný přístup",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Login/Logout Button
            Button(
                onClick = if (isLoggedIn) onLogoutClick else onLoginClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoggedIn)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isLoggedIn) Icons.Default.Close else Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isLoggedIn) "Odhlásit se" else "Přihlásit se")
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            content()
        }
    }
}

@Composable
private fun SwitchSettingItem(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoginDialog(
    onDismiss: () -> Unit,
    onEmailLogin: (String, String) -> Unit,
    onGoogleLogin: () -> Unit
) {
    var selectedMethod by remember { mutableStateOf<LoginMethod?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Přihlášení",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedVisibility(
                    visible = selectedMethod == null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Vyberte způsob přihlášení:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Email Login Option
                        LoginMethodCard(
                            icon = Icons.Default.Email,
                            title = "Email",
                            description = "Přihlásit se emailem a heslem",
                            onClick = { selectedMethod = LoginMethod.EMAIL }
                        )

                        // Google Login Option
                        LoginMethodCard(
                            icon = Icons.Default.AccountCircle,
                            title = "Google",
                            description = "Přihlásit se účtem Google",
                            onClick = onGoogleLogin
                        )
                    }
                }

                AnimatedVisibility(
                    visible = selectedMethod == LoginMethod.EMAIL,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                showError = false
                            },
                            label = { Text("Email") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null)
                            },
                            singleLine = true,
                            isError = showError,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                showError = false
                            },
                            label = { Text("Heslo") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible)
                                            Icons.Default.Favorite
                                        else
                                            Icons.Default.FavoriteBorder,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            singleLine = true,
                            isError = showError,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (showError) {
                            Text(
                                text = "Prosím vyplňte email a heslo",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        TextButton(
                            onClick = { selectedMethod = null },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Zpět")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedMethod == LoginMethod.EMAIL) {
                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            onEmailLogin(email, password)
                        } else {
                            showError = true
                        }
                    }
                ) {
                    Text("Přihlásit se")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}

@Composable
private fun LoginMethodCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.Default.MailOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}