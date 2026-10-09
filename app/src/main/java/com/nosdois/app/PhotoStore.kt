package com.nosdois.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File

/** Guarda uma cópia da foto do casal dentro do app (continua funcionando mesmo se a original for apagada). */
object PhotoStore {
    private const val MAX_SIZE = 1600
    private const val THUMB_SIZE = 192

    private fun file(ctx: Context) = File(ctx.filesDir, "foto_casal.jpg")
    private fun thumbFile(ctx: Context) = File(ctx.filesDir, "foto_casal_widget.png")

    /** Miniatura redonda e leve para o widget (é criada a partir da foto se ainda não existir). */
    fun loadThumb(ctx: Context): Bitmap? {
        val t = thumbFile(ctx)
        if (!t.exists()) {
            val full = load(ctx) ?: return null
            saveThumb(ctx, full)
        }
        return BitmapFactory.decodeFile(t.absolutePath)
    }

    private fun saveThumb(ctx: Context, src: Bitmap) {
        val side = minOf(src.width, src.height)
        val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, THUMB_SIZE, THUMB_SIZE, true)
        val out = Bitmap.createBitmap(THUMB_SIZE, THUMB_SIZE, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val r = THUMB_SIZE / 2f
        Canvas(out).drawCircle(r, r, r, paint)
        thumbFile(ctx).outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private var widgetCache: Pair<String, Bitmap>? = null

    /**
     * Foto para o widget "Foto": recortada no formato do widget e com cantos arredondados
     * (assim fica bonita em qualquer versão do Android). Guarda em cache para não refazer à toa.
     */
    fun widgetPhoto(ctx: Context, widthDp: Float, heightDp: Float): Bitmap? {
        val f = file(ctx)
        if (!f.exists()) return null
        val key = "${f.lastModified()}:${widthDp.toInt()}x${heightDp.toInt()}"
        widgetCache?.let { (k, bmp) -> if (k == key) return bmp }

        val density = ctx.resources.displayMetrics.density
        var w = widthDp * density
        var h = heightDp * density
        val shrink = minOf(1f, 720f / maxOf(w, h))
        w *= shrink
        h *= shrink
        val outW = w.toInt().coerceAtLeast(1)
        val outH = h.toInt().coerceAtLeast(1)

        val src = loadScaled(f, maxOf(outW, outH)) ?: return null
        val scale = maxOf(outW.toFloat() / src.width, outH.toFloat() / src.height)
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate((outW - src.width * scale) / 2f, (outH - src.height * scale) / 2f)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(matrix) }
        }
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val radius = 24f * density * shrink
        Canvas(out).drawRoundRect(0f, 0f, outW.toFloat(), outH.toFloat(), radius, radius, paint)
        widgetCache = key to out
        return out
    }

    /** Abre a foto já reduzida (para não gastar memória à toa). */
    private fun loadScaled(f: File, target: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.absolutePath, bounds)
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= target) sample *= 2
        return BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    fun load(ctx: Context): Bitmap? {
        val f = file(ctx)
        return if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    }

    fun saveFromUri(ctx: Context, uri: Uri): Boolean = runCatching {
        val resolver = ctx.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIZE) sample *= 2

        var bmp = resolver.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }!!

        val rotation = resolver.openInputStream(uri)!!.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }
        if (rotation != 0f) {
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rotation) }, true)
        }

        file(ctx).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        saveThumb(ctx, bmp)
        true
    }.getOrDefault(false)
}
