package cz.matyasuss.hikerbox.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cz.matyasuss.hikerbox.data.ChargerRepository
import cz.matyasuss.hikerbox.model.Charger
import cz.matyasuss.hikerbox.ui.map.OSMMapView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MapScreen(onChargerClick: (String) -> Unit = {}) {
    val context = LocalContext.current
    val repository = remember { ChargerRepository(context) }

    var chargers by remember { mutableStateOf<List<Charger>>(emptyList()) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isOfflineMode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastUpdateTime by remember { mutableStateOf<Long?>(null) }

    val scope = rememberCoroutineScope()

    // Funkce pro načtení dat
    fun loadData() {
        scope.launch {
            isLoading = true
            errorMessage = null

            val result = repository.loadChargers()

            result.onSuccess { data ->
                chargers = data
                isOfflineMode = false
                val cacheInfo = repository.getCacheInfo()
                lastUpdateTime = cacheInfo.lastUpdateTime
            }.onFailure { e ->
                // Zkusíme načíst z cache
                val cacheInfo = repository.getCacheInfo()
                if (cacheInfo.hasCachedData) {
                    isOfflineMode = true
                    lastUpdateTime = cacheInfo.lastUpdateTime
                    errorMessage = "Offline režim - používám uložená data"
                } else {
                    errorMessage = "Chyba: ${e.message}"
                }
            }

            isLoading = false
        }
    }

    // Load data on first composition
    LaunchedEffect(Unit) {
        loadData()
    }

    // Filter chargers based on selected type
    val filteredChargers = if (selectedType != null) {
        chargers.filter { it.typeSpec == selectedType }
    } else {
        chargers
    }

    // Get unique types from data
    val types = chargers.map { it.typeSpec }.distinct()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Načítám data...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Map view as background
            OSMMapView(
                chargers = filteredChargers,
                onChargerClick = onChargerClick,
                modifier = Modifier.fillMaxSize()
            )

            // Top bar with filters and status
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f)
            ) {
                // Status banner
                if (isOfflineMode || errorMessage != null) {
                    OfflineModeBanner(
                        isOfflineMode = isOfflineMode,
                        errorMessage = errorMessage,
                        lastUpdateTime = lastUpdateTime,
                        onRefresh = { loadData() }
                    )
                }

                // Filter chips
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 4.dp
                ) {
                    FilterChipsRow(
                        types = types,
                        selectedType = selectedType,
                        totalCount = chargers.size,
                        chargers = chargers,
                        onTypeSelected = { type ->
                            selectedType = if (selectedType == type) null else type
                        },
                        onAllSelected = { selectedType = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineModeBanner(
    isOfflineMode: Boolean,
    errorMessage: String?,
    lastUpdateTime: Long?,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isOfflineMode)
            MaterialTheme.colorScheme.tertiaryContainer
        else
            MaterialTheme.colorScheme.errorContainer,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ThumbUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isOfflineMode)
                        MaterialTheme.colorScheme.onTertiaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
                Column {
                    Text(
                        text = if (isOfflineMode) "Offline režim" else "Upozornění",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOfflineMode)
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                    if (lastUpdateTime != null) {
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        Text(
                            text = "Aktualizováno: ${dateFormat.format(Date(lastUpdateTime))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOfflineMode)
                                MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Obnovit",
                    tint = if (isOfflineMode)
                        MaterialTheme.colorScheme.onTertiaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    types: List<String>,
    selectedType: String?,
    totalCount: Int,
    chargers: List<Charger>,
    onTypeSelected: (String) -> Unit,
    onAllSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" filter chip
        FilterChip(
            selected = selectedType == null,
            onClick = onAllSelected,
            label = { Text("Vše ($totalCount)") }
        )

        // Type-specific filter chips
        types.forEach { type ->
            val count = chargers.count { it.typeSpec == type }
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text("$type ($count)") }
            )
        }
    }
}