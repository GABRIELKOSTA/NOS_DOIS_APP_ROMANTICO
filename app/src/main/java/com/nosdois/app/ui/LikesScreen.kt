package com.nosdois.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosdois.app.CoupleData
import com.nosdois.app.Like

private val emojis = listOf(
    "❤️", "🍕", "🍫", "🍔", "🍣", "🍷", "☕", "🎬", "🎵", "🎮",
    "📚", "✈️", "🏖️", "⛰️", "🌙", "🌹", "🐶", "🐱", "⚽", "⭐",
)

@Composable
fun LikesScreen(data: CoupleData, onUpdate: (CoupleData) -> Unit) {
    var text by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf(emojis.first()) }
    var toDelete by remember { mutableStateOf<Like?>(null) }
    val pastels = love.pastels

    fun add() {
        val t = text.trim()
        if (t.isEmpty()) return
        onUpdate(data.copy(likes = listOf(Like(System.currentTimeMillis(), emoji, t)) + data.likes))
        text = ""
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().background(love.page).statusBarsPadding().imePadding(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenTitle("Coisinhas nossas", "Comidas, lugares, filmes, manias… tudo que é de vocês 💕")
        }

        // Caixa de adicionar
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionCard {
                Column(Modifier.padding(16.dp)) {
                    Eyebrow("Nova coisinha")
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(emojis) { e ->
                            val selected = e == emoji
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                                    .border(
                                        2.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(14.dp),
                                    )
                                    .clickable { emoji = e },
                                contentAlignment = Alignment.Center,
                            ) { Text(e, fontSize = 22.sp) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = { Text("Ex: pizza de domingo") },
                            singleLine = true,
                            shape = RoundedCornerShape(50),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { add() }),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(
                            Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(love.love)
                                .clickable { add() },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Add, "Adicionar", tint = Color.White) }
                    }
                }
            }
        }

        if (data.likes.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState("💝", "Nada por aqui ainda", "Escolham um emoji, escrevam algo que vocês amam e toquem em +")
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Eyebrow(
                    "${data.likes.size} coisinha${if (data.likes.size == 1) "" else "s"} de vocês",
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        itemsIndexed(data.likes, key = { _, it -> it.id }) { i, like ->
            val pastel = pastels[i % pastels.size]
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(pastel.bg),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(60.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(like.emoji, fontSize = 30.sp) }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        like.text,
                        style = MaterialTheme.typography.titleSmall,
                        color = pastel.fg,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { toDelete = like },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, "Remover", tint = pastel.fg.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    toDelete?.let { like ->
        ConfirmDialog(
            title = "Remover?",
            text = "Remover \"${like.text}\" da lista?",
            confirm = "Remover",
            onConfirm = { onUpdate(data.copy(likes = data.likes.filterNot { it.id == like.id })) },
            onDismiss = { toDelete = null },
        )
    }
}
