# OpenAPK icon asset

This file is intentionally separate from Levyra's Android launcher resources.

- `openapk-icon.png` is the existing round Levyra launcher icon copied from `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png`.
- It is provided specifically for the OpenAPK listing.
- Do not wire this asset into `AndroidManifest.xml`, Fastlane metadata, or the normal launcher icon resources just to change the OpenAPK page. That would affect Android launchers and/or other stores too.

The goal is to let OpenAPK use the round icon without changing Levyra's branding anywhere else.
