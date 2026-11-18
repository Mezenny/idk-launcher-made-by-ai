package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.data.system.AppDownloadHelper
import com.example.data.system.ConnectivityRepository
import com.example.data.system.DefaultLauncherHelper
import com.example.data.system.VolumeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Zstar Launcher`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Zstar Launcher", appName)
    }

    @Test
    fun `manifest declares separate home and launcher intent filters`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val homeIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addCategory(Intent.CATEGORY_DEFAULT)
        val homeResolved = context.packageManager.queryIntentActivities(homeIntent, 0)
        assertTrue(homeResolved.any { it.activityInfo.packageName == context.packageName })

        val launchers = DefaultLauncherHelper.getInstalledHomeLaunchers(context)
        assertTrue(launchers.any { it.isZstar })

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launcherResolved = context.packageManager.queryIntentActivities(launcherIntent, 0)
        assertTrue(launcherResolved.any { it.activityInfo.packageName == context.packageName })
    }

    @Test
    fun `default launcher request intent is valid`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = DefaultLauncherHelper.createRequestDefaultHomeIntent(context)
        assertTrue(intent.action != null)
    }

    @Test
    fun `manifest declares network and audio permissions and valid connectivity intents`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permissions = pkgInfo.requestedPermissions?.toSet().orEmpty()
        assertTrue(permissions.contains(Manifest.permission.INTERNET))
        assertTrue(permissions.contains(Manifest.permission.ACCESS_NETWORK_STATE))
        assertTrue(permissions.contains(Manifest.permission.ACCESS_WIFI_STATE))
        assertTrue(permissions.contains(Manifest.permission.CHANGE_WIFI_STATE))
        assertTrue(permissions.contains(Manifest.permission.MODIFY_AUDIO_SETTINGS))

        assertNotNull(ConnectivityRepository.buildInternetPanelIntent().action)
        assertEquals(Settings.ACTION_WIFI_SETTINGS, ConnectivityRepository.buildWifiPanelIntent().action)
        assertNotNull(ConnectivityRepository.buildMobileDataIntent().action)
        assertNotNull(VolumeRepository.buildVolumePanelIntent().action)
    }

    @Test
    fun `volume repository reads valid stream ranges and app download helper builds store intents`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val volumeRepo = VolumeRepository(context)
        val volume = volumeRepo.readCurrentVolume()
        assertTrue(volume.mediaMax >= 1)
        assertTrue(volume.ringMax >= 1)

        val searchIntent = AppDownloadHelper.buildPlayStoreSearchIntent("spotify")
        assertEquals("market://search?q=spotify&c=apps", searchIntent.dataString)

        val detailsIntent = AppDownloadHelper.buildPlayStoreDetailsIntent("com.spotify.music")
        assertEquals("market://details?id=com.spotify.music", detailsIntent.dataString)
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.spotify.music",
            AppDownloadHelper.buildPlayStoreWebDetailsIntent("com.spotify.music").dataString
        )
        assertTrue(AppDownloadHelper.featuredCatalog.isNotEmpty())
        assertTrue(com.example.model.HomeItemType.entries.contains(com.example.model.HomeItemType.ZSTAR_STORE_SHORTCUT))
    }
}
