package com.ioannapergamali.mysmartroute.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

/**
 * Φορτώνει τα εξωτερικά Materialize themes από το αρχείο assets/themes.json.
 */
object ThemeLoader {
    private var cache: List<CustomTheme>? = null

    fun load(context: Context): List<CustomTheme> {
        cache?.let { return it }
        return try {
            context.assets.open("themes.json").use { stream ->
                InputStreamReader(stream).use { reader ->
                    val type = object : TypeToken<List<ThemeEntry>>() {}.type
                    val entries: List<ThemeEntry> = Gson().fromJson(reader, type)
                    val themes = entries.map {
                        CustomTheme(
                            label = it.label,
                            seed = Color(android.graphics.Color.parseColor(it.seed))
                        )
                    }
                    cache = themes
                    themes
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private data class ThemeEntry(val label: String, val seed: String)
}
