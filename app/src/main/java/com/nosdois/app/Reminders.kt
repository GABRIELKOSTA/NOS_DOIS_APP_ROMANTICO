package com.nosdois.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Lembretes: um alarme diário no horário escolhido verifica se o dia é especial
 * (aniversário de namoro, mesversário, dia em que se conheceram, marcos de 100 dias)
 * e mostra a notificação. Depois, agenda o próximo dia.
 */
object Reminders {
    private const val CHANNEL_PREFIX = "datas_"
    private const val PREFS = "lembretes"
    private const val KEY_LAST_DAY = "ultimo_dia"

    /** O som de um canal não pode mudar depois de criado, então cada som tem o seu canal. */
    private fun channelId(d: CoupleData) = CHANNEL_PREFIX + Integer.toHexString(d.soundKey.hashCode())

    /** Cria o canal com o som escolhido e apaga os canais de sons antigos. */
    fun createChannel(ctx: Context): String {
        val d = Repo.load(ctx)
        val id = channelId(d)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.notificationChannels
            .filter { it.id != id && it.id.startsWith(CHANNEL_PREFIX) }
            .forEach { nm.deleteNotificationChannel(it.id) }
        if (nm.getNotificationChannel(id) == null) {
            val ch = NotificationChannel(id, "Datas especiais", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Aniversário de namoro, mesversário e o dia em que vocês se conheceram"
                val sound = Sounds.uri(ctx, d.soundKey)
                if (sound == null) {
                    setSound(null, null)
                } else {
                    setSound(
                        sound,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                }
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFFE0457B.toInt()
            }
            nm.createNotificationChannel(ch)
        }
        return id
    }

    fun schedule(ctx: Context) {
        val d = Repo.load(ctx)
        var at = LocalDate.now().atTime(d.notifyHour, 0)
        if (!at.isAfter(LocalDateTime.now())) at = at.plusDays(1)
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = PendingIntent.getBroadcast(
            ctx, 0, Intent(ctx, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        ctx.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    /** Se o horário de hoje já passou e o alarme não rodou (celular desligado etc.), roda agora. */
    fun catchUp(ctx: Context) {
        if (LocalTime.now().hour >= Repo.load(ctx).notifyHour) runDaily(ctx)
    }

    fun runDaily(ctx: Context) {
        val today = LocalDate.now()
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_DAY, null) == today.toString()) return
        prefs.edit().putString(KEY_LAST_DAY, today.toString()).apply()

        val d = Repo.load(ctx)
        Dates.specialsFor(d, today)
            .filter {
                when (it.kind) {
                    SpecialKind.DATING_YEARLY -> d.notifyDatingYearly
                    SpecialKind.MONTHLY -> d.notifyMonthly
                    SpecialKind.MET_YEARLY -> d.notifyMet
                    SpecialKind.MILESTONE -> d.notifyMilestones
                    SpecialKind.DAY_BEFORE -> d.notifyDayBefore
                }
            }
            .forEachIndexed { i, s -> show(ctx, 100 + i, s.title, s.text) }
    }

    fun canNotify(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun show(ctx: Context, id: Int, title: String, text: String) {
        if (!canNotify(ctx)) return
        val channel = createChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_heart)
            .setColor(0xFFE0457B.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Reminders.runDaily(ctx)
        Reminders.schedule(ctx)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Reminders.catchUp(ctx)
        Reminders.schedule(ctx)
        CoupleWidget.updateAll(ctx)
        CoupleWidget.scheduleMidnight(ctx)
    }
}
