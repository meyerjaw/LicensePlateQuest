package com.getmecookies.licenseplatequest.data.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.data.repository.SettingsRepository
import com.getmecookies.licenseplatequest.domain.CelebrationSounds

/**
 * [CelebrationSounds] backed by [SoundPool], playing the bundled `res/raw` chimes on the media
 * stream. Playback is suppressed when the user's **sound setting** is off, when the device is in
 * silent/vibrate mode, or (implicitly, via the media stream) when media volume is zero — so it
 * never blares unexpectedly. Failures are swallowed; sound is a nice-to-have.
 */
class SoundPoolCelebrationSounds(
    context: Context,
    private val settingsRepository: SettingsRepository,
) : CelebrationSounds {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    // SoundPool.load is async; play() is a no-op until loading completes, which is fine.
    private val findSound = pool.load(appContext, R.raw.sfx_find, 1)
    private val rareSound = pool.load(appContext, R.raw.sfx_rare, 1)
    private val fiftySound = pool.load(appContext, R.raw.sfx_fifty, 1)

    override fun playFind() = play(findSound)

    override fun playRare() = play(rareSound)

    override fun playFifty() = play(fiftySound)

    private fun play(soundId: Int) {
        if (!settingsRepository.soundEnabled.value) return
        // Courtesy: stay quiet when the phone is silenced (the media stream still carries volume).
        if (audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        runCatching { pool.play(soundId, 1f, 1f, 1, 0, 1f) }
    }
}
