package com.nosdois.app.ui

import android.app.Activity
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import com.nosdois.app.Sounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Diálogo para escolher o som das notificações, com prévia de cada opção. */
@Composable
fun SoundPickerDialog(
    currentKey: String,
    currentName: String,
    onSave: (Sounds.Option) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val fixed = listOf(Sounds.defaultOption) + Sounds.builtIn + Sounds.silentOption
    // Toque do celular ou áudio importado que já estava escolhido
    val saved = if (fixed.none { it.key == currentKey }) Sounds.Option(currentKey, currentName, "🎵") else null
    var custom by remember { mutableStateOf(saved) }
    var selected by remember { mutableStateOf(fixed.firstOrNull { it.key == currentKey } ?: saved ?: Sounds.defaultOption) }
    var importing by remember { mutableStateOf(false) }

    var playing by remember { mutableStateOf<Ringtone?>(null) }
    fun preview(option: Sounds.Option) {
        playing?.stop()
        val uri = Sounds.uri(ctx, option.key) ?: return
        playing = runCatching {
            RingtoneManager.getRingtone(ctx, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                play()
            }
        }.getOrNull()
    }
    DisposableEffect(Unit) { onDispose { playing?.stop() } }

    fun choose(option: Sounds.Option) {
        selected = option
        preview(option)
    }

    // Seletor de toques do próprio Android
    val systemPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = res.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
        val option = when (uri) {
            null -> Sounds.silentOption
            Settings.System.DEFAULT_NOTIFICATION_URI -> Sounds.defaultOption
            else -> Sounds.Option(uri.toString(), Sounds.title(ctx, uri), "🎵").also { custom = it }
        }
        choose(option)
    }

    // Música ou áudio do celular (Android 10+)
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@rememberLauncherForActivityResult
        importing = true
        scope.launch {
            val option = withContext(Dispatchers.IO) { Sounds.importAudio(ctx, uri) }
            importing = false
            if (option == null) {
                Toast.makeText(ctx, "Não foi possível usar esse áudio", Toast.LENGTH_SHORT).show()
            } else {
                custom = option
                choose(option)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Som da notificação") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Toque numa opção para ouvir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                SoundRow(Sounds.defaultOption, selected.key == Sounds.DEFAULT, ::choose)

                SectionLabel("Exclusivos do Nós Dois")
                Sounds.builtIn.forEach { SoundRow(it, selected.key == it.key, ::choose) }

                custom?.let {
                    SectionLabel("Seu toque")
                    SoundRow(it, selected.key == it.key, ::choose)
                }

                SoundRow(Sounds.silentOption, selected.key == Sounds.SILENT, ::choose)

                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                ActionRow(Icons.Filled.LibraryMusic, "Escolher um toque do celular") {
                    playing?.stop()
                    systemPicker.launch(
                        Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Som da notificação")
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Sounds.uri(ctx, selected.key)),
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ActionRow(
                        Icons.Filled.FolderOpen,
                        if (importing) "Preparando áudio…" else "Usar uma música ou áudio do celular",
                        hint = "Prefira áudios curtos, de até uns 10 segundos",
                    ) {
                        playing?.stop()
                        filePicker.launch(arrayOf("audio/*"))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !importing, onClick = { onSave(selected); onDismiss() }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun SoundRow(option: Sounds.Option, selected: Boolean, onClick: (Sounds.Option) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick(option) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = { onClick(option) })
        Text(option.emoji, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            option.name,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (option.key != Sounds.SILENT) {
            IconButton(onClick = { onClick(option) }) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, "Ouvir", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, hint: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
