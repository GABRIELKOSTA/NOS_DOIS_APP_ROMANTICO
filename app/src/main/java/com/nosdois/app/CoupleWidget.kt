package com.nosdois.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.ZoneId

/** Widget "Contador": muda de visual conforme o tamanho (pequeno, médio, grande). */
class CoupleWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        val d = Repo.load(ctx)
        ids.forEach { WidgetRenderer.updateCounter(ctx, mgr, it, d) }
        scheduleMidnight(ctx)
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, options: Bundle) {
        WidgetRenderer.updateCounter(ctx, mgr, id, Repo.load(ctx))
    }

    override fun onEnabled(ctx: Context) = scheduleMidnight(ctx)

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_REFRESH) {
            updateAll(ctx)
            scheduleMidnight(ctx)
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.nosdois.app.ATUALIZAR_WIDGET"

        /** Atualiza todos os widgets do app (Contador e Foto). */
        fun updateAll(ctx: Context) = WidgetRenderer.updateAll(ctx)

        /** Agenda a atualização para 00:01 do dia seguinte, quando a contagem de dias muda. */
        fun scheduleMidnight(ctx: Context) {
            val at = LocalDate.now().plusDays(1).atStartOfDay().plusMinutes(1)
            val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val pi = PendingIntent.getBroadcast(
                ctx, 1,
                Intent(ctx, CoupleWidget::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            ctx.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC, millis, pi)
        }
    }
}

/** Widget "Foto": a foto do casal ocupando tudo, com os contadores por cima. */
class PhotoWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        val d = Repo.load(ctx)
        ids.forEach { WidgetRenderer.updatePhoto(ctx, mgr, it, d) }
        CoupleWidget.scheduleMidnight(ctx)
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, options: Bundle) {
        WidgetRenderer.updatePhoto(ctx, mgr, id, Repo.load(ctx))
    }

    override fun onEnabled(ctx: Context) = CoupleWidget.scheduleMidnight(ctx)
}

private object WidgetRenderer {
    // Larguras/alturas (dp) a partir das quais cada visual do Contador é usado
    private const val MEDIUM_MIN_WIDTH = 230f
    private const val LARGE_MIN_HEIGHT = 220f

