package cz.matyasuss.hikerbox.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cz.matyasuss.hikerbox.data.ChargerRepository
import cz.matyasuss.hikerbox.model.Charger
import cz.matyasuss.hikerbox.ui.map.OSMMapView
import kotlinx.coroutines.launch

@Composable
fun MapScreen(onChargerClick: (String) -> Unit = {}) {
    var chargers by remember { mutableStateOf<List<Charger>>(emptyList()) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Load data on first composition
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val data = ChargerRepository.loadChargers()
                chargers = data
                isLoading = false
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading = false
            }
        }
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
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Map view as background
            OSMMapView(
                chargers = filteredChargers,
                onChargerClick = onChargerClick,
                modifier = Modifier.fillMaxSize()
            )

            // Filter chips on top of map with proper elevation
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f),
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