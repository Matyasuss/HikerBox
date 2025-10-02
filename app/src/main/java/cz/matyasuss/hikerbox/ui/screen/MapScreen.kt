package cz.matyasuss.hikerbox.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.matyasuss.hikerbox.data.ChargerRepository
import cz.matyasuss.hikerbox.model.Charger
import cz.matyasuss.hikerbox.ui.map.OSMMapView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
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
        chargers.filter { it.typ_spec == selectedType }
    } else {
        chargers
    }

    // Get unique types from data
    val types = chargers.map { it.typ_spec }.distinct()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hikerbox") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Filter chips row
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

                // Map view
                OSMMapView(
                    chargers = filteredChargers,
                    modifier = Modifier.fillMaxSize()
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
            .padding(8.dp),
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
            val count = chargers.count { it.typ_spec == type }
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text("$type ($count)") }
            )
        }
    }
}