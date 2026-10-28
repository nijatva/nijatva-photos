package com.vasudev.photos.data

import android.content.Context

/** Tiny settings store backed by SharedPreferences. */
object Prefs {
    private const val FILE = "zoho_photos_prefs"
    private const val KEY_NEWEST_FIRST = "photos_newest_first"
    private const val KEY_STRIP_METADATA = "strip_share_metadata"
    private const val KEY_AUTOPLAY = "autoplay_videos"
    private const val KEY_SHOW_PRIVATE = "show_private_album"
    private const val KEY_LABEL_AI = "label_ai_content"
    private const val KEY_EDITED_BEHAVIOR = "edited_file_behavior"
    private const val KEY_SECTION_ORDER = "albums_section_order"
    private const val KEY_HIDDEN_SECTIONS = "albums_hidden_sections"
    private const val KEY_LENS_API_URL = "lens_api_url"
    private const val KEY_LENS_API_KEY = "lens_api_key"

    val SECTION_IDS = listOf("moreAlbums", "featured", "mediaTypes", "more")

    fun sectionLabels(id: String): String = when (id) {
        "moreAlbums" -> "More albums"
        "featured" -> "Featured"
        "mediaTypes" -> "Media types"
        else -> "More"
    }

    fun newestFirst(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_NEWEST_FIRST, true)

    fun setNewestFirst(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_NEWEST_FIRST, value).apply()
    }

    fun stripMetadata(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_STRIP_METADATA, false)

    fun setStripMetadata(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_STRIP_METADATA, value).apply()
    }

    fun autoplayVideos(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTOPLAY, false)

    fun setAutoplayVideos(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTOPLAY, value).apply()
    }

    fun showPrivateAlbum(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_PRIVATE, true)

    fun setShowPrivateAlbum(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_PRIVATE, value).apply()
    }

    fun labelAiContent(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_LABEL_AI, false)

    fun setLabelAiContent(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LABEL_AI, value).apply()
    }

    fun editedFileBehavior(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_EDITED_BEHAVIOR, "Always ask") ?: "Always ask"

    fun setEditedFileBehavior(context: Context, value: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_EDITED_BEHAVIOR, value).apply()
    }

    fun sectionOrder(context: Context): List<String> {
        val saved = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_SECTION_ORDER, null) ?: return SECTION_IDS
        val parsed = saved.split(",").filter { it in SECTION_IDS }.distinct()
        // Append any missing sections so new sections always appear.
        return parsed + SECTION_IDS.filter { it !in parsed }
    }

    fun setSectionOrder(context: Context, order: List<String>) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_SECTION_ORDER, order.joinToString(",")).apply()
    }

    fun lensApiUrl(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_LENS_API_URL, "") ?: ""

    fun setLensApiUrl(context: Context, value: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_LENS_API_URL, value.trim()).apply()
    }

    fun lensApiKey(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_LENS_API_KEY, "") ?: ""

    fun setLensApiKey(context: Context, value: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_LENS_API_KEY, value.trim()).apply()
    }

    fun hiddenSections(context: Context): Set<String> =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_HIDDEN_SECTIONS, "")!!.split(",")
            .filter { it.isNotBlank() }.toSet()

    fun setHiddenSections(context: Context, hidden: Set<String>) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_HIDDEN_SECTIONS, hidden.joinToString(",")).apply()
    }
}
