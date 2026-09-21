package com.luc4n3x.levyra.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
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
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.TimeUnit

class ReleaseRadarWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val preferences = LevyraPreferences(applicationContext)
        if (!preferences.interfaceSettings().releaseNotificationsEnabled) return Result.success()
        val store = FollowedArtistsStore(applicationContext)
        val followed = store.loadOrNull() ?: return Result.retry()
        if (followed.isEmpty()) return Result.success()
        val artistRepository = ArtistRepository(YoutubeMusicRepository(applicationContext), applicationContext)
        val scanResults = rotatedWindow(store, followed).map { artist ->
            scanArtist(store, artistRepository, artist)
        }
        val successfulFetches = scanResults.count { it != null }
        val failedFetches = scanResults.count { it == null }
        if (ReleaseRadarPolicy.shouldRetryFetches(successfulFetches, failedFetches, runAttemptCount)) {
            return Result.retry()
        }
        if (!preferences.interfaceSettings().releaseNotificationsEnabled) return Result.success()
        val stillFollowed = scanResults
            .filterNotNull()
            .flatten()
            .filter { store.containsOrNull(it.artist) == true }
            .distinctBy { ReleaseRadarWorker.releaseKey(it.release) }
            .take(MAX_NOTIFICATIONS_PER_RUN)
        if (ReleaseNotificationCoordinator(applicationContext).notify(stillFollowed)) {
            persistDeliveredReleases(store, stillFollowed)
        }
        return Result.success()
    }

    private suspend fun scanArtist(
        store: FollowedArtistsStore,
        artistRepository: ArtistRepository,
        artist: FollowedArtist
    ): List<FollowedArtistRelease>? {
        val profile = try {
            artistRepository.profile(artist.browseId, artist.name)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Release radar fetch failed for ${artist.name}")
            return null
        } ?: return emptyList()
        val releases = profile.albums + profile.singles
        if (releases.isEmpty() || store.containsOrNull(artist) != true) return emptyList()
        val keys = releases.flatMapTo(linkedSetOf(), ReleaseRadarPolicy::identityKeys)
        if (!store.hasReleaseBaseline(artist.key)) {
            store.saveKnownReleases(artist.key, keys)
            if (store.containsOrNull(artist) == false) store.clearKnownReleases(artist.key)
            return emptyList()
        }
        val known = store.knownReleases(artist.key)
        val fresh = ReleaseRadarPolicy.freshReleases(releases, known)
        if (store.containsOrNull(artist) != true) return emptyList()
        val pendingKeys = fresh.flatMapTo(linkedSetOf(), ReleaseRadarPolicy::identityKeys)
        store.saveKnownReleases(artist.key, keys + known - pendingKeys)
        if (store.containsOrNull(artist) == false) store.clearKnownReleases(artist.key)
        return fresh.take(MAX_NOTIFICATIONS_PER_ARTIST).map { FollowedArtistRelease(artist, it) }
    }

    private suspend fun persistDeliveredReleases(
        store: FollowedArtistsStore,
        delivered: List<FollowedArtistRelease>
    ) {
        delivered.groupBy { it.artist.key }.values.forEach { artistReleases ->
            val artist = artistReleases.first().artist
            if (store.containsOrNull(artist) != true) return@forEach
            val deliveredKeys = artistReleases
                .flatMapTo(linkedSetOf()) { ReleaseRadarPolicy.identityKeys(it.release) }
            store.saveKnownReleases(artist.key, deliveredKeys + store.knownReleases(artist.key))
            if (store.containsOrNull(artist) == false) store.clearKnownReleases(artist.key)
        }
    }

    private fun rotatedWindow(store: FollowedArtistsStore, followed: List<FollowedArtist>): List<FollowedArtist> {
        if (followed.size <= MAX_ARTISTS_PER_RUN) return followed
        val offset = store.radarOffset() % followed.size
        store.saveRadarOffset((offset + MAX_ARTISTS_PER_RUN) % followed.size)
        return (followed + followed).subList(offset, offset + MAX_ARTISTS_PER_RUN)
    }

    companion object {
        const val CHANNEL_ID = "levyra_new_releases"
        private const val WORK_NAME = "levyra_release_radar"
        private const val MAX_ARTISTS_PER_RUN = 12
        private const val MAX_NOTIFICATIONS_PER_ARTIST = 2
        private const val MAX_NOTIFICATIONS_PER_RUN = 5

        fun releaseKey(release: ArtistRelease): String =
            release.browseId.ifBlank { "${release.title.lowercase(Locale.ROOT)}|${release.year}" }

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<ReleaseRadarWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(30, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
        }
    }
}

