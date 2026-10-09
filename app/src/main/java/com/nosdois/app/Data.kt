package com.nosdois.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class Like(val id: Long, val emoji: String, val text: String)

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val note: String = "",
    val link: String = "",
)

data class CoupleData(
    val name1: String = "",
    val name2: String = "",
    val metDate: LocalDate? = null,
    val datingDate: LocalDate? = null,
    val likes: List<Like> = emptyList(),
    val favoriteSong: Song? = null,
    val playlist: List<Song> = emptyList(),
    val neverForget: String = "",
    val notifyDatingYearly: Boolean = true,
    val notifyMonthly: Boolean = true,
    val notifyMet: Boolean = true,
    val notifyMilestones: Boolean = true,
    val notifyDayBefore: Boolean = true,
    val notifyHour: Int = 9,
    val photoVersion: Long = 0,
    /** 0 = automático (segue o celular), 1 = claro, 2 = escuro */
    val themeMode: Int = 0,
    val soundKey: String = Sounds.DEFAULT,
    val soundName: String = Sounds.defaultOption.name,
) {
    val isSetUp get() = name1.isNotBlank() || name2.isNotBlank()

    /** "Ana e João", ou "vocês" se faltar algum nome. */
    val coupleName: String
        get() = when {
            name1.isNotBlank() && name2.isNotBlank() -> "${name1.trim()} e ${name2.trim()}"
            else -> "vocês"
        }
}

object Repo {
    private const val PREFS = "nos_dois"
    private const val KEY = "dados"

    fun load(ctx: Context): CoupleData {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return CoupleData()
        return try {
            fromJson(JSONObject(raw))
        } catch (e: Exception) {
            CoupleData()
        }
    }

    fun save(ctx: Context, d: CoupleData) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, toJson(d).toString()).apply()
    }

    private fun toJson(d: CoupleData) = JSONObject()
        .put("name1", d.name1)
        .put("name2", d.name2)
        .put("metDate", d.metDate?.toString() ?: "")
        .put("datingDate", d.datingDate?.toString() ?: "")
        .put("likes", JSONArray().apply {
            d.likes.forEach {
                put(JSONObject().put("id", it.id).put("emoji", it.emoji).put("text", it.text))
            }
        })
        .put("favoriteSong", d.favoriteSong?.toJson() ?: JSONObject.NULL)
        .put("playlist", JSONArray().apply { d.playlist.forEach { put(it.toJson()) } })
        .put("neverForget", d.neverForget)
        .put("notifyDatingYearly", d.notifyDatingYearly)
        .put("notifyMonthly", d.notifyMonthly)
        .put("notifyMet", d.notifyMet)
        .put("notifyMilestones", d.notifyMilestones)
        .put("notifyDayBefore", d.notifyDayBefore)
        .put("notifyHour", d.notifyHour)
        .put("photoVersion", d.photoVersion)
        .put("themeMode", d.themeMode)
        .put("soundKey", d.soundKey)
        .put("soundName", d.soundName)

    private fun fromJson(o: JSONObject): CoupleData {
        val likes = o.optJSONArray("likes")?.let { arr ->
            (0 until arr.length()).map { i ->
                val l = arr.getJSONObject(i)
                Like(l.optLong("id"), l.optString("emoji", "❤️"), l.optString("text"))
            }
        } ?: emptyList()
        val playlist = o.optJSONArray("playlist")?.let { arr ->
            (0 until arr.length()).map { arr.getJSONObject(it).toSong() }
        } ?: emptyList()
        return CoupleData(
            name1 = o.optString("name1"),
            name2 = o.optString("name2"),
            metDate = o.optString("metDate").toDateOrNull(),
            datingDate = o.optString("datingDate").toDateOrNull(),
            likes = likes,
            favoriteSong = o.optJSONObject("favoriteSong")?.toSong(),
            playlist = playlist,
            neverForget = o.optString("neverForget"),
            notifyDatingYearly = o.optBoolean("notifyDatingYearly", true),
            notifyMonthly = o.optBoolean("notifyMonthly", true),
            notifyMet = o.optBoolean("notifyMet", true),
            notifyMilestones = o.optBoolean("notifyMilestones", true),
            notifyDayBefore = o.optBoolean("notifyDayBefore", true),
            notifyHour = o.optInt("notifyHour", 9),
            photoVersion = o.optLong("photoVersion"),
            themeMode = o.optInt("themeMode", 0),
            soundKey = o.optString("soundKey", Sounds.DEFAULT),
            soundName = o.optString("soundName", Sounds.defaultOption.name),
        )
    }

    private fun Song.toJson() = JSONObject()
        .put("id", id).put("title", title).put("artist", artist).put("note", note).put("link", link)

    private fun JSONObject.toSong() = Song(
        optLong("id"), optString("title"), optString("artist"), optString("note"), optString("link"),
    )

    private fun String.toDateOrNull(): LocalDate? =
        if (isBlank()) null else runCatching { LocalDate.parse(this) }.getOrNull()
}
