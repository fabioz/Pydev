'''
This is a helper to make PyDev releases.

Requires:

pip install fire

to work.

Note: paths are hardcoded for the machine doing the builds, it's windows-only
and relies on additional apps installed in the system.
'''
import fire
import os
from typing import Sequence, Callable
from pathlib import Path
import sys
import subprocess

_VERSION = '11.0.3'

_pydev_root = Path(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def _call(cmdline: str | Sequence[str]):
    import subprocess
    if not isinstance(cmdline, (list, tuple)):
        cmdline = cmdline.split(' ')
    subprocess.check_call(cmdline)


def _create_temp(contents: str, suffix: str) -> str:
    import tempfile
    with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as batch_file:
        batch_file_path = batch_file.name
        # Write batch commands to the temporary file
        batch_file.write(contents.encode('utf-8'))
    return batch_file_path


def _cmd(batch_commands:str, cwd=None):
    import subprocess

    batch_file_path = _create_temp(batch_commands, ".bat")
    try:
        subprocess.run(['cmd', '/c', batch_file_path], input=b'\n', check=True, cwd=cwd)
    finally:
        os.remove(batch_file_path)


def update_pydevd_bins():
    '''
    Make sure debugger builtins are updated and commited

    ## TODO: Get debugger binaries from modules in pypi (either during build or when
    ## the user starts up PyDev) so that the step below is not needed.
    '''
    pysrc = _pydev_root / 'plugins' / 'org.python.pydev.core' / 'pysrc'

    _cmd(f'''
set FORCE_PYDEVD_VC_VARS=C:/Program Files (x86)/Microsoft Visual Studio/2017/BuildTools/VC/Auxiliary/Build/vcvars64.bat
set PYTHONPATH={pysrc}
python build_tools/build.py
''', cwd=pysrc)


def update_version_in_homepage():
    # Add to build_homepage
    build_py = Path('X:/liclipsews/pydev.page/build_homepage.py')
    contents = build_py.read_text(encoding='utf-8')
    lines = contents.splitlines()
    iter_in = iter(lines)
    for i, line in enumerate(iter_in):
        if line.strip() == 'update_site_versions = [':
            next_line = next(iter_in)

            expected = f"    '{_VERSION}',"
            if next_line.strip() != expected.strip():
                lines.insert(i + 1, expected)
                print('CHANGED', build_py)
                build_py.write_text('\n'.join(lines), 'utf-8')
            break
    else:
        write_and_exit(f'Unable to find "update_site_versions = [" in {build_py}')

    # Add to pydev.page/homepage/update_sites/index.contents.rst_html
    contents_path = Path('X:/liclipsews/pydev.page/homepage/update_sites/index.contents.rst_html')

    contents = contents_path.read_text(encoding='utf-8')
    lines = contents.splitlines()
    iter_in = iter(lines)
    add = f'    <li><a href="http://www.pydev.org/update_sites/{_VERSION}">http://www.pydev.org/update_sites/{_VERSION}</a></li>'
    search = '<li><a href="http://www.pydev.org/update_sites/'

    for i, line in enumerate(iter_in):
        if line.strip().startswith(search):
            if _VERSION not in line:
                lines.insert(i, add)
                print('CHANGED', contents_path)
                contents_path.write_text('\n'.join(lines), 'utf-8')

            break
    else:
        write_and_exit(f'Unable to find {search} in {contents_path}')


def update_version():
    _call(f'python update_version.py {_VERSION}')


def update_typeshed():
    typeshed_dir = _pydev_root / 'plugins' / 'org.python.pydev.core' / 'typeshed'
    _cmd(f'''
@echo off
echo Remote Info
git remote -v
echo Fetch/Rebase
git fetch
git rebase origin/main
echo ---
echo Updated typeshed at: {typeshed_dir}.
echo Commit on PyDev is now required:
echo i.e.:
echo mu ac Updated typeshed
''', typeshed_dir)


def write_and_exit(msg):
    sys.stderr.write(f'{msg}\n')
    sys.exit(1)


def build_pydev_in_build_dir():
    if 'SIGN_KEYSTORE' not in os.environ:
        write_and_exit('SIGN_KEYSTORE not defined in the os.environ.')

    BUILD_DIR = 'X:/pydev_build/build_dir'
    BASE_LOCAL_PYDEV_GIT = 'X:/liclipsews/liclipsews/Pydev'  # git://github.com/fabioz/Pydev.git could be used to build with the repo.
    JAVA_HOME = 'D:/bin/jdk-17.0.8.1+1'
    MAVEN_BIN = 'X:/liclipsews/maven/apache-maven-3.9.5/bin'

    os.makedirs(BUILD_DIR, exist_ok=True)
    _cmd(rf'''
@echo off
set BRANCH=master

set JAVA_HOME={JAVA_HOME}
set MAVEN_BIN={MAVEN_BIN}
set BUILD_DIR={BUILD_DIR}
SET MAVEN_OPTS=-Xmx1024m

set PATH=%MAVEN_BIN%;%PATH%
set PATH=%JAVA_HOME%/bin;%PATH%
set PATH="C:/Program Files/Git/bin/";%PATH%

cd %BUILD_DIR%
git clone {BASE_LOCAL_PYDEV_GIT}
cd Pydev
git reset --hard
git clean -f -d -x
git checkout -f
git remote update
git fetch
git checkout %BRANCH%
git pull origin %BRANCH%
git submodule foreach --recursive git reset --hard
git submodule foreach --recursive git clean -f -d -x
git submodule update --init --recursive

set PYTHONPATH=%BUILD_DIR%/Pydev/plugins/org.python.pydev.core/pysrc
set FORCE_PYDEVD_VC_VARS=C:/Program Files (x86)/Microsoft Visual Studio/2017/BuildTools/VC/Auxiliary/Build/vcvars64.bat
python %BUILD_DIR%/Pydev/plugins/org.python.pydev.core/pysrc/build_tools/build.py
python %BUILD_DIR%/Pydev/plugins/org.python.pydev.core/pysrc/build_tools/build_binaries_windows.py

mvn install -Dsign-release=true
''', cwd=BUILD_DIR)


def copy_and_zips():
    # Copy features and plugins folders
    # from
    # X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\repository
    # to
    # X:\pydev_build\update_site_dir\updates
    _cmd(r'''
copy X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\repository\features\*.jar X:\pydev_build\update_site_dir\updates\features /Y
copy X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\repository\plugins\*.jar X:\pydev_build\update_site_dir\updates\plugins /Y
''')

    _cmd(
        f'"C:/Program Files/7-Zip/7z" a -tzip -r "PyDev {_VERSION}.zip" features plugins',
        cwd='X:/pydev_build/build_dir/pydev/features/org.python.pydev.p2-repo/target/runnable'
    )

    _cmd(
        f'git archive -o "X:/pydev_build/build_dir/pydev/features/org.python.pydev.p2-repo/target/runnable/PyDev {_VERSION}-sources.zip" origin/master',
        cwd='X:/pydev_build/build_dir/pydev'
    )


def copy_zips_to_sf():
    script = rf'''
open sftp://fabioz%2Cpydev@frs.sourceforge.net/ -hostkey="ssh-ed25519 255 209BDmH3jsRyO9UeGPPgLWPSegKmYCBIya0nR/AWWCY="
mkdir "/home/pfs/project/p/py/pydev/pydev/PyDev {_VERSION}/"
put "X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\runnable\PyDev {_VERSION}.zip" "/home/pfs/project/p/py/pydev/pydev/PyDev {_VERSION}/PyDev {_VERSION}.zip"
put "X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\runnable\PyDev {_VERSION}-sources.zip" "/home/pfs/project/p/py/pydev/pydev/PyDev {_VERSION}/PyDev {_VERSION}-sources.zip"
exit
'''
    temp_file = _create_temp(script, '.txt')
    try:
        dirname = os.path.dirname(temp_file)
        basename = os.path.basename(temp_file)
        _cmd(f"winscp.com.exe /script={basename} /log=winscp_log.txt", cwd=dirname)
    finally:
        os.remove(temp_file)

    import webbrowser
    webbrowser.open(f'https://sourceforge.net/projects/pydev/files/pydev/{_VERSION}/')


def add_to_update_site_mirror():
    base = Path(r'X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target')
    snapshot = base / rf'org.python.pydev.p2-repo-{_VERSION}-SNAPSHOT'
    snapshot.mkdir(exist_ok=True)
    print(f'Creating .zip of snapshot ({snapshot})')
    _cmd(rf'"C:\Program Files\7-Zip\7z" x ..\org.python.pydev.p2-repo-{_VERSION}-SNAPSHOT.zip', cwd=snapshot)

    print(f'Pushing to surge')
    _cmd(rf'''C:\Users\fabio\AppData\Roaming\npm\surge.cmd --domain {_VERSION.replace('.', '-')}.surge.sh''', cwd=snapshot)

    print(f'Pushing to sourceforge')
    script = rf'''
open sftp://fabioz%2Cpydev@frs.sourceforge.net/ -hostkey="ssh-ed25519 255 209BDmH3jsRyO9UeGPPgLWPSegKmYCBIya0nR/AWWCY="
mkdir "/home/project-web/pydev/htdocs/pydev_update_site/{_VERSION}/"
synchronize remote "X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\org.python.pydev.p2-repo-{_VERSION}-SNAPSHOT" "/home/project-web/pydev/htdocs/pydev_update_site/{_VERSION}"
exit
'''
    temp_file = _create_temp(script, '.txt')
    try:
        dirname = os.path.dirname(temp_file)
        basename = os.path.basename(temp_file)
        _cmd(f"winscp.com.exe /script={basename} /log=winscp_log.txt", cwd=dirname)
    finally:
        os.remove(temp_file)


def add_to_github():
    env = {
        'CONVERT_SOURCE':rf'X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\org.python.pydev.p2-repo-{_VERSION}-SNAPSHOT',
        'CONVERT_FINAL_ZIP':rf'X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\runnable\PyDev {_VERSION}.zip',
        'CONVERT_TARGET_DIR':rf'X:\pydev_build\build_dir\pydev\features\org.python.pydev.p2-repo\target\github',
    }
    subprocess.check_call([sys.executable, r'X:\release_tools\convert_to_github.py', _VERSION], env=env)
    import webbrowser
    webbrowser.open(rf'''https://github.com/fabioz/Pydev/releases/new?tag=pydev_{_VERSION.replace('.', '_')}''')


def _gen_fire_args() -> dict[str, Callable]:
    """Generate a dict of arguments to be called by fire.Fire()"""
    globals_ = globals()
    all_modules = set(sys.modules)
    imported_modules = all_modules & set(globals_)
    other_constants = set(globals_) - imported_modules

    return {
        name: callable
        for name in other_constants
        if not name.startswith("_")  # Exclude private constants
        and isinstance((callable := globals_[name]), Callable)  # Exclude non-callables
        and callable.__module__ == "__main__"  # Exclude imported callables
    }


if __name__ == "__main__":
    # Everything should be relative to /PyDev.
    os.chdir(_pydev_root)
    # Make Python Fire not use a pager when it prints a help text
    fire.core.Display = lambda lines, out: out.write("\n".join(lines) + "\n")
    fire.Fire(_gen_fire_args())
