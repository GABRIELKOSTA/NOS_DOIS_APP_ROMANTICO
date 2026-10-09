package com.nosdois.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DatesTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun breakdown() {
        assertEquals("2 anos, 3 meses e 5 dias", Dates.breakdown(d("2022-01-10"), d("2024-04-15")))
        assertEquals("1 ano", Dates.breakdown(d("2023-05-01"), d("2024-05-01")))
        assertEquals("Começou hoje! 💕", Dates.breakdown(d("2024-05-01"), d("2024-05-01")))
        assertEquals("Faltam 3 dias", Dates.breakdown(d("2024-05-04"), d("2024-05-01")))
    }

    @Test fun compact() {
        assertEquals("2a 3m 5d", Dates.compact(d("2022-01-10"), d("2024-04-15")))
        assertEquals("1a 0m 0d", Dates.compact(d("2023-05-01"), d("2024-05-01")))
        assertEquals("2m 3d", Dates.compact(d("2024-03-01"), d("2024-05-04")))
        assertEquals("0d", Dates.compact(d("2024-05-01"), d("2024-05-01")))
    }

    @Test fun progress() {
        val p = Dates.anniversaryProgress(d("2022-06-12"), d("2024-12-12"))!!
        assertEquals(d("2025-06-12"), p.next)
        assertEquals(3, p.years)
        assertEquals(182L, p.daysLeft)
        assertTrue(p.fraction in 0.49f..0.51f)
        assertEquals(1f, Dates.anniversaryProgress(d("2022-06-12"), d("2024-06-12"))!!.fraction)
        assertEquals(1, Dates.anniversaryProgress(d("2024-06-12"), d("2024-06-13"))!!.years)
    }

    @Test fun anniversaries() {
        assertEquals(2, Dates.yearsOn(d("2022-06-12"), d("2024-06-12")))
        assertEquals(0, Dates.yearsOn(d("2022-06-12"), d("2024-06-13")))
        // 29/02 comemora em 28/02 nos anos não bissextos
        assertEquals(1, Dates.yearsOn(d("2024-02-29"), d("2025-02-28")))
        // começou dia 31: mesversário no último dia dos meses curtos
        assertEquals(1, Dates.monthsOn(d("2024-01-31"), d("2024-02-29")))
        assertEquals(3, Dates.monthsOn(d("2024-01-31"), d("2024-04-30")))
        assertEquals(0, Dates.monthsOn(d("2024-01-31"), d("2024-03-30")))
    }

    @Test fun nextDates() {
        assertEquals(d("2024-06-12"), Dates.nextYearly(d("2020-06-12"), d("2024-06-12")))
        assertEquals(d("2025-06-12"), Dates.nextYearly(d("2020-06-12"), d("2024-06-13")))
        assertEquals(d("2025-06-12"), Dates.nextYearly(d("2024-06-12"), d("2024-06-12")))
        assertEquals(d("2024-07-12"), Dates.nextMonthly(d("2024-06-12"), d("2024-06-12")))
        assertEquals(d("2024-09-12"), Dates.nextMonthly(d("2024-06-12"), d("2024-08-13")))
    }

    @Test fun specials() {
        val c = CoupleData(name1 = "Ana", name2 = "João", metDate = d("2021-03-05"), datingDate = d("2022-06-12"))
        val anniv = Dates.specialsFor(c, d("2024-06-12")).map { it.kind }
        assertTrue(SpecialKind.DATING_YEARLY in anniv)
        assertTrue(SpecialKind.MONTHLY !in anniv)
        assertTrue(SpecialKind.MONTHLY in Dates.specialsFor(c, d("2024-07-12")).map { it.kind })
        assertTrue(SpecialKind.DAY_BEFORE in Dates.specialsFor(c, d("2024-06-11")).map { it.kind })
        assertTrue(SpecialKind.MET_YEARLY in Dates.specialsFor(c, d("2024-03-05")).map { it.kind })
        assertTrue(SpecialKind.MILESTONE in Dates.specialsFor(c, d("2022-06-12").plusDays(300)).map { it.kind })
        assertTrue(Dates.specialsFor(c, d("2024-06-20")).isEmpty())
        assertTrue(Dates.specialsFor(c, d("2024-06-12")).first().text.contains("Ana e João completam 2 anos"))
    }
}