internal data class FollowedArtistRelease(
    val artist: FollowedArtist,
    val release: ArtistRelease
)

internal object ReleaseRadarPolicy {
    private val combiningMarks = Regex("\\p{M}+")
    private val editionSuffix = Regex(
        """\b(?:deluxe|expanded|anniversary|remaster(?:ed)?|reissue|bonus(?:\s+track)?\s+edition)\b.*$""",
        RegexOption.IGNORE_CASE
    )
    private val punctuation = Regex("[^\\p{L}\\p{N}]+")

    fun identityKeys(release: ArtistRelease): Set<String> = linkedSetOf(
        ReleaseRadarWorker.releaseKey(release),
        "fingerprint:${fingerprint(release)}"
    )

    fun freshReleases(releases: List<ArtistRelease>, knownKeys: Set<String>): List<ArtistRelease> =
        releases
            .distinctBy(::fingerprint)
            .filter { release -> identityKeys(release).none(knownKeys::contains) }

    fun shouldRetryFetches(successful: Int, failed: Int, runAttemptCount: Int): Boolean =
        successful == 0 && failed > 0 && runAttemptCount < 2

    private fun fingerprint(release: ArtistRelease): String {
        val normalizedTitle = Normalizer.normalize(release.title, Normalizer.Form.NFKD)
            .replace(combiningMarks, "")
            .replace(editionSuffix, "")
            .lowercase(Locale.ROOT)
            .replace(punctuation, " ")
            .trim()
        return listOf(
            normalizedTitle.ifBlank { ReleaseRadarWorker.releaseKey(release) },
            release.year.trim(),
            release.releaseType.name
        ).joinToString("|")
    }
}

private class ReleaseNotificationCoordinator(context: Context) {
    private val appContext = context.applicationContext

    fun notify(candidates: List<FollowedArtistRelease>): Boolean {
        val releases = candidates.distinctBy { ReleaseRadarWorker.releaseKey(it.release) }
        if (releases.isEmpty()) return false
        val manager = NotificationManagerCompat.from(appContext)
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        ensureChannel()
        val first = releases.first()
        val intent = Intent(appContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(LevyraLaunchActions.EXTRA_ARTIST, first.artist.name)
            putExtra(LevyraLaunchActions.EXTRA_RELEASE_ID, first.release.browseId)
            putExtra(LevyraLaunchActions.EXTRA_RELEASE_TITLE, first.release.title)
            putExtra(LevyraLaunchActions.EXTRA_RELEASE_ARTIST, first.artist.name)
            putExtra(LevyraLaunchActions.EXTRA_RELEASE_ARTWORK, first.release.thumbnailUrl)
            putExtra(LevyraLaunchActions.EXTRA_RELEASE_YEAR, first.release.year)
        }
        val requestCode = (first.artist.key + "|" + ReleaseRadarWorker.releaseKey(first.release)).hashCode()
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val strings = LevyraStrings.forCode(LevyraPreferences(appContext).languageCode())
        val lines = releases.map { "${it.artist.name} — ${it.release.title}" }
        val title = if (releases.size == 1) {
            appContext.getString(R.string.radar_new_release_title, first.artist.name)
        } else {
            "${releases.size} ${strings.newReleases}"
        }
        val style = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText(strings.releaseRadar)
        lines.forEach { style.addLine(it) }
        val notification = NotificationCompat.Builder(appContext, ReleaseRadarWorker.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_levyra_radar)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(style)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return runCatching { manager.notify(NOTIFICATION_ID, notification) }
            .onFailure { Timber.w(it, "Release radar notification failed") }
            .isSuccess
    }

    private fun ensureChannel() {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            ReleaseRadarWorker.CHANNEL_ID,
            appContext.getString(R.string.radar_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = appContext.getString(R.string.radar_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val NOTIFICATION_ID = 0x4c525244
    }
}
