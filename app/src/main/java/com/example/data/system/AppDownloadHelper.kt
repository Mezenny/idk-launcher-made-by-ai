package com.example.data.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class FeaturedStoreApp(
    val title: String,
    val category: String,
    val description: String,
    val packageName: String
)

object AppDownloadHelper {

    val featuredCatalog: List<FeaturedStoreApp> = listOf(
        FeaturedStoreApp(
            title = "Google Chrome",
            category = "Browser",
            description = "Fast, secure web browser with sync",
            packageName = "com.android.chrome"
        ),
        FeaturedStoreApp(
            title = "Mozilla Firefox",
            category = "Browser",
            description = "Privacy-first browser with extensions",
            packageName = "org.mozilla.firefox"
        ),
        FeaturedStoreApp(
            title = "WhatsApp",
            category = "Messaging",
            description = "Encrypted messaging and voice calls",
            packageName = "com.whatsapp"
        ),
        FeaturedStoreApp(
            title = "Telegram",
            category = "Messaging",
            description = "Cloud-synced fast messaging",
            packageName = "org.telegram.messenger"
        ),
        FeaturedStoreApp(
            title = "Spotify",
            category = "Music",
            description = "Music streaming and podcasts",
            packageName = "com.spotify.music"
        ),
        FeaturedStoreApp(
            title = "YouTube",
            category = "Video",
            description = "Videos, channels, and music streams",
            packageName = "com.google.android.youtube"
        ),
        FeaturedStoreApp(
            title = "VLC for Android",
            category = "Media",
            description = "Open-source local video & audio player",
            packageName = "org.videolan.vlc"
        ),
        FeaturedStoreApp(
            title = "Google Docs",
            category = "Productivity",
            description = "Create and edit documents anywhere",
            packageName = "com.google.android.apps.docs.editors.docs"
        ),
        FeaturedStoreApp(
            title = "Slack",
            category = "Work",
            description = "Team messaging and channels",
            packageName = "com.Slack"
        ),
        FeaturedStoreApp(
            title = "Notion",
            category = "Productivity",
            description = "Notes, tasks, and wikis in one workspace",
            packageName = "notion.id"
        )
    )

    fun buildPlayStoreSearchIntent(query: String): Intent {
        val encoded = Uri.encode(query.trim())
        return Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$encoded&c=apps"))
    }

    fun buildPlayStoreWebSearchIntent(query: String): Intent {
        val encoded = Uri.encode(query.trim())
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/search?q=$encoded&c=apps")
        )
    }

    fun buildPlayStoreDetailsIntent(packageName: String): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
    }

    fun buildPlayStoreWebDetailsIntent(packageName: String): Intent {
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        )
    }

    fun openAppStoreHome(context: Context): Boolean {
        val vendingIntent = context.packageManager.getLaunchIntentForPackage("com.android.vending")
        val candidates = buildList {
            if (vendingIntent != null) add(vendingIntent)
            add(Intent(Intent.ACTION_VIEW, Uri.parse("market://apps")))
            add(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps")))
        }
        return launchFirst(context, candidates)
    }

    fun openAppStoreSearch(context: Context, query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return openAppStoreHome(context)
        val candidates = listOf(
            buildPlayStoreSearchIntent(trimmed),
            buildPlayStoreWebSearchIntent(trimmed)
        )
        return launchFirst(context, candidates)
    }

    fun openAppStoreDetails(context: Context, packageName: String): Boolean {
        val candidates = listOf(
            buildPlayStoreDetailsIntent(packageName),
            buildPlayStoreWebDetailsIntent(packageName)
        )
        return launchFirst(context, candidates)
    }

    private fun launchFirst(context: Context, intents: List<Intent>): Boolean {
        for (intent in intents) {
            val ok = runCatching {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (ok) return true
        }
        return false
    }
}
