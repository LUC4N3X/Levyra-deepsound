import json
import os
from pathlib import Path
import subprocess
import tempfile
import time
import wave


DESTINATION = 'org.mpris.MediaPlayer2.levyra'
OBJECT = '/org/mpris/MediaPlayer2'
PLAYER = 'org.mpris.MediaPlayer2.Player'


def call(method, *arguments):
    return subprocess.check_output(['gdbus', 'call', '--session', '--dest', DESTINATION,
        '--object-path', OBJECT, '--method', method, *arguments], text=True, timeout=5, stderr=subprocess.DEVNULL)


def property_value(name):
    return call('org.freedesktop.DBus.Properties.Get', PLAYER, name)


def wait_for(predicate, label, process, seconds=30):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise AssertionError(f'Application exited during {label}: {process.returncode}')
        try:
            if predicate():
                print(f'PASS: {label}', flush=True)
                return
        except (subprocess.SubprocessError, ValueError):
            pass
        time.sleep(0.2)
    raise AssertionError(f'Timeout during {label}')


def position():
    import re
    text = property_value('Position')
    match = re.search(r'int64\s+(\d+)', text)
    if not match:
        raise ValueError(text)
    return int(match.group(1))


def main():
    installed = subprocess.check_output(['dpkg-query', '-L', 'levyra'], text=True).splitlines()
    launchers = [Path(name) for name in installed if name.endswith('/bin/Levyra')]
    if len(launchers) != 1:
        raise AssertionError(f'Expected one installed launcher, got {launchers}')
    executable = launchers[0]
    if not executable.is_file():
        raise AssertionError('Installed launcher missing')
    log_path = Path('desktop/build/linux-smoke.log')
    with tempfile.TemporaryDirectory(prefix='levyra-native-') as temporary, log_path.open('w') as log:
        root = Path(temporary)
        environment = dict(os.environ, XDG_DATA_HOME=str(root / 'data'), XDG_CONFIG_HOME=str(root / 'config'))
        tracks = []
        for index, seconds in enumerate((20, 25)):
            audio = root / f'città हिन्दी {index}.wav'
            with wave.open(str(audio), 'wb') as fixture:
                fixture.setnchannels(1)
                fixture.setsampwidth(2)
                fixture.setframerate(8000)
                fixture.writeframes(bytes(8000 * 2 * seconds))
            tracks.append({'id': f'local:linux-{index}', 'title': f'Linux native fixture {index}',
                'artist': 'Fixture', 'offlinePath': str(audio), 'durationMs': seconds * 1000})
        data = root / 'data/Levyra'
        data.mkdir(parents=True)
        (data / 'settings.json').write_text(json.dumps({'globalMediaKeys': True, 'resumeOnStartup': True,
            'sponsorBlock': False, 'preloadNextTrack': False, 'autoplayRadio': False}))
        (data / 'session.json').write_text(json.dumps({'queue': tracks, 'index': 0, 'positionMs': 0}))
        process = subprocess.Popen([str(executable)], env=environment, stdout=log, stderr=subprocess.STDOUT)
        try:
            wait_for(lambda: 'Levyra' in call('org.freedesktop.DBus.Properties.Get', 'org.mpris.MediaPlayer2', 'Identity'),
                'installed application and MPRIS registration', process)
            call(f'{PLAYER}.Play')
            wait_for(lambda: position() >= 500_000, 'native WAV decoding and advancing clock', process)
            call(f'{PLAYER}.Seek', '5000000')
            wait_for(lambda: position() >= 5_000_000, 'seek', process)
            call(f'{PLAYER}.Pause')
            wait_for(lambda: 'Paused' in property_value('PlaybackStatus'), 'pause', process)
            paused = position()
            time.sleep(0.4)
            assert abs(position() - paused) < 200_000, 'Pause clock drift'
            call(f'{PLAYER}.Play')
            wait_for(lambda: position() > paused + 100_000, 'resume', process)
            call(f'{PLAYER}.Next')
            wait_for(lambda: 'Linux native fixture 1' in property_value('Metadata') and position() > 200_000,
                'automatic switch to second native track', process)
            second = subprocess.Popen([str(executable)], env=environment, stdout=log, stderr=subprocess.STDOUT)
            assert second.wait(timeout=10) == 0, 'Second launch did not forward to the existing instance'
            assert process.poll() is None, 'Original playback instance exited'
            print('PASS: second-launch forwarding during playback', flush=True)
            call('org.mpris.MediaPlayer2.Quit')
            assert process.wait(timeout=15) == 0, 'Application did not exit cleanly'
            process = subprocess.Popen([str(executable)], env=environment, stdout=log, stderr=subprocess.STDOUT)
            wait_for(lambda: 'Levyra' in call('org.freedesktop.DBus.Properties.Get', 'org.mpris.MediaPlayer2', 'Identity'),
                'relaunch after player/bridge/IPC cleanup', process)
            call('org.mpris.MediaPlayer2.Quit')
            assert process.wait(timeout=15) == 0
        finally:
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=5)


if __name__ == '__main__':
    main()
