package com.nosdois.app

import android.content.ContentValues
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.annotation.RequiresApi

/**
 * Sons de notificação. A escolha é guardada como uma "chave":
 *  - "default"     → som padrão do celular
 *  - "silent"      → sem som
 *  - "app:<nome>"  → som exclusivo do app (res/raw/<nome>)
 *  - qualquer URI  → toque do celular ou áudio importado
 */
object Sounds {
    const val DEFAULT = "default"
    const val SILENT = "silent"

    data class Option(val key: String, val name: String, val emoji: String)

    val defaultOption = Option(DEFAULT, "Padrão do celular", "📱")
    val silentOption = Option(SILENT, "Silencioso", "🔕")
    val builtIn = listOf(
        Option("app:sininho", "Sininho do amor", "🔔"),
        Option("app:melodia", "Melodia doce", "🎶"),
        Option("app:coracao", "Batida do coração", "💓"),
    )

    fun uri(ctx: Context, key: String): Uri? = when {
        key == DEFAULT -> Settings.System.DEFAULT_NOTIFICATION_URI
        key == SILENT -> null
        key.startsWith("app:") -> Uri.parse("android.resource://${ctx.packageName}/raw/${key.removePrefix("app:")}")
        else -> Uri.parse(key)
    }

    /** Nome amigável de um toque escolhido no seletor do sistema. */
    fun title(ctx: Context, uri: Uri): String =
        runCatching { RingtoneManager.getRingtone(ctx, uri)?.getTitle(ctx) }.getOrNull() ?: "Toque do celular"

    /**
     * Copia um áudio escolhido pela pessoa para a pasta "Notifications/Nós Dois" do celular,
     * onde o sistema consegue tocá-lo como som de notificação.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun importAudio(ctx: Context, src: Uri): Option? = runCatching {
        val cr = ctx.contentResolver
        val fileName = cr.query(src, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "Meu toque"
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, cr.getType(src) ?: "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_NOTIFICATIONS}/Nós Dois")
            put(MediaStore.Audio.Media.IS_NOTIFICATION, 1)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val dest = cr.insert(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)!!
        cr.openInputStream(src)!!.use { input -> cr.openOutputStream(dest)!!.use { input.copyTo(it) } }
        cr.update(dest, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
        Option(dest.toString(), fileName.substringBeforeLast('.'), "🎵")
    }.getOrNull()
}
