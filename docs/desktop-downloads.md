# Download Levyra Desktop

Windows and Linux run the same Desktop app: the same interface, 42 languages,
preferred 320 kbps audio, optional FLAC, library, downloads, lyrics and queue.

| System | Download | Installation |
|---|---|---|
| Windows 11 / 10, x64 | [Windows 1.3.0](https://github.com/LUC4N3X/Levyra-deepsound/releases/tag/desktop-v1.3.0) | MSI or EXE installer; ZIP for portable use |
| Ubuntu / Debian, x64 | [Linux builds](https://github.com/LUC4N3X/Levyra-deepsound/actions?query=workflow%3A%22Desktop+Linux+Build%22) | Download the DEB from the `Levyra-Linux-x64` artifact; install with `sudo apt install ./LEVYRA-Linux-*-x64.deb` |
| Fedora / RPM-based Linux, x64 | [Linux builds](https://github.com/LUC4N3X/Levyra-deepsound/actions?query=workflow%3A%22Desktop+Linux+Build%22) | Install VLC, then the RPM through the system package manager |
| Other Linux desktops, x64 | [Portable Linux builds](https://github.com/LUC4N3X/Levyra-deepsound/actions?query=workflow%3A%22Desktop+Linux+Build%22) | Install VLC, extract the TAR.GZ, then run `Levyra/bin/Levyra` |

Linux packages are initially distributed as workflow artifacts; a GitHub login is
required to download these artifacts. Published Linux releases will use
`desktop-linux-v<version>` tags independently of Windows and Android.

Each package includes a SHA-256 sidecar. The portable builds include their Java
runtime and require system libVLC. Linux desktop media controls use MPRIS/D-Bus;
tray availability depends on the desktop environment. Without a supported tray,
closing the window exits the app rather than leaving an inaccessible window.

See [Desktop setup and build instructions](../desktop/README.md) for details.
