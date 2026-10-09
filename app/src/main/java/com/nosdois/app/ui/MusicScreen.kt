package com.nosdois.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosdois.app.CoupleData
import com.nosdois.app.Song

/** Qual música o diálogo está editando. */
private sealed interface Editing {
    data object Favorite : Editing
    data class Playlist(val song: Song?) : Editing
}

@Composable
fun MusicScreen(data: CoupleData, photo: Bitmap?, onUpdate: (CoupleData) -> Unit) {
    val ctx = LocalContext.current
    var editing by remember { mutableStateOf<Editing?>(null) }
    var toDelete by remember { mutableStateOf<Song?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().background(love.page).statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle("Nossa música", "A trilha sonora de vocês 🎶") }

        item {
            NowPlayingCard(
                song = data.favoriteSong,
                photo = photo,
                onPlay = { data.favoriteSong?.let { openSong(ctx, it) } },
                onEdit = { editing = Editing.Favorite },
            )
        }

        item {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Nossa playlist", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Outras músicas que marcaram vocês",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { editing = Editing.Playlist(null) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Adicionar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        if (data.playlist.isEmpty()) {
            item { EmptyState("🎧", "Playlist vazia", "Adicionem as músicas que contam a história de vocês") }
        } else {
            item {
                SectionCard {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        data.playlist.forEachIndexed { i, song ->
                            PlaylistRow(
                                index = i,
                                song = song,
                                onPlay = { openSong(ctx, song) },
                                onEdit = { editing = Editing.Playlist(song) },
                                onDelete = { toDelete = song },
                            )
                        }
                    }
                }
            }
        }
    }

    when (val e = editing) {
        null -> {}
        Editing.Favorite -> SongDialog(
            title = "Música do casal",
            initial = data.favoriteSong,
            onSave = { onUpdate(data.copy(favoriteSong = it)) },
            onRemove = data.favoriteSong?.let { { onUpdate(data.copy(favoriteSong = null)) } },
            onDismiss = { editing = null },
        )
        is Editing.Playlist -> SongDialog(
            title = if (e.song == null) "Nova música" else "Editar música",
            initial = e.song,
            onSave = { s ->
                val list = if (e.song == null) data.playlist + s
                else data.playlist.map { if (it.id == s.id) s else it }
                onUpdate(data.copy(playlist = list))
            },
            onRemove = null,
            onDismiss = { editing = null },
        )
    }

    toDelete?.let { song ->
        ConfirmDialog(
            title = "Remover música?",
            text = "Remover \"${song.title}\" da playlist?",
            confirm = "Remover",
            onConfirm = { onUpdate(data.copy(playlist = data.playlist.filterNot { it.id == song.id })) },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun NowPlayingCard(song: Song?, photo: Bitmap?, onPlay: () -> Unit, onEdit: () -> Unit) {
    val t = rememberInfiniteTransition(label = "vinil")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "giro")

    Box(
        Modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(32.dp), spotColor = love.shadow.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(32.dp))
            .background(love.love),
    ) {
        FloatingHearts(Modifier.matchParentSize(), Color.White.copy(alpha = 0.14f))
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Disco de vinil girando com a foto do casal no centro
            Box(Modifier.size(200.dp).rotate(angle), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(200.dp).shadow(20.dp, CircleShape)) { vinylGrooves(Color.White.copy(alpha = 0.07f)) }
                Box(
                    Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (photo != null) {
                        Image(photo.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.size(84.dp))
                    } else {
                        Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    }
                }
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF1C1116)))
            }

            Spacer(Modifier.height(20.dp))
            if (song == null) {
                Text("Qual é a música de vocês?", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Aquela que toca e faz lembrar um do outro",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                PillButton("Escolher nossa música", filled = true, onClick = onEdit)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Equalizer()
                    Spacer(Modifier.width(8.dp))
                    Eyebrow("Música do casal", Color.White.copy(alpha = 0.85f))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    song.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                if (song.artist.isNotBlank()) {
                    Text(song.artist, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.titleMedium)
                }
                if (song.note.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "“${song.note}”",
                        color = Color.White,
                        fontFamily = Script,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    PillButton("Trocar", filled = false, onClick = onEdit)
                    Box(
                        Modifier
                            .size(64.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(onClick = onPlay),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.PlayArrow, "Ouvir", tint = Color(0xFFB0285A), modifier = Modifier.size(36.dp)) }
                    PillButton("Ouvir", filled = false, onClick = onPlay)
                }
            }
        }
    }
}

@Composable
private fun PillButton(text: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) Color.White else Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (filled) Color(0xFFB0285A) else Color.White,
        )
    }
}

/** Barrinhas de equalizador dançando. */
@Composable
private fun Equalizer() {
    val t = rememberInfiniteTransition(label = "eq")
    val bars = listOf(380, 520, 300, 450).map { ms ->
        t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(ms), RepeatMode.Reverse), label = "barra")
    }
    Row(Modifier.height(14.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        bars.forEach { h ->
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight(h.value)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White),
            )
        }
    }
}

@Composable
private fun PlaylistRow(index: Int, song: Song, onPlay: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val pastel = love.pastels[index % love.pastels.size]
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(pastel.bg),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.MusicNote, null, tint = pastel.fg) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = listOf(song.artist, song.note).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onPlay) { Icon(Icons.Filled.PlayArrow, "Ouvir", tint = MaterialTheme.colorScheme.primary) }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, "Opções", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Editar") }, onClick = { menu = false; onEdit() })
                DropdownMenuItem(text = { Text("Remover") }, onClick = { menu = false; onDelete() })
            }
        }
    }
}

@Composable
private fun SongDialog(
    title: String,
    initial: Song?,
    onSave: (Song) -> Unit,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.title ?: "") }
    var artist by remember { mutableStateOf(initial?.artist ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var link by remember { mutableStateOf(initial?.link ?: "") }
    val words = KeyboardOptions(capitalization = KeyboardCapitalization.Words)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nome da música *") }, singleLine = true, keyboardOptions = words)
                OutlinedTextField(artist, { artist = it }, label = { Text("Cantor / banda") }, singleLine = true, keyboardOptions = words)
                OutlinedTextField(
                    note, { note = it }, label = { Text("Por que é especial?") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                OutlinedTextField(
                    link, { link = it }, label = { Text("Link (Spotify, YouTube…)") },
                    supportingText = { Text("Opcional. Sem link, o app procura no YouTube.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                if (onRemove != null) {
                    TextButton(onClick = { onRemove(); onDismiss() }) { Text("Remover música do casal") }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        Song(
                            id = initial?.id ?: System.currentTimeMillis(),
                            title = name.trim(), artist = artist.trim(), note = note.trim(), link = link.trim(),
                        ),
                    )
                    onDismiss()
                },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun openSong(ctx: Context, song: Song) {
    val url = when {
        song.link.isBlank() ->
            "https://www.youtube.com/results?search_query=" + Uri.encode("${song.title} ${song.artist}".trim())
        song.link.startsWith("http://") || song.link.startsWith("https://") -> song.link
        else -> "https://${song.link}"
    }
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(ctx, "Nenhum app encontrado para abrir a música", Toast.LENGTH_SHORT).show()
    }
}
