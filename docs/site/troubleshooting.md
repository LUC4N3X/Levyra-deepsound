# FAQ & Troubleshooting

## Android cannot install the APK

Check that:

1. the APK came from the official Levyra repository;
2. Android allows your browser or file manager to install unknown apps;
3. enough storage is available;
4. you are not trying to install an incompatible or differently signed package over the current installation.

If an update fails, downloading the current official release again is a good first step.

## A song does not start

Try:

- another track;
- searching for the track again;
- restarting playback;
- checking the network connection;
- closing and reopening Levyra.

If many unrelated tracks fail consistently, report the issue with the Levyra version and device details.

## Playback stops in the background

Android battery restrictions can interfere with long-running media playback.

Check device settings related to:

- battery optimization;
- sleeping apps;
- background activity;
- auto-start or vendor-specific power management.

## Wrong artist, album or artwork

Levyra resolves metadata from several sources. Ambiguous releases or external-provider mismatches can occasionally produce incorrect associations.

For a useful bug report include:

- track title;
- artist;
- album;
- screenshot;
- what Levyra displayed;
- what should have been displayed.

## Lyrics are missing or incorrect

Lyrics depend on compatible matches from the available lyric sources. Similar titles, alternate releases or incomplete metadata can produce a miss or incorrect match.

Report a reproducible wrong match with the exact track and artist.

## Downloads fail

Check:

- available storage;
- network connectivity;
- Android permissions;
- whether the selected media can currently be resolved;
- whether background restrictions are interrupting the operation.

## Local music is missing

Make sure Levyra has permission to read audio files and that the files use a supported format.

## Windows does not launch

Try:

1. close Levyra completely;
2. restart Windows;
3. download the latest Desktop release;
4. reinstall or extract the package again.

If the problem continues, include the Windows version and Levyra Desktop version in the report.

## Report a bug

Before opening a new issue, search existing reports.

[View GitHub Issues](https://github.com/LUC4N3X/Levyra-deepsound/issues){ .md-button }
