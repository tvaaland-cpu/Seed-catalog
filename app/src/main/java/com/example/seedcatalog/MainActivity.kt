package com.example.seedcatalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.seedcatalog.ui.theme.SeedCatalogTheme

data class Seed(
    val id: Int,
    val name: String,
    val type: String,
    val notes: String
)

sealed class Screen(val route: String) {
    data object SeedList : Screen("seedList")
    data object SeedDetail : Screen("seedDetail/{seedId}") {
        fun createRoute(seedId: Int) = "seedDetail/$seedId"
    }

    data object AddEditSeed : Screen("addEditSeed")
    data object Settings : Screen("settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SeedCatalogTheme {
                SeedCatalogApp()
            }
        }
    }
}

@Composable
private fun SeedCatalogApp() {
    val navController = rememberNavController()
    var seeds by remember {
        mutableStateOf(
            listOf(
                Seed(1, "Tomato", "Vegetable", "Needs warm soil."),
                Seed(2, "Sunflower", "Flower", "Plant in full sun."),
                Seed(3, "Basil", "Herb", "Keep soil moist.")
            )
        )
    }

    NavHost(navController = navController, startDestination = Screen.SeedList.route) {
        composable(Screen.SeedList.route) {
            SeedListScreen(
                seeds = seeds,
                onSeedClick = { navController.navigate(Screen.SeedDetail.createRoute(it)) },
                onAddSeedClick = { navController.navigate(Screen.AddEditSeed.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(
            route = Screen.SeedDetail.route,
            arguments = listOf(navArgument("seedId") { type = NavType.IntType })
        ) { backStackEntry ->
            val seedId = backStackEntry.arguments?.getInt("seedId") ?: -1
            val seed = seeds.find { it.id == seedId }
            SeedDetailScreen(
                seed = seed,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddEditSeed.route) {
            AddEditSeedScreen(
                onSave = { name, type, notes ->
                    val nextId = (seeds.maxOfOrNull { it.id } ?: 0) + 1
                    seeds = seeds + Seed(nextId, name, type, notes)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeedListScreen(
    seeds: List<Seed>,
    onSeedClick: (Int) -> Unit,
    onAddSeedClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(id = R.string.seed_list_title)) }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onAddSeedClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(id = R.string.add_seed))
            }
            Button(onClick = onSettingsClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(id = R.string.go_to_settings))
            }
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(seeds) { seed ->
                    Card(
                        onClick = { onSeedClick(seed.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(seed.name, fontWeight = FontWeight.Bold)
                            Text(seed.type)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeedDetailScreen(seed: Seed?, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(id = R.string.seed_detail_title)) }) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (seed == null) {
                Text(stringResource(id = R.string.not_found))
            } else {
                Text(text = seed.name, style = MaterialTheme.typography.headlineSmall)
                Text(text = "${stringResource(id = R.string.seed_type_label)}: ${seed.type}")
                Text(text = "${stringResource(id = R.string.seed_notes_label)}: ${seed.notes}")
            }
            Button(onClick = onBack) {
                Text(stringResource(id = R.string.back))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSeedScreen(
    onSave: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(id = R.string.add_edit_seed_title)) }) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(id = R.string.seed_name_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            androidx.compose.material3.OutlinedTextField(
                value = type,
                onValueChange = { type = it },
                label = { Text(stringResource(id = R.string.seed_type_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            androidx.compose.material3.OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(id = R.string.seed_notes_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onSave(name, type, notes) },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && type.isNotBlank()
            ) {
                Text(stringResource(id = R.string.save))
            }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(id = R.string.back))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var notificationsEnabled by rememberSaveable { mutableStateOf(true) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(id = R.string.settings_title)) }) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(id = R.string.dummy_setting))
            Text(text = stringResource(id = R.string.language))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(id = R.string.notifications))
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                    Text(
                        text = if (notificationsEnabled) {
                            stringResource(id = R.string.notifications_enabled)
                        } else {
                            stringResource(id = R.string.notifications_disabled)
                        }
                    )
                }
            }
            Button(onClick = onBack) {
                Text(stringResource(id = R.string.back))
            }
        }
    }
}
