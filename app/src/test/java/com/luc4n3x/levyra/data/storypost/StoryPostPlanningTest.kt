package com.luc4n3x.levyra.data.storypost

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryPostPlanningTest {

    private fun file(path: String, size: Long = 10L) = StoryPostSourceFile(path, size, "")

    private fun entry(name: String, size: Long = 10L, directory: Boolean = false) =
        StoryPostZipEntry(name, size, directory)

    private val smallLimits = StoryPostLimits(maxEntries = 3, maxEntryBytes = 100L, maxTotalBytes = 150L, maxCaptionBytes = 10L)

    @Test
    fun naturalOrderSortsNumbersByValueAndIgnoresCase() {
        assertTrue(compareNatural("1.jpg", "2.jpg") < 0)
        assertTrue(compareNatural("2.jpg", "10.jpg") < 0)
        assertTrue(compareNatural("IMG_002.jpg", "IMG_010.jpg") < 0)
        assertTrue(compareNatural("a.jpg", "B.jpg") < 0)
        assertTrue(compareNatural("B.jpg", "a.jpg") > 0)

        val plan = planStoryPost(listOf(file("post/10.jpg"), file("post/2.jpg"), file("post/1.jpg"), file("post/song.mp3")))
        assertEquals(listOf("1.jpg", "2.jpg", "10.jpg"), plan.images.map { it.name })

        val prefixed = planStoryPost(listOf(file("IMG_010.jpg"), file("img_002.JPG"), file("song.mp3")))
        assertEquals(listOf("img_002.JPG", "IMG_010.jpg"), prefixed.images.map { it.name })
    }

    @Test
    fun photosWithAudioAndCaptionBecomePhotosPost() {
        val plan = planStoryPost(
            listOf(
                file("notes.txt"),
                file("2.jpg"),
                file("1.jpg"),
                file("b-song.mp3"),
                file("a-song.m4a"),
                file("desc.txt"),
                file("caption.txt")
            )
        )
        assertEquals(StoryPostKind.Photos, plan.kind)
        assertNull(plan.problem)
        assertEquals("a-song.m4a", plan.defaultAudio?.name)
        assertEquals(listOf("caption.txt", "desc.txt", "notes.txt"), plan.captionCandidates.map { it.name })
        assertEquals("caption.txt", plan.defaultCaption?.name)
        assertTrue(plan.needsAudioChoice)
        assertTrue(plan.needsCaptionChoice)
        assertFalse(plan.needsVideoChoice)

        val single = planStoryPost(listOf(file("1.jpg"), file("song.mp3"), file("caption.txt")))
        assertFalse(single.needsAudioChoice)
        assertFalse(single.needsCaptionChoice)

        val twoCaptions = planStoryPost(listOf(file("1.jpg"), file("song.mp3"), file("notes.txt"), file("caption.txt")))
        assertTrue(twoCaptions.needsCaptionChoice)
        assertEquals("caption.txt", twoCaptions.defaultCaption?.name)
    }

    @Test
    fun kindAndProblemFollowTheAvailableMedia() {
        val video = planStoryPost(listOf(file("clip.mp4")))
        assertEquals(StoryPostKind.Video, video.kind)
        assertNull(video.problem)
        assertEquals("clip.mp4", video.defaultVideo?.name)

        val noAudio = planStoryPost(listOf(file("1.jpg"), file("2.png")))
        assertNull(noAudio.kind)
        assertEquals(StoryPostPlanProblem.NoAudio, noAudio.problem)

        val nothing = planStoryPost(listOf(file("readme.pdf"), file("song.mp3")))
        assertNull(nothing.kind)
        assertEquals(StoryPostPlanProblem.NoMedia, nothing.problem)
        assertEquals(StoryPostPlanProblem.NoMedia, planStoryPost(emptyList()).problem)

        val mixed = planStoryPost(listOf(file("1.jpg"), file("song.mp3"), file("clip.mp4")))
        assertEquals(StoryPostKind.Photos, mixed.kind)
        assertNull(mixed.problem)
    }

    @Test
    fun macosMetadataAndDotFilesAreIgnored() {
        val plan = planStoryPost(
            listOf(
                file("__MACOSX/post/1.jpg"),
                file("post/.DS_Store"),
                file("post/._1.jpg"),
                file("post/.hidden/3.jpg"),
                file("post/1.jpg"),
                file("post/song.mp3")
            )
        )
        assertEquals(listOf("post/1.jpg"), plan.images.map { it.path })
        assertTrue(isIgnoredStoryPostPath("__macosx/a.jpg"))
        assertTrue(isIgnoredStoryPostPath("._1.jpg"))
        assertFalse(isIgnoredStoryPostPath("post/1.jpg"))
    }

    @Test
    fun unsafeZipPathsAreRejected() {
        listOf("../evil.jpg", "/abs.jpg", "a/../b.jpg", "a\\b.jpg", "c:x.jpg", "./a.jpg").forEach { name ->
            assertEquals(name, StoryPostZipCheck.UnsafePath, checkStoryPostZipEntries(listOf(entry(name))))
        }
        assertEquals(
            StoryPostZipCheck.UnsafePath,
            checkStoryPostZipEntries(listOf(entry("post/Photo.jpg"), entry("post/photo.JPG")))
        )
    }

    @Test
    fun oversizedZipsAreRejected() {
        assertEquals(StoryPostZipCheck.TooLarge, checkStoryPostZipEntries(listOf(entry("1.jpg", 101L)), smallLimits))
        assertEquals(StoryPostZipCheck.TooLarge, checkStoryPostZipEntries(listOf(entry("1.jpg", -1L)), smallLimits))
        assertEquals(
            StoryPostZipCheck.TooLarge,
            checkStoryPostZipEntries(listOf(entry("1.jpg", 100L), entry("song.mp3", 100L)), smallLimits)
        )
        assertEquals(
            StoryPostZipCheck.TooLarge,
            checkStoryPostZipEntries(listOf(entry("1.jpg"), entry("2.jpg"), entry("3.jpg"), entry("4.jpg")), smallLimits)
        )
        assertEquals(StoryPostZipCheck.TooLarge, checkStoryPostZipEntries(listOf(entry("caption.txt", 11L)), smallLimits))
        assertTrue(checkStoryPostZipEntries(listOf(entry("caption.txt", 10L), entry("1.jpg", 100L)), smallLimits) is StoryPostZipCheck.Accepted)
    }

    @Test
    fun acceptedZipKeepsOnlyUsableFiles() {
        val check = checkStoryPostZipEntries(
            listOf(
                entry("post/", 0L, directory = true),
                entry("post/1.jpg"),
                entry("post/readme.pdf"),
                entry("post/song.mp3"),
                entry("post/caption.txt"),
                entry("__MACOSX/post/._1.jpg")
            )
        )
        assertTrue(check is StoryPostZipCheck.Accepted)
        assertEquals(
            listOf("post/1.jpg", "post/song.mp3", "post/caption.txt"),
            (check as StoryPostZipCheck.Accepted).files.map { it.name }
        )
    }

    @Test
    fun captionsAreNormalizedAndTitlesDerived() {
        assertEquals("Hello\nWorld", normalizeStoryPostCaption("﻿Hello\r\nWorld\r\n"))
        assertEquals("a\nb", normalizeStoryPostCaption("a\rb"))

        assertEquals("First line", storyPostTitle("\n   \n  First line \nSecond", "post.zip"))
        assertEquals("Holiday", storyPostTitle("", "Holiday.zip"))
        assertEquals("Holiday", storyPostTitle("  \n ", "Holiday.zip"))

        val title = storyPostTitle("a".repeat(100), "post.zip")
        assertEquals(80, title.length)
        assertEquals("a".repeat(79) + "…", title)
        assertEquals("b".repeat(80), storyPostTitle("b".repeat(80), "post.zip"))
    }

    @Test
    fun storedNamesAreStableAndSanitized() {
        assertEquals("image-001.jpg", storyPostStoredName(0, file("post/Photo.JPG")))
        assertEquals("image-012.png", storyPostStoredName(11, file("12.png")))
        assertEquals("audio.m4a", storyPostStoredName(0, file("post/Song.M4A")))
        assertEquals("video.mp4", storyPostStoredName(0, file("clip.mp4")))
        assertEquals("caption.txt", storyPostStoredName(0, file("desc.txt")))
        assertEquals("file-2.bd", storyPostStoredName(2, file("weird.b@d!")))
        assertEquals("file-0", storyPostStoredName(0, file("README")))
    }
}