    fun updateAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val counters = mgr.getAppWidgetIds(ComponentName(ctx, CoupleWidget::class.java))
        val photos = mgr.getAppWidgetIds(ComponentName(ctx, PhotoWidget::class.java))
        if (counters.isEmpty() && photos.isEmpty()) return
        val d = Repo.load(ctx)
        counters.forEach { updateCounter(ctx, mgr, it, d) }
        photos.forEach { updatePhoto(ctx, mgr, it, d) }
    }

    // ---------- Contador ----------

    fun updateCounter(ctx: Context, mgr: AppWidgetManager, id: Int, d: CoupleData) {
        val today = LocalDate.now()
        val thumb = PhotoStore.loadThumb(ctx)
        val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: o próprio sistema escolhe o visual conforme o tamanho
            RemoteViews(
                mapOf(
                    SizeF(100f, 100f) to counter(ctx, R.layout.widget_small, d, today, thumb),
                    SizeF(MEDIUM_MIN_WIDTH, 100f) to counter(ctx, R.layout.widget_medium, d, today, thumb),
                    SizeF(MEDIUM_MIN_WIDTH, LARGE_MIN_HEIGHT) to counter(ctx, R.layout.widget_large, d, today, thumb),
                ),
            )
        } else {
            val (w, h) = sizeDp(mgr, id)
            val layout = when {
                w < MEDIUM_MIN_WIDTH -> R.layout.widget_small
                h < LARGE_MIN_HEIGHT -> R.layout.widget_medium
                else -> R.layout.widget_large
            }
            counter(ctx, layout, d, today, thumb)
        }
        mgr.updateAppWidget(id, views)
    }

    private fun counter(ctx: Context, layout: Int, d: CoupleData, today: LocalDate, thumb: Bitmap?): RemoteViews {
        val v = RemoteViews(ctx.packageName, layout)
        val main = d.datingDate ?: d.metDate

        if (layout == R.layout.widget_small) {
            v.setTextViewText(R.id.w_dating_days, main?.let { days(it, today) } ?: "♥")
            v.setTextViewText(
                R.id.w_dating_label,
                when {
                    d.datingDate != null -> "dias juntos"
                    d.metDate != null -> "dias se conhecendo"
                    else -> "Nós Dois"
                },
            )
            v.setTextViewText(R.id.w_since, main?.let { "desde ${Dates.short(it)}" } ?: "toque para configurar")
        } else {
            v.setTextViewText(R.id.w_names, names(d))
            v.setTextViewText(R.id.w_since, main?.let { "juntos desde ${Dates.short(it)}" } ?: "toque para configurar")
            if (thumb != null) v.setImageViewBitmap(R.id.w_photo, thumb)
            v.setTextViewText(R.id.w_dating_days, d.datingDate?.let { days(it, today) } ?: "—")
            v.setTextViewText(R.id.w_met_days, d.metDate?.let { days(it, today) } ?: "—")
        }

        if (layout == R.layout.widget_large) {
            v.setViewVisibility(R.id.w_dating_detail, View.VISIBLE)
            v.setViewVisibility(R.id.w_met_detail, View.VISIBLE)
            v.setTextViewText(R.id.w_dating_detail, d.datingDate?.let { Dates.compact(it, today) } ?: "defina no app")
            v.setTextViewText(R.id.w_met_detail, d.metDate?.let { Dates.compact(it, today) } ?: "defina no app")

            val prog = d.datingDate?.let { Dates.anniversaryProgress(it, today) }
            if (prog == null) {
                v.setViewVisibility(R.id.w_next_box, View.GONE)
            } else {
                v.setViewVisibility(R.id.w_next_box, View.VISIBLE)
                v.setTextViewText(R.id.w_next_title, "🎂 ${Dates.plural(prog.years.toLong(), "ano", "anos")} de namoro")
                v.setTextViewText(
                    R.id.w_next_count,
                    if (prog.daysLeft == 0L) "é hoje! 🎉" else "faltam ${Dates.plural(prog.daysLeft, "dia", "dias")}",
                )
                v.setProgressBar(R.id.w_progress, 100, (prog.fraction * 100).toInt(), false)
            }
            v.setTextViewText(R.id.w_message, d.neverForget.trim().ifEmpty { "Escreva sua mensagem em Ajustes no app 💕" })
        }

        v.setOnClickPendingIntent(R.id.w_root, openApp(ctx))
        return v
    }

    // ---------- Foto ----------

    fun updatePhoto(ctx: Context, mgr: AppWidgetManager, id: Int, d: CoupleData) {
        val today = LocalDate.now()
        val v = RemoteViews(ctx.packageName, R.layout.widget_photo)
        val (w, h) = sizeDp(mgr, id)
        val photo = PhotoStore.widgetPhoto(ctx, w, h)
        if (photo != null) v.setImageViewBitmap(R.id.w_bg, photo) else v.setImageViewResource(R.id.w_bg, R.drawable.widget_bg)

        v.setTextViewText(R.id.w_names, names(d))
        if (d.datingDate != null) {
            v.setViewVisibility(R.id.w_dating_box, View.VISIBLE)
            v.setTextViewText(R.id.w_dating_days, days(d.datingDate, today))
        } else {
            v.setViewVisibility(R.id.w_dating_box, View.GONE)
        }
        if (d.metDate != null) {
            v.setViewVisibility(R.id.w_met_box, View.VISIBLE)
            v.setTextViewText(R.id.w_met_days, days(d.metDate, today))
        } else {
            v.setViewVisibility(R.id.w_met_box, View.GONE)
        }
        val msg = d.neverForget.trim()
        if (msg.isEmpty()) {
            v.setViewVisibility(R.id.w_message, View.GONE)
        } else {
            v.setViewVisibility(R.id.w_message, View.VISIBLE)
            v.setTextViewText(R.id.w_message, "“$msg”")
        }
        v.setOnClickPendingIntent(R.id.w_root, openApp(ctx))
        mgr.updateAppWidget(id, v)
    }

    // ---------- Utilidades ----------

    private fun days(start: LocalDate, today: LocalDate) = Dates.num(maxOf(0, Dates.daysBetween(start, today)))

    private fun names(d: CoupleData): String = when {
        d.name1.isNotBlank() && d.name2.isNotBlank() -> "${d.name1.trim()} & ${d.name2.trim()}"
        d.name1.isNotBlank() -> d.name1.trim()
        d.name2.isNotBlank() -> d.name2.trim()
        else -> "Nós Dois"
    }

    /** Tamanho atual do widget em dp (largura e altura com o celular em pé). */
    private fun sizeDp(mgr: AppWidgetManager, id: Int): Pair<Float, Float> {
        val o = mgr.getAppWidgetOptions(id)
        val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        return if (w <= 0 || h <= 0) 250f to 250f else w.toFloat() to h.toFloat()
    }

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
