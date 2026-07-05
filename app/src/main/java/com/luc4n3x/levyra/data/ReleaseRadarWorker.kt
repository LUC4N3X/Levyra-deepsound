package com.luc4n3x.levyra.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.luc4n3x.levyra.LevyraLaunchActions
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.domain.FollowedArtist
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ReleaseRadarWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = FollowedArtistsStore(applicationContext)
        val followed = store.load()
        if (followed.isEmpty()) return Result.success()
        val artistRepository = ArtistRepository(YoutubeMusicRepository(applicationContext), applicationContext)
        var notified = 0
        rotatedWindow(store, followed).forEach { artist ->
            val profile = runCatching { artistRepository.profile(artist.browseId, artist.name) }
                .onFailure { Timber.w(it, "Release radar fetch failed for ${artist.name}") }
                .getOrNull() ?: return@forEach
            val releases = profile.albums + profile.singles
            if (releases.isEmpty()) return@forEach
            val keys = releases.map { releaseKey(it) }.toSet()
            if (!store.hasReleaseBaseline(artist.key)) {
                store.saveKnownReleases(artist.key, keys)
                return@forEach
            }
            val known = store.knownReleases(artist.key)
            val fresh = releases.filter { releaseKey(it) !in known }
            val notifiedKeys = mutableSetOf<String>()
            fresh.take(MAX_NOTIFICATIONS_PER_ARTIST).forEach { release ->
                if (notified < MAX_NOTIFICATIONS_PER_RUN) {
                    notifyRelease(artist, release)
                    notified++
                    notifiedKeys += releaseKey(release)
                }
            }
            store.saveKnownReleases(artist.key, known + notifiedKeys)
        }
        return Result.success()
    }

    private fun rotatedWindow(store: FollowedArtistsStore, followed: List<FollowedArtist>): List<FollowedArtist> {
        if (followed.size <= MAX_ARTISTS_PER_RUN) return followed
        val offset = store.radarOffset() % followed.size
        store.saveRadarOffset((offset + MAX_ARTISTS_PER_RUN) % followed.size)
        return (followed + followed).subList(offset, offset + MAX_ARTISTS_PER_RUN)
    }

    private fun notifyRelease(artist: FollowedArtist, release: ArtistRelease) {
        val manager = NotificationManagerCompat.from(applicationContext)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel()
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(LevyraLaunchActions.EXTRA_ARTIST, artist.name)
        }
        val requestCode = (artist.key + release.title).hashCode()
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_levyra_radar)
            .setContentTitle(applicationContext.getString(R.string.radar_new_release_title, artist.name))
            .setContentText(release.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${release.title}\n${release.subtitle}".trim()))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { manager.notify(requestCode, notification) }
            .onFailure { Timber.w(it, "Release radar notification failed") }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.radar_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = applicationContext.getString(R.string.radar_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "levyra_new_releases"
        private const val WORK_NAME = "levyra_release_radar"
        private const val MAX_ARTISTS_PER_RUN = 12
        private const val MAX_NOTIFICATIONS_PER_ARTIST = 2
        private const val MAX_NOTIFICATIONS_PER_RUN = 5

        fun releaseKey(release: ArtistRelease): String =
            release.browseId.ifBlank { "${release.title.lowercase()}|${release.year}" }

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<ReleaseRadarWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
