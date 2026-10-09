package com.nosdois.app

import java.text.NumberFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class SpecialKind { DATING_YEARLY, MONTHLY, MET_YEARLY, MILESTONE, DAY_BEFORE }

data class Special(val kind: SpecialKind, val title: String, val text: String)

object Dates {
    private val ptBR = Locale.forLanguageTag("pt-BR")
    private val longFmt = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ptBR)
    private val shortFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", ptBR)
    private val numFmt = NumberFormat.getIntegerInstance(ptBR)

    fun long(d: LocalDate): String = longFmt.format(d)
    fun short(d: LocalDate): String = shortFmt.format(d)
    fun num(n: Long): String = numFmt.format(n)

    fun daysBetween(start: LocalDate, end: LocalDate): Long = ChronoUnit.DAYS.between(start, end)

    fun plural(n: Long, one: String, many: String) = if (n == 1L) "1 $one" else "${num(n)} $many"

    /** "2 anos, 3 meses e 5 dias" */
    fun breakdown(start: LocalDate, today: LocalDate): String {
        if (start.isAfter(today)) return "Faltam ${plural(daysBetween(today, start), "dia", "dias")}"
        val p = Period.between(start, today)
        val parts = buildList {
            if (p.years > 0) add(plural(p.years.toLong(), "ano", "anos"))
            if (p.months > 0) add(plural(p.months.toLong(), "mês", "meses"))
            if (p.days > 0) add(plural(p.days.toLong(), "dia", "dias"))
        }
        return when (parts.size) {
            0 -> "Começou hoje! 💕"
            1 -> parts[0]
            else -> parts.dropLast(1).joinToString(", ") + " e " + parts.last()
        }
    }

    /** Versão curta para o widget: "2a 3m 5d" */
    fun compact(start: LocalDate, today: LocalDate): String {
        if (start.isAfter(today)) return "faltam ${num(daysBetween(today, start))}d"
        val p = Period.between(start, today)
        return buildList {
            if (p.years > 0) add("${p.years}a")
            if (p.years > 0 || p.months > 0) add("${p.months}m")
            add("${p.days}d")
        }.joinToString(" ")
    }

    fun totalMonths(start: LocalDate, today: LocalDate): Long =
        if (start.isAfter(today)) 0 else Period.between(start, today).toTotalMonths()

    /** Quantos anos completa exatamente em [date] (0 se não for aniversário). */
    fun yearsOn(start: LocalDate, date: LocalDate): Int {
        val y = date.year - start.year
        return if (y > 0 && start.plusYears(y.toLong()) == date) y else 0
    }

    /** Quantos meses completa exatamente em [date] (0 se não for "mesversário"). */
    fun monthsOn(start: LocalDate, date: LocalDate): Int {
        val m = (date.year - start.year) * 12 + (date.monthValue - start.monthValue)
        return if (m > 0 && start.plusMonths(m.toLong()) == date) m else 0
    }

    /** Próximo aniversário anual (hoje conta). */
    fun nextYearly(start: LocalDate, today: LocalDate): LocalDate {
        if (start.isAfter(today)) return start
        val y = maxOf(1, today.year - start.year).toLong()
        val d = start.plusYears(y)
        return if (d.isBefore(today)) start.plusYears(y + 1) else d
    }

    data class Progress(val next: LocalDate, val years: Int, val daysLeft: Long, val fraction: Float)

    /** Quanto já passou do ano atual de namoro, rumo ao próximo aniversário. */
    fun anniversaryProgress(start: LocalDate, today: LocalDate): Progress? {
        if (start.isAfter(today)) return null
        val next = nextYearly(start, today)
        val years = next.year - start.year
        val prev = start.plusYears((years - 1).toLong())
        val total = daysBetween(prev, next).coerceAtLeast(1)
        val left = daysBetween(today, next)
        val fraction = if (left == 0L) 1f else (total - left).toFloat() / total
        return Progress(next, years, left, fraction)
    }

    /** Próximo mesversário (hoje conta). */
    fun nextMonthly(start: LocalDate, today: LocalDate): LocalDate {
        if (start.isAfter(today)) return start
        val m = maxOf(1, (today.year - start.year) * 12 + (today.monthValue - start.monthValue)).toLong()
        val d = start.plusMonths(m)
        return if (d.isBefore(today)) start.plusMonths(m + 1) else d
    }

    fun countdown(days: Long): String = when (days) {
        0L -> "É hoje! 🎉"
        1L -> "Amanhã!"
        else -> "em ${num(days)} dias"
    }

    /** Tudo o que há de especial em [today] para o casal. */
    fun specialsFor(d: CoupleData, today: LocalDate): List<Special> {
        val out = mutableListOf<Special>()
        val casal = d.coupleName
        val tomorrow = today.plusDays(1)

        d.datingDate?.let { s ->
            val y = yearsOn(s, today)
            if (y > 0) {
                out += Special(
                    SpecialKind.DATING_YEARLY, "Feliz aniversário de namoro! 💖",
                    "Hoje $casal completam ${plural(y.toLong(), "ano", "anos")} de namoro! Que venham muitos mais. ❤️",
                )
            } else {
                val m = monthsOn(s, today)
                if (m > 0) out += Special(
                    SpecialKind.MONTHLY, "Feliz mesversário! 💕",
                    "Hoje $casal completam ${plural(m.toLong(), "mês", "meses")} de namoro!",
                )
            }
            val days = daysBetween(s, today)
            if (days > 0 && days % 100 == 0L) out += Special(
                SpecialKind.MILESTONE, "Marco especial! 🎉",
                "Hoje são ${num(days)} dias de namoro! Cada dia ao seu lado vale a pena.",
            )
            if (yearsOn(s, tomorrow) > 0) out += Special(
                SpecialKind.DAY_BEFORE, "Amanhã é o grande dia! 🎁",
                "Amanhã é o aniversário de namoro de $casal. Já preparou a surpresa?",
            )
        }

        d.metDate?.let { s ->
            val y = yearsOn(s, today)
            if (y > 0) out += Special(
                SpecialKind.MET_YEARLY, "Dia em que tudo começou ✨",
                "Hoje faz ${plural(y.toLong(), "ano", "anos")} que $casal se conheceram!",
            )
            if (yearsOn(s, tomorrow) > 0) out += Special(
                SpecialKind.DAY_BEFORE, "Amanhã tem data especial! ✨",
                "Amanhã faz ${plural(yearsOn(s, tomorrow).toLong(), "ano", "anos")} que $casal se conheceram.",
            )
        }
        return out
    }
}
