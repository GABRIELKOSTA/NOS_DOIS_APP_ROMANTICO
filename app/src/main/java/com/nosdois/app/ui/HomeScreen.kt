package com.nosdois.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosdois.app.CoupleData
import com.nosdois.app.Dates
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(data: CoupleData, photo: Bitmap?, onOpenSettings: () -> Unit) {
    val today = remember { LocalDate.now() }
    val specials = remember(data, today) { Dates.specialsFor(data, today) }
    val mainStart = data.datingDate ?: data.metDate

    Column(
        Modifier
            .fillMaxSize()
            .background(love.page)
            .verticalScroll(rememberScrollState()),
    ) {
        Header(data, photo, mainStart)

        // O conteúdo "sobe" um pouco por cima do cabeçalho
        Column(
            Modifier.offset(y = (-36).dp).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            data.datingDate?.let { HeroCounter(it, today) }

            specials.forEach { SpecialBanner(it.title, it.text) }

            data.metDate?.let { MetCard(it, today, isMain = data.datingDate == null) }

            if (data.neverForget.isNotBlank()) NeverForgetCard(data.neverForget.trim())

            if (mainStart != null) {
                UpcomingCard(data, today)
            } else {
                SetupCard(onOpenSettings)
            }
        }
    }
}

// ---------- Cabeçalho ----------

