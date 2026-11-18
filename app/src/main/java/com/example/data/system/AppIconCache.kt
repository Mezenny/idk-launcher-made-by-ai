package com.example.data.system

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class AppIconCache(private val context: Context) {

    private val maxCacheEntries = 240
    private val cache = object : LruCache<String, ImageBitmap>(maxCacheEntries) {}
    private val hits = AtomicLong(0L)
    private val misses = AtomicLong(0L)

    // External icon pack component -> drawableName cache
    private val externalPackMaps = ConcurrentHashMap<String, Map<String, String>>()

    fun getHits(): Long = hits.get()
    fun getMisses(): Long = misses.get()
    fun getEntryCount(): Int = cache.size()

    fun clear() {
        cache.evictAll()
        hits.set(0L)
        misses.set(0L)
    }

    fun getCachedIcon(componentKey: String, externalPackPkg: String?): ImageBitmap? {
        val key = buildCacheKey(componentKey, externalPackPkg)
        val bitmap = cache.get(key)
        if (bitmap != null) {
            hits.incrementAndGet()
        }
        return bitmap
    }

    suspend fun loadIcon(
        packageName: String,
        activityName: String,
        externalPackPkg: String? = null
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        val componentKey = "$packageName/$activityName"
        val key = buildCacheKey(componentKey, externalPackPkg)

        cache.get(key)?.let {
            hits.incrementAndGet()
            return@withContext it
        }
        misses.incrementAndGet()

        val drawable: Drawable? = try {
            if (!externalPackPkg.isNullOrBlank()) {
                loadFromExternalPack(packageName, activityName, externalPackPkg)
                    ?: loadSystemDrawable(packageName, activityName)
            } else {
                loadSystemDrawable(packageName, activityName)
            }
        } catch (_: Exception) {
            null
        }

        val bitmap = drawable?.let { rasterizeDrawable(it, 176) }?.asImageBitmap()
        if (bitmap != null) {
            cache.put(key, bitmap)
        }
        bitmap
    }

    private fun buildCacheKey(componentKey: String, externalPackPkg: String?): String {
        return if (externalPackPkg.isNullOrBlank()) componentKey else "$externalPackPkg::$componentKey"
    }

    private fun loadSystemDrawable(packageName: String, activityName: String): Drawable? {
        val pm = context.packageManager
        return try {
            val component = ComponentName(packageName, activityName)
            pm.getActivityIcon(component)
        } catch (_: PackageManager.NameNotFoundException) {
            try {
                pm.getApplicationIcon(packageName)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun loadFromExternalPack(
        packageName: String,
        activityName: String,
        iconPackPkg: String
    ): Drawable? {
        return try {
            val pm = context.packageManager
            val packRes = pm.getResourcesForApplication(iconPackPkg)
            val componentMap = externalPackMaps.getOrPut(iconPackPkg) {
                parseAppFilterXml(packRes, iconPackPkg)
            }
            val fullComponent = "ComponentInfo{$packageName/$activityName}"
            val drawableName = componentMap[fullComponent]
                ?: componentMap[packageName]
                ?: return null
            val resId = packRes.getIdentifier(drawableName, "drawable", iconPackPkg)
            if (resId != 0) {
                packRes.getDrawable(resId, null)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseAppFilterXml(res: Resources, iconPackPkg: String): Map<String, String> {
        val map = HashMap<String, String>(512)
        try {
            val xmlId = res.getIdentifier("appfilter", "xml", iconPackPkg)
            if (xmlId != 0) {
                val parser = res.getXml(xmlId)
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val comp = parser.getAttributeValue(null, "component")
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (!comp.isNullOrBlank() && !drawable.isNullOrBlank()) {
                            map[comp] = drawable
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (_: Exception) {
            // Gracefully fall back if external pack does not expose standard appfilter.xml
        }
        return map
    }

    private fun rasterizeDrawable(drawable: Drawable, targetSizePx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val src = drawable.bitmap
            if (src.width <= targetSizePx && src.height <= targetSizePx) {
                return src
            }
            return Bitmap.createScaledBitmap(src, targetSizePx, targetSizePx, true)
        }

        val bitmap = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            drawable.setBounds(0, 0, targetSizePx, targetSizePx)
            drawable.draw(canvas)
        } else {
            val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: targetSizePx
            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: targetSizePx
            val ratio = minOf(targetSizePx.toFloat() / w, targetSizePx.toFloat() / h)
            val drawW = (w * ratio).toInt().coerceAtLeast(1)
            val drawH = (h * ratio).toInt().coerceAtLeast(1)
            val left = (targetSizePx - drawW) / 2
            val top = (targetSizePx - drawH) / 2
            drawable.setBounds(left, top, left + drawW, top + drawH)
            drawable.draw(canvas)
        }
        return bitmap
    }
}
