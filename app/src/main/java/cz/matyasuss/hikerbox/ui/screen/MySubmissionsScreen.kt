package cz.matyasuss.hikerbox.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.matyasuss.hikerbox.data.ChargerSubmissionRepository
import cz.matyasuss.hikerbox.data.FirebaseAuthManager
import cz.matyasuss.hikerbox.model.ChargerSubmission
import cz.matyasuss.hikerbox.model.SubmissionStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySubmissionsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager = remember { FirebaseAuthManager(context) }
    val repository = remember { ChargerSubmissionRepository() }

    var submissions by remember { mutableStateOf<List<ChargerSubmission>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val currentUser = authManager.getCurrentUser()
        if (currentUser?.email != null) {
            scope.launch {
                repository.getUserSubmissions(currentUser.email!!)
                    .onSuccess {
                        submissions = it
                        isLoading = false
                    }
                    .onFailure { e ->
                        errorMessage = e.message
                        isLoading = false
                    }
            }
        } else {
            errorMessage = "Musíte být přihlášeni"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moje návrhy") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = errorMessage ?: "Chyba",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                submissions.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Zatím jste nepřidali žádné návrhy",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(submissions) { submission ->
                            SubmissionCard(submission)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmissionCard(submission: ChargerSubmission) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = submission.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(submission.status)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${submission.latitude}, ${submission.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = submission.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(submission.submittedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FilterChip(
                    selected = false,
                    onClick = { },
                    label = {
                        Text(
                            when (submission.typeSpec) {
                                "charge_lock" -> "Zamčená"
                                "charge_unloc" -> "Odemčená"
                                else -> submission.typeSpec
                            }
                        )
                    },
                    enabled = false
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: SubmissionStatus) {
    val (icon, text, colors) = when (status) {
        SubmissionStatus.PENDING -> Triple(
            Icons.Default.Info,
            "Čeká na schválení",
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        )
        SubmissionStatus.APPROVED -> Triple(
            Icons.Default.CheckCircle,
            "Schváleno",
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        SubmissionStatus.REJECTED -> Triple(
            Icons.Default.Clear,
            "Zamítnuto",
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                labelColor = MaterialTheme.colorScheme.onErrorContainer
            )
        )
    }

    AssistChip(
        onClick = { },
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = colors,
        enabled = false
    )
}