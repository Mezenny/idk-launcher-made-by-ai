package com.example.data.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.model.VolumeStateUi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class VolumeRepository(private val context: Context) {

    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun observeVolumeState(): Flow<VolumeStateUi> = callbackFlow {
        val am = audioManager
        if (am == null) {
            trySend(VolumeStateUi())
            awaitClose { }
            return@callbackFlow
        }

        fun emitCurrent() {
            trySend(readCurrentState(am))
        }

        emitCurrent()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                emitCurrent()
            }
        }

        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }.distinctUntilChanged()

    fun readCurrentVolume(): VolumeStateUi {
        val am = audioManager ?: return VolumeStateUi()
        return readCurrentState(am)
    }

    private fun readCurrentState(am: AudioManager): VolumeStateUi {
        val mediaMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val mediaVol = am.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, mediaMax)
        val ringMax = am.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)
        val ringVol = am.getStreamVolume(AudioManager.STREAM_RING).coerceIn(0, ringMax)
        val notifMax = am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION).coerceAtLeast(1)
        val notifVol = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION).coerceIn(0, notifMax)
        val alarmMax = am.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        val alarmVol = am.getStreamVolume(AudioManager.STREAM_ALARM).coerceIn(0, alarmMax)
        val isMuted = mediaVol == 0 || runCatching {
            am.isStreamMute(AudioManager.STREAM_MUSIC)
        }.getOrDefault(false)

        return VolumeStateUi(
            mediaVolume = mediaVol,
            mediaMax = mediaMax,
            ringVolume = ringVol,
            ringMax = ringMax,
            notificationVolume = notifVol,
            notificationMax = notifMax,
            alarmVolume = alarmVol,
            alarmMax = alarmMax,
            ringerMode = am.ringerMode,
            isMediaMuted = isMuted
        )
    }

    fun setStreamVolume(streamType: Int, value: Int): VolumeStateUi? {
        val am = audioManager ?: return null
        val max = am.getStreamMaxVolume(streamType).coerceAtLeast(1)
        val target = value.coerceIn(0, max)
        runCatching {
            am.setStreamVolume(streamType, target, 0)
        }
        return readCurrentState(am)
    }

    fun toggleMediaMute(): VolumeStateUi? {
        val am = audioManager ?: return null
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        runCatching {
            if (current > 0) {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                if (am.getStreamVolume(AudioManager.STREAM_MUSIC) > 0) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                }
            } else {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                if (am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                    val restore = (am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2).coerceAtLeast(1)
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, restore, 0)
                }
            }
        }
        return readCurrentState(am)
    }

    companion object {
        fun buildVolumePanelIntent(): Intent {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_VOLUME)
            } else {
                Intent(Settings.ACTION_SOUND_SETTINGS)
            }
        }

        fun openSystemVolumePanel(context: Context): Boolean {
            val candidates = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Intent(Settings.Panel.ACTION_VOLUME))
                }
                add(Intent(Settings.ACTION_SOUND_SETTINGS))
                add(Intent(Settings.ACTION_SETTINGS))
            }
            for (intent in candidates) {
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
}