@Composable
private fun Header(data: CoupleData, photo: Bitmap?, since: LocalDate?) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 44.dp, bottomEnd = 44.dp))
            .background(love.love),
    ) {
        FloatingHearts(Modifier.matchParentSize(), Color.White.copy(alpha = 0.22f))
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 20.dp, bottom = 64.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CouplePhoto(photo, 150.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                coupleTitle(data),
                fontFamily = Script,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            if (since != null) {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        "♥  juntos desde ${Dates.short(since)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

private fun coupleTitle(d: CoupleData): String = when {
    d.name1.isNotBlank() && d.name2.isNotBlank() -> "${d.name1.trim()} & ${d.name2.trim()}"
    d.name1.isNotBlank() -> d.name1.trim()
    d.name2.isNotBlank() -> d.name2.trim()
    else -> "Nós Dois"
}

// ---------- Contador principal ----------

@Composable
private fun HeroCounter(start: LocalDate, today: LocalDate) {
    val p = love.pastels
    val future = start.isAfter(today)
    SectionCard {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Eyebrow(if (future) "Falta pouco" else "Juntos há")
            if (future) {
                Text(
                    Dates.breakdown(start, today),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                return@Column
            }
            AnimatedCount(
                Dates.daysBetween(start, today),
                style = TextStyle(
                    brush = love.love,
                    fontFamily = Nunito,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 68.sp,
                    lineHeight = 72.sp,
                ),
            )
            Text(
                "dias de namoro",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(18.dp))
            val period = Period.between(start, today)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TimeTile(period.years.toLong(), if (period.years == 1) "ano" else "anos", p[0], Modifier.weight(1f))
                TimeTile(period.months.toLong(), if (period.months == 1) "mês" else "meses", p[1], Modifier.weight(1f))
                TimeTile(period.days.toLong(), if (period.days == 1) "dia" else "dias", p[2], Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            LiveClock()

            Dates.anniversaryProgress(start, today)?.let { prog ->
                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    ProgressRing(
                        fraction = prog.fraction,
                        size = 76.dp,
                        stroke = 9.dp,
                        track = MaterialTheme.colorScheme.primaryContainer,
                        brush = love.love,
                    ) {
                        Text(
                            "${(prog.fraction * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Eyebrow("Próximo aniversário", MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${Dates.plural(prog.years.toLong(), "ano", "anos")} de namoro",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            if (prog.daysLeft == 0L) "É hoje! 🎉" else
                                "faltam ${Dates.plural(prog.daysLeft, "dia", "dias")} · ${Dates.short(prog.next)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeTile(value: Long, label: String, pastel: Pastel, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(pastel.bg)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedCount(value, style = MaterialTheme.typography.headlineSmall, color = pastel.fg)
        Text(label, style = MaterialTheme.typography.labelMedium, color = pastel.fg.copy(alpha = 0.8f))
    }
}

/** "e mais 14h 22min 08s" atualizando a cada segundo. */
@Composable
private fun LiveClock() {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L - (System.currentTimeMillis() % 1000))
            now = LocalTime.now()
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        PulsingHeart(14.dp, MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(
            "e mais %dh %02dmin %02ds".format(now.hour, now.minute, now.second),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------- Outros cartões ----------

@Composable
private fun MetCard(start: LocalDate, today: LocalDate, isMain: Boolean) {
    val pastel = love.pastels[1]
    SectionCard {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge("✨", pastel, 56.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow(if (isMain) "Se conheceram há" else "Desde que se conheceram", pastel.fg)
                if (start.isAfter(today)) {
                    Text(Dates.breakdown(start, today), style = MaterialTheme.typography.titleLarge)
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimatedCount(
                            Dates.daysBetween(start, today),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            " dias",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    Text(
                        Dates.breakdown(start, today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "desde ${Dates.long(start)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun NeverForgetCard(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(love.soft),
    ) {
        Text(
            "“",
            fontFamily = Script,
            fontSize = 120.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            modifier = Modifier.offset(x = 12.dp, y = (-28).dp),
        )
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 24.dp)) {
            Eyebrow("Nunca esquecer")
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                fontFamily = Script,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SpecialBanner(title: String, text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(love.banner)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎉", fontSize = 34.sp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun SetupCard(onOpenSettings: () -> Unit) {
    SectionCard {
        Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            EmojiBadge("📅", love.pastels[0], 64.dp)
            Spacer(Modifier.height(12.dp))
            Text("Coloquem as datas de vocês", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                "Quando se conheceram e quando começaram a namorar. O app conta cada dia e avisa nas datas especiais.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onOpenSettings, modifier = Modifier.height(48.dp)) { Text("Configurar agora") }
        }
    }
}

// ---------- Próximas datas (linha do tempo) ----------

private val monthAbbr = DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("pt-BR"))

private data class NextDate(val title: String, val date: LocalDate, val pastel: Int)

@Composable
private fun UpcomingCard(data: CoupleData, today: LocalDate) {
    val rows = buildList {
        data.datingDate?.let { s ->
            val y = Dates.nextYearly(s, today)
            if (y != s) add(NextDate("${Dates.plural((y.year - s.year).toLong(), "ano", "anos")} de namoro", y, 0))
            val m = Dates.nextMonthly(s, today)
            if (m != s && m != y) {
                val months = (m.year - s.year) * 12L + (m.monthValue - s.monthValue)
                add(NextDate("Mesversário · ${Dates.plural(months, "mês", "meses")}", m, 2))
            }
            val days = Dates.daysBetween(s, today)
            if (days >= 0) {
                val next100 = (days / 100 + 1) * 100
                add(NextDate("${Dates.num(next100)} dias juntos", s.plusDays(next100), 5))
            }
        }
        data.metDate?.let { s ->
            val y = Dates.nextYearly(s, today)
            if (y != s) add(NextDate("${Dates.plural((y.year - s.year).toLong(), "ano", "anos")} que se conheceram", y, 1))
        }
    }.sortedBy { it.date }

    if (rows.isEmpty()) return
    val pastels = love.pastels

    SectionCard {
        Column(Modifier.padding(20.dp)) {
            Eyebrow("Próximas datas especiais")
            Spacer(Modifier.height(10.dp))
            rows.forEachIndexed { i, r ->
                val pastel = pastels[r.pastel]
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // "folhinha de calendário"
                    Column(
                        Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(pastel.bg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            r.date.format(monthAbbr).trimEnd('.').uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = pastel.fg,
                        )
                        Text(
                            r.date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleLarge.copy(lineHeight = 22.sp),
                            color = pastel.fg,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            Dates.long(r.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val days = Dates.daysBetween(today, r.date)
                    val soon = days <= 1
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (soon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            when (days) {
                                0L -> "hoje!"
                                1L -> "amanhã"
                                else -> "${Dates.num(days)}d"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (soon) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                if (i < rows.lastIndex) {
                    Box(
                        Modifier
                            .padding(start = 26.dp)
                            .width(2.dp)
                            .height(8.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
        }
    }
}
