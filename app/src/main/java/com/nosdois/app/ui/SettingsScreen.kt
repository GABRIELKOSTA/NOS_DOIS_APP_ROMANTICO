package com.nosdois.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosdois.app.CoupleData
import com.nosdois.app.Dates
import com.nosdois.app.PhotoStore
import com.nosdois.app.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class DateField { MET, DATING }

@Composable
fun SettingsScreen(
    data: CoupleData,
    photo: Bitmap?,
    canNotify: Boolean,
    onUpdate: (CoupleData) -> Unit,
    onAskPermission: () -> Unit,
    onRefreshPermission: () -> Unit,
    onDone: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf<DateField?>(null) }
    var savingPhoto by remember { mutableStateOf(false) }
    var pickingSound by remember { mutableStateOf(false) }
    val pastels = love.pastels

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        savingPhoto = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) { PhotoStore.saveFromUri(ctx, uri) }
            savingPhoto = false
            if (ok) onUpdate(data.copy(photoVersion = System.currentTimeMillis()))
            else Toast.makeText(ctx, "Não foi possível abrir essa foto", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(love.page)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!data.isSetUp) {
            ScreenTitle("Bem-vindos! 💕", "Vamos montar o cantinho de vocês. Comecem pela foto, nomes e datas.")
        } else {
            ScreenTitle("Ajustes", "O casal, o widget e os lembretes")
        }

        // ---------- Cartão de perfil ----------
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(18.dp, RoundedCornerShape(32.dp), spotColor = love.shadow.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(32.dp))
                .background(love.love),
        ) {
            FloatingHearts(Modifier.matchParentSize(), Color.White.copy(alpha = 0.15f))
            Column(
                Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.clickable {
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                ) {
                    CouplePhoto(photo, 120.dp)
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(38.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.PhotoCamera, "Trocar foto", tint = Color(0xFFB0285A), modifier = Modifier.size(20.dp)) }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    listOf(data.name1, data.name2).filter { it.isNotBlank() }.joinToString(" & ") { it.trim() }
                        .ifEmpty { "Nós Dois" },
                    fontFamily = Script,
                    fontSize = 32.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    if (savingPhoto) "Salvando foto…" else "Toque na foto para trocar",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }

        // ---------- O casal ----------
        SettingsSection("💑", pastels[0], "O casal", "Nomes e datas importantes") {
            val words = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            OutlinedTextField(
                value = data.name1, onValueChange = { onUpdate(data.copy(name1 = it)) },
                label = { Text("Seu nome") }, singleLine = true, keyboardOptions = words,
                shape = RoundedCornerShape(18.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = data.name2, onValueChange = { onUpdate(data.copy(name2 = it)) },
                label = { Text("Nome do seu amor") }, singleLine = true, keyboardOptions = words,
                shape = RoundedCornerShape(18.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            DateRow("✨", pastels[1], "Quando se conheceram", data.metDate) { picking = DateField.MET }
            DateRow("💖", pastels[0], "Quando começaram a namorar", data.datingDate) { picking = DateField.DATING }
        }

        // ---------- Nunca esquecer ----------
        SettingsSection("📝", pastels[5], "Nunca esquecer:", "Aparece no widget e na tela inicial") {
            OutlinedTextField(
                value = data.neverForget,
                onValueChange = { onUpdate(data.copy(neverForget = it.take(200))) },
                placeholder = { Text("Ex: Você é a minha pessoa favorita no mundo.") },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                supportingText = { Text("${data.neverForget.length}/200") },
                shape = RoundedCornerShape(18.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("📱", fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Para colocar o widget: segure o dedo num espaço vazio da tela inicial → Widgets → Nós Dois.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---------- Aparência ----------
        SettingsSection("🎨", pastels[1], "Aparência", "Automático segue o modo escuro do celular") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(4.dp),
            ) {
                listOf("📱 Auto", "☀️ Claro", "🌙 Escuro").forEachIndexed { i, label ->
                    val selected = data.themeMode == i
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .then(if (selected) Modifier.background(love.love) else Modifier)
                            .clickable { onUpdate(data.copy(themeMode = i)) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // ---------- Lembretes ----------
        SettingsSection("🔔", pastels[2], "Lembretes", "O app avisa nas datas especiais, mesmo fechado") {
            if (!canNotify) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(14.dp),
                ) {
                    Text("As notificações estão desligadas", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Permita para receber os lembretes.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (Build.VERSION.SDK_INT >= 33) {
                            Button(onClick = onAskPermission) { Text("Permitir") }
                        }
                        OutlinedButton(onClick = {
                            ctx.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName),
                            )
                        }) { Text("Configurações") }
                    }
                    TextButton(onClick = onRefreshPermission) { Text("Já permiti, verificar de novo") }
                }
                Spacer(Modifier.height(6.dp))
            }

            ToggleRow("Aniversário de namoro", "Todo ano, no dia em que começaram", data.notifyDatingYearly) {
                onUpdate(data.copy(notifyDatingYearly = it))
            }
            ToggleRow("Mesversário", "Todo mês, no mesmo dia", data.notifyMonthly) {
                onUpdate(data.copy(notifyMonthly = it))
            }
            ToggleRow("Dia em que se conheceram", "Todo ano", data.notifyMet) {
                onUpdate(data.copy(notifyMet = it))
            }
            ToggleRow("Marcos de dias", "A cada 100 dias de namoro", data.notifyMilestones) {
                onUpdate(data.copy(notifyMilestones = it))
            }
            ToggleRow("Aviso na véspera", "Um dia antes dos aniversários", data.notifyDayBefore) {
                onUpdate(data.copy(notifyDayBefore = it))
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Horário
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Horário do lembrete", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Quando a notificação aparece",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerLow),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StepButton(Icons.Filled.Remove, "Mais cedo") { onUpdate(data.copy(notifyHour = (data.notifyHour + 23) % 24)) }
                    Text(
                        "%02d:00".format(data.notifyHour),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    StepButton(Icons.Filled.Add, "Mais tarde") { onUpdate(data.copy(notifyHour = (data.notifyHour + 1) % 24)) }
                }
            }

            // Som
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { pickingSound = true }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Som da notificação", style = MaterialTheme.typography.titleSmall)
                    Text(
                        data.soundName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    "Escolher som",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        if (!Reminders.canNotify(ctx)) {
                            Toast.makeText(ctx, "Permita as notificações primeiro", Toast.LENGTH_SHORT).show()
                        } else {
                            Reminders.show(ctx, 999, "Lembretes funcionando! 💕", "É assim que ${data.coupleName} vão ser avisados nas datas especiais.")
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("🔔  Testar notificação", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        if (data.isSetUp) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(12.dp, RoundedCornerShape(50), spotColor = love.shadow.copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(50))
                    .background(love.love)
                    .clickable(onClick = onDone),
                contentAlignment = Alignment.Center,
            ) {
                Text("Ver nosso contador 💖", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
        Text(
            "Nós Dois · feito com amor",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
    }

    if (pickingSound) {
        SoundPickerDialog(
            currentKey = data.soundKey,
            currentName = data.soundName,
            onSave = { option ->
                if (option.key != data.soundKey) {
                    onUpdate(data.copy(soundKey = option.key, soundName = option.name))
                    Toast.makeText(ctx, "Som alterado! Toque em \"Testar notificação\" para ouvir.", Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { pickingSound = false },
        )
    }

    picking?.let { field ->
        DatePick(
            initial = if (field == DateField.MET) data.metDate else data.datingDate,
            onPick = { d ->
                onUpdate(if (field == DateField.MET) data.copy(metDate = d) else data.copy(datingDate = d))
            },
            onDismiss = { picking = null },
        )
    }
}

// ---------- Peças da tela ----------

@Composable
private fun SettingsSection(
    emoji: String,
    pastel: Pastel,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    SectionCard {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(emoji, pastel, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
)

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun DateRow(emoji: String, pastel: Pastel, label: String, date: LocalDate?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiBadge(emoji, pastel, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                date?.let { Dates.long(it) } ?: "Toque para escolher",
                color = if (date == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePick(initial: LocalDate?, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) {
        DatePicker(state = state)
    }
}
