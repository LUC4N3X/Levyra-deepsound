# FAQ & Troubleshooting

Here are quick solutions for the most common issues on Android and Windows.

## Android cannot install the APK

If Android refuses to install the package, check the following:

1. Make sure the APK came directly from the official Levyra GitHub releases or an authorized repository.
2. Ensure your browser or file manager has permission to install unknown apps in Android settings.
3. Verify that your device has enough free internal storage space.
4. If updating, confirm the build is signed with the same key as your installed version (for instance, GitHub builds cannot be installed directly over F-Droid builds without uninstalling first).

If an update fails unexpectedly, redownloading the official release package usually resolves the problem.

## Playback does not start

If a track fails to play:

- Try playing a different song to see if the issue is track-specific.
- Search for the track again to refresh any expired stream tokens.
- Check that your network connection is active and stable.
- If you use a custom DNS or proxy in settings, try disabling it temporarily.
- Restart the app.

If many unrelated tracks fail at the same time, YouTube or another upstream provider may have changed their stream formats. Check GitHub Issues to see if a fix is already in progress.

## Playback stops when the screen is off or in background

Aggressive battery-saving policies on some Android devices can terminate background audio services.

To fix this:

- Set Levyra's battery usage to "Unrestricted" in Android app settings.
- Exclude Levyra from system "sleeping apps" or memory cleaner utilities.
- On devices with custom Android skins (such as Xiaomi, Huawei, or Samsung), ensure background autostart permissions are enabled.

## Incorrect artist, album, or cover art

Levyra combines metadata from multiple providers. Occasionally, ambiguous track titles or rare releases can result in mismatched artwork or tags.

When filing a bug report for a mismatch, please provide:

- Track title, artist name, and album name
- A screenshot of what Levyra displays
- The expected artwork or metadata

## Lyrics are missing or out of sync

Lyrics are fetched from community databases like LRCLIB. Minor title differences or alternate master releases can occasionally lead to missing or unsynchronized lyrics.

- If lyrics are slightly early or late, tap the sync adjustment controls in the player to set an offset.
- If lyrics are completely wrong, report the track title and artist so the mapping can be corrected upstream.

## Downloads fail or stop midway

If downloads are not completing:

- Confirm your device has sufficient free storage space.
- Verify that Levyra has permission to write audio files to storage.
- Check that your Wi-Fi or mobile data connection is reliable.
- Ensure Android battery saver is not killing the download service in the background.

## Local audio files do not appear in the library

Ensure you have granted Levyra permission to read audio files on your device (`READ_MEDIA_AUDIO` on Android 13+ or storage permissions on older releases). Also verify that the files are in standard audio formats such as MP3, M4A, FLAC, or OGG.

## Windows Desktop fails to start

If the Windows application does not open:

1. Close any running Levyra processes in Task Manager.
2. Verify that your system has the required Visual C++ redistributables installed.
3. Try running the application as administrator once to verify file permissions.
4. Download the latest release package and extract or reinstall it.

If the problem continues, open an issue on GitHub including your Windows version and any error logs generated.

## Report a bug

Before creating a new bug report, search existing issues to see if someone has already reported the problem.

[View GitHub Issues](https://github.com/LUC4N3X/Levyra-deepsound/issues){ .md-button }
