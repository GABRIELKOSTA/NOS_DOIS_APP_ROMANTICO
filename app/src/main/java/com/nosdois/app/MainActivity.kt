package com.nosdois.app

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nosdois.app.ui.HomeScreen
import com.nosdois.app.ui.LikesScreen
import com.nosdois.app.ui.MusicScreen
import com.nosdois.app.ui.NosDoisTheme
import com.nosdois.app.ui.SettingsScreen
import com.nosdois.app.ui.isDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        Reminders.createChannel(this)
        Reminders.catchUp(this)
        Reminders.schedule(this)
        CoupleWidget.updateAll(this)
        CoupleWidget.scheduleMidnight(this)
        setContent { App() }
    }
}

private data class Tab(val label: String, val selected: ImageVector, val idle: ImageVector)

private val tabs = listOf(
    Tab("Nós", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    Tab("Coisinhas", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    Tab("Música", Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
    Tab("Ajustes", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
private fun App() {
    val ctx = LocalContext.current
    val state = remember { mutableStateOf(Repo.load(ctx)) }
    val dark = isDarkTheme(state.value.themeMode)
    NosDoisTheme(dark) { AppContent(state, dark) }
}

@Composable
private fun AppContent(state: MutableState<CoupleData>, dark: Boolean) {
    val ctx = LocalContext.current
    var data by state
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(if (data.isSetUp) 0 else 3) }
    var canNotify by remember { mutableStateOf(Reminders.canNotify(ctx)) }

    val update: (CoupleData) -> Unit = { new ->
        val reschedule = new.notifyHour != data.notifyHour
        val newSound = new.soundKey != data.soundKey
        data = new
        Repo.save(ctx, new)
        if (reschedule) Reminders.schedule(ctx)
        if (newSound) Reminders.createChannel(ctx)
        CoupleWidget.updateAll(ctx)
    }

    LaunchedEffect(data.photoVersion) {
        photo = withContext(Dispatchers.IO) { PhotoStore.load(ctx) }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = Reminders.canNotify(ctx)
    }
    val askPermission = {
        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(Unit) { if (!canNotify) askPermission() }

    // Ícones da barra de status: claros sobre o degradê da tela inicial e no modo escuro
    val view = LocalView.current
    SideEffect {
        val window = (ctx as ComponentActivity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark && tab != 0
            isAppearanceLightNavigationBars = !dark
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(if (tab == i) t.selected else t.idle, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(data, photo, onOpenSettings = { tab = 3 })
                1 -> LikesScreen(data, update)
                2 -> MusicScreen(data, photo, update)
                else -> SettingsScreen(
                    data = data,
                    photo = photo,
                    canNotify = canNotify,
                    onUpdate = update,
                    onAskPermission = askPermission,
                    onRefreshPermission = { canNotify = Reminders.canNotify(ctx) },
                    onDone = { tab = 0 },
                )
            }
        }
    }
}
