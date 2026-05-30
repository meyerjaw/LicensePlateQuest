package com.getmecookies.licenseplatequest.data.map

import android.content.Context
import android.graphics.Region
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposePath
import androidx.core.graphics.PathParser
import com.getmecookies.licenseplatequest.ui.map.StateShape
import com.getmecookies.licenseplatequest.ui.map.UsMapShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Loads and parses the bundled US map shapes (Milestone 5). Parsing every state path into a
 * [android.graphics.Path] + [Region] is non-trivial, so the result is computed once off the
 * main thread and cached for the process lifetime.
 */
class MapRepository(
    private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    @Volatile private var cached: UsMapShapes? = null

    suspend fun loadShapes(): UsMapShapes {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: parse().also { cached = it }
        }
    }

    private suspend fun parse(): UsMapShapes = withContext(Dispatchers.Default) {
        val text = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        val dto = json.decodeFromString(MapShapesDto.serializer(), text)

        val bounds = Region(
            0,
            0,
            dto.viewBox.width.toInt() + 1,
            dto.viewBox.height.toInt() + 1,
        )

        val shapes = dto.states.map { (code, pathData) ->
            val androidPath = PathParser.createPathFromPathData(pathData)
                ?: error("Invalid map path data for state $code")
            val region = Region().apply { setPath(androidPath, bounds) }
            val anchor = dto.centroids[code]?.takeIf { it.size >= 2 }?.let { Offset(it[0], it[1]) }
            StateShape(
                code = code,
                path = androidPath.asComposePath(),
                region = region,
                labelAnchor = anchor,
            )
        }

        UsMapShapes(
            width = dto.viewBox.width,
            height = dto.viewBox.height,
            states = shapes,
            showLabels = dto.style == "tile-grid",
        )
    }

    private companion object {
        const val ASSET_PATH = "maps/us_states_paths.json"
    }
}
