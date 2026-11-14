from pathlib import Path
import hashlib
import re
import shutil
import subprocess
import tarfile
import tempfile


def prepare(root: Path) -> None:
    version = (root / 'desktop/version.properties').read_text().strip().split('=', 1)[1]
    if not re.fullmatch(r'\d+\.\d+\.\d+', version):
        raise ValueError('Invalid Desktop package version')
    binaries = root / 'desktop/app/build/compose/binaries/main-release'
    output = root / 'desktop/build/linux-assets'
    output.mkdir(parents=True, exist_ok=True)
    deb = next((binaries / 'deb').glob('*.deb'))
    deb_target = output / f'LEVYRA-Linux-{version}-x64.deb'
    with tempfile.TemporaryDirectory(prefix='levyra-deb-') as temporary:
        subprocess.run(['dpkg-deb', '--raw-extract', str(deb), temporary], check=True)
        control = Path(temporary) / 'DEBIAN/control'
        text = control.read_text()
        dependencies = re.search(r'(?m)^Depends:[^\n]*(?:\n[ \t][^\n]*)*', text)
        if dependencies:
            folded = re.sub(r'\n[ \t]+', ' ', dependencies.group()).rstrip(', ')
            text = text[:dependencies.start()] + folded + ', libvlc5, vlc-plugin-base, fonts-noto-core, fonts-noto-cjk' + text[dependencies.end():]
        else:
            text = 'Depends: libvlc5, vlc-plugin-base, fonts-noto-core, fonts-noto-cjk\n' + text
        control.write_text(text)
        subprocess.run(['dpkg-deb', '--root-owner-group', '--build', temporary, str(deb_target)], check=True)
    rpm_target = output / f'LEVYRA-Linux-{version}-x64.rpm'
    shutil.copy2(next((binaries / 'rpm').glob('*.rpm')), rpm_target)
    image = binaries / 'app/Levyra'
    if not (image / 'bin/Levyra').is_file():
        raise RuntimeError('Linux app image missing')
    archive = output / f'LEVYRA-Linux-{version}-portable-x64.tar.gz'
    with tarfile.open(archive, 'w:gz') as package:
        package.add(image, arcname='Levyra')
    for artifact in (deb_target, rpm_target, archive):
        with artifact.open('rb') as source:
            digest = hashlib.file_digest(source, 'sha256').hexdigest()
        artifact.with_name(artifact.name + '.sha256').write_text(f'{digest}  {artifact.name}', encoding='ascii')
        print(f'{artifact.name}: {artifact.stat().st_size} bytes, sha256={digest}')


if __name__ == '__main__':
    prepare(Path(__file__).resolve().parents[3])
