# coding: utf-8
import os
import sys

import pytest

from _pydev_bundle.pydev_monkey import pydev_src_dir
from _pydevd_bundle.pydevd_constants import sorted_dict_repr
from pydevd import SetupHolder
from tests_python.debug_constants import IS_PY2

try:
    from _pydev_bundle import pydev_monkey
except:
    sys.path.append(os.path.dirname(os.path.dirname(__file__)))
    from _pydev_bundle import pydev_monkey


def test_monkey():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0', 'ppid': os.getpid(), 'protocol-quoted-line': True}
        check = '''C:\\bin\\python.exe -u -c connect(\\"127.0.0.1\\")'''
        debug_command = (
            'import sys; '
            'sys.path.insert(0, r\'%s\'); '
            "import pydevd; pydevd.PydevdCustomization.DEFAULT_PROTOCOL='quoted-line'; "
            "pydevd.settrace(host='127.0.0.1', port=0, suspend=False, "
            'trace_only_current_thread=False, patch_multiprocessing=True, access_token=None, client_access_token=None, __setup_holder__=%s); '
            ''
            'connect("127.0.0.1")') % (pydev_src_dir, sorted_dict_repr(SetupHolder.setup))
        if sys.platform == "win32":
            debug_command = debug_command.replace('"', '\\"')
            debug_command = '"%s"' % debug_command

        assert 'C:\\bin\\python.exe -u -c %s' % debug_command == pydev_monkey.patch_arg_str_win(check)
    finally:
        SetupHolder.setup = original


def test_str_to_args_windows():
    assert ['a', 'b'] == pydev_monkey.str_to_args_windows('a "b"')


def test_monkey_patch_return_original_args():
    check = ['echo', '"my"', '"args"']
    res = pydev_monkey.patch_args(check[:])
    assert res == check


def test_monkey_patch_args_indc():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0', 'ppid': os.getpid(), 'protocol-quoted-line': True, 'skip-notify-stdin': True}
        check = ['C:\\bin\\python.exe', '-u', '-c', 'connect("127.0.0.1")']
        debug_command = (
            "import sys; sys.path.insert(0, r\'%s\'); import pydevd; pydevd.PydevdCustomization.DEFAULT_PROTOCOL='quoted-line'; "
            'pydevd.settrace(host=\'127.0.0.1\', port=0, suspend=False, trace_only_current_thread=False, patch_multiprocessing=True, access_token=None, client_access_token=None, __setup_holder__=%s); '
            ''
            'connect("127.0.0.1")') % (pydev_src_dir, sorted_dict_repr(SetupHolder.setup))
        if sys.platform == "win32":
            debug_command = debug_command.replace('"', '\\"')
            debug_command = '"%s"' % debug_command
        res = pydev_monkey.patch_args(check)
        assert res == [
            'C:\\bin\\python.exe',
            '-u',
            '-c',
            debug_command
        ]
    finally:
        SetupHolder.setup = original


def test_monkey_patch_args_indc2():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0', 'ppid': os.getpid(), 'protocol-quoted-line': True, 'skip-notify-stdin': True}
        check = ['C:\\bin\\python.exe', '-u', '-qcconnect("127.0.0.1")']
        debug_command = (
            "import sys; sys.path.insert(0, r\'%s\'); import pydevd; pydevd.PydevdCustomization.DEFAULT_PROTOCOL='quoted-line'; "
            'pydevd.settrace(host=\'127.0.0.1\', port=0, suspend=False, trace_only_current_thread=False, patch_multiprocessing=True, access_token=None, client_access_token=None, __setup_holder__=%s); '
            ''
            'connect("127.0.0.1")') % (pydev_src_dir, sorted_dict_repr(SetupHolder.setup))
        if sys.platform == "win32":
            debug_command = debug_command.replace('"', '\\"')
            debug_command = '"%s"' % debug_command
        res = pydev_monkey.patch_args(check)
        assert res == [
            'C:\\bin\\python.exe',
            '-u',
            '-qc',
            debug_command
        ]
    finally:
        SetupHolder.setup = original


def test_monkey_patch_args_x_flag():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0', 'ppid': os.getpid(), 'protocol-quoted-line': True, 'skip-notify-stdin': True}
        check = ['C:\\bin\\python.exe', '-X', 'faulthandler', '-c', 'connect("127.0.0.1")']
        debug_command = (
            "import sys; sys.path.insert(0, r\'%s\'); import pydevd; pydevd.PydevdCustomization.DEFAULT_PROTOCOL='quoted-line'; "
            'pydevd.settrace(host=\'127.0.0.1\', port=0, suspend=False, trace_only_current_thread=False, patch_multiprocessing=True, access_token=None, client_access_token=None, __setup_holder__=%s); '
            ''
            'connect("127.0.0.1")') % (pydev_src_dir, sorted_dict_repr(SetupHolder.setup))
        if sys.platform == "win32":
            debug_command = debug_command.replace('"', '\\"')
            debug_command = '"%s"' % debug_command
        res = pydev_monkey.patch_args(check)
        assert res == [
            'C:\\bin\\python.exe',
            '-X',
            'faulthandler',
            '-c',
            debug_command
        ]
    finally:
        SetupHolder.setup = original


def test_monkey_patch_args_flag_in_single_arg_1():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0', 'ppid': os.getpid(), 'protocol-quoted-line': True, 'skip-notify-stdin': True}
        check = ['C:\\bin\\python.exe', '-qX', 'faulthandler', '-c', 'connect("127.0.0.1")']
        debug_command = (
            "import sys; sys.path.insert(0, r\'%s\'); import pydevd; pydevd.PydevdCustomization.DEFAULT_PROTOCOL='quoted-line'; "
            'pydevd.settrace(host=\'127.0.0.1\', port=0, suspend=False, trace_only_current_thread=False, patch_multiprocessing=True, access_token=None, client_access_token=None, __setup_holder__=%s); '
            ''
            'connect("127.0.0.1")') % (pydev_src_dir, sorted_dict_repr(SetupHolder.setup))
        if sys.platform == "win32":
            debug_command = debug_command.replace('"', '\\"')
            debug_command = '"%s"' % debug_command
        res = pydev_monkey.patch_args(check)
        assert res == [
            'C:\\bin\\python.exe',
            '-qX',
            'faulthandler',
            '-c',
            debug_command
        ]
    finally:
        SetupHolder.setup = original


def test_monkey_patch_args_flag_in_single_arg_2():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0', 'ppid': os.getpid(), 'protocol-quoted-line': True, 'skip-notify-stdin': True}
        check = ['C:\\bin\\python.exe', '-qX', 'faulthandler', '-c', 'connect("127.0.0.1")']
        debug_command = (
            "import sys; sys.path.insert(0, r\'%s\'); import pydevd; pydevd.PydevdCustomization.DEFAULT_PROTOCOL='quoted-line'; "
            'pydevd.settrace(host=\'127.0.0.1\', port=0, suspend=False, trace_only_current_thread=False, patch_multiprocessing=True, access_token=None, client_access_token=None, __setup_holder__=%s); '
            ''
            'connect("127.0.0.1")') % (pydev_src_dir, sorted_dict_repr(SetupHolder.setup))
        if sys.platform == "win32":
            debug_command = debug_command.replace('"', '\\"')
            debug_command = '"%s"' % debug_command
        res = pydev_monkey.patch_args(check)
        assert res == [
            'C:\\bin\\python.exe',
            '-qX',
            'faulthandler',
            '-c',
            debug_command
        ]
    finally:
        SetupHolder.setup = original


def test_monkey_patch_args_flag_in_single_arg_3():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0', 'ppid': os.getpid(), 'protocol-quoted-line': True, 'skip-notify-stdin': True}
        check = ['C:\\bin\\python.exe', '-qc', 'connect("127.0.0.1")']
        debug_command = (
            "import sys; sys.path.insert(0, r\'%s\'); import pydevd; pydevd.PydevdCustomization.DEFAULT_PROTOCOL='quoted-line'; "
            'pydevd.settrace(host=\'127.0.0.1\', port=0, suspend=False, trace_only_current_thread=False, patch_multiprocessing=True, access_token=None, client_access_token=None, __setup_holder__=%s); '
            ''
            'connect("127.0.0.1")') % (pydev_src_dir, sorted_dict_repr(SetupHolder.setup))
        if sys.platform == "win32":
            debug_command = debug_command.replace('"', '\\"')
            debug_command = '"%s"' % debug_command
        res = pydev_monkey.patch_args(check)
        assert res == [
            'C:\\bin\\python.exe',
            '-qc',
            debug_command
        ]
    finally:
        SetupHolder.setup = original


def test_monkey_patch_args_x_flag_inline():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0', 'ppid': os.getpid(), 'protocol-quoted-line': True, 'skip-notify-stdin': True}
        check = ['C:\\bin\\python.exe', '-Xfaulthandler', '-c', 'connect("127.0.0.1")', 'arg1']
        debug_command = (
            "import sys; sys.path.insert(0, r\'%s\'); import pydevd; pydevd.PydevdCustomization.DEFAULT_PROTOCOL='quoted-line'; "
            'pydevd.settrace(host=\'127.0.0.1\', port=0, suspend=False, trace_only_current_thread=False, patch_multiprocessing=True, access_token=None, client_access_token=None, __setup_holder__=%s); '
            ''
            'connect("127.0.0.1")') % (pydev_src_dir, sorted_dict_repr(SetupHolder.setup))
        if sys.platform == "win32":
            debug_command = debug_command.replace('"', '\\"')
            debug_command = '"%s"' % debug_command
        res = pydev_monkey.patch_args(check)
        assert res == [
            'C:\\bin\\python.exe',
            '-Xfaulthandler',
            '-c',
            debug_command,
            'arg1'
        ]
    finally:
        SetupHolder.setup = original


def test_monkey_patch_args_c_flag_inline():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0', 'ppid': os.getpid(), 'protocol-quoted-line': True, 'skip-notify-stdin': True}
        check = ['C:\\bin\\python.exe', '-X', 'faulthandler', '-cconnect("127.0.0.1")', 'arg1']
        debug_command = (
            "import sys; sys.path.insert(0, r\'%s\'); import pydevd; pydevd.PydevdCustomization.DEFAULT_PROTOCOL='quoted-line'; "
            'pydevd.settrace(host=\'127.0.0.1\', port=0, suspend=False, trace_only_current_thread=False, patch_multiprocessing=True, access_token=None, client_access_token=None, __setup_holder__=%s); '
            ''
            'connect("127.0.0.1")') % (pydev_src_dir, sorted_dict_repr(SetupHolder.setup))
        if sys.platform == "win32":
            debug_command = debug_command.replace('"', '\\"')
            debug_command = '"%s"' % debug_command
        res = pydev_monkey.patch_args(check)
        assert res == [
            'C:\\bin\\python.exe',
            '-X',
            'faulthandler',
            '-c',
            debug_command,
            'arg1'
        ]
    finally:
        SetupHolder.setup = original


def test_monkey_patch_args_module():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0', 'multiprocess': True, 'skip-notify-stdin': True}
        check = ['C:\\bin\\python.exe', '-m', 'test']
        from _pydevd_bundle.pydevd_command_line_handling import get_pydevd_file
        assert pydev_monkey.patch_args(check) == [
            'C:\\bin\\python.exe',
            get_pydevd_file(),
            '--module',
            '--port',
            '0',
            '--ppid',
            str(os.getpid()),
            '--client',
            '127.0.0.1',
            '--multiprocess',
            '--skip-notify-stdin',
            '--protocol-quoted-line',
            '--file',
            'test',
        ]
    finally:
        SetupHolder.setup = original


def test_monkey_patch_args_unbuffered_module():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0', 'multiprocess': True, 'skip-notify-stdin': True}
        check = ['C:\\bin\\python.exe', '-u', '-m', 'test']
        from _pydevd_bundle.pydevd_command_line_handling import get_pydevd_file
        assert pydev_monkey.patch_args(check) == [
            'C:\\bin\\python.exe',
            '-u',
            get_pydevd_file(),
            '--module',
            '--port',
            '0',
            '--ppid',
            str(os.getpid()),
            '--client',
            '127.0.0.1',
            '--multiprocess',
            '--skip-notify-stdin',
            '--protocol-quoted-line',
            '--file',
            'test',
        ]
    finally:
        SetupHolder.setup = original


def test_monkey_patch_args_module_inline():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0', 'multiprocess': True, 'skip-notify-stdin': True}
        check = ['C:\\bin\\python.exe', '-qOmtest']
        from _pydevd_bundle.pydevd_command_line_handling import get_pydevd_file
        assert pydev_monkey.patch_args(check) == [
            'C:\\bin\\python.exe',
            '-qO',
            get_pydevd_file(),
            '--module',
            '--port',
            '0',
            '--ppid',
            str(os.getpid()),
            '--client',
            '127.0.0.1',
            '--multiprocess',
            '--skip-notify-stdin',
            '--protocol-quoted-line',
            '--file',
            'test',
        ]
    finally:
        SetupHolder.setup = original


def test_monkey_patch_args_module_inline2():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0', 'multiprocess': True, 'skip-notify-stdin': True}
        check = ['C:\\bin\\python.exe', '-qOm', 'test']
        from _pydevd_bundle.pydevd_command_line_handling import get_pydevd_file
        assert pydev_monkey.patch_args(check) == [
            'C:\\bin\\python.exe',
            '-qO',
            get_pydevd_file(),
            '--module',
            '--port',
            '0',
            '--ppid',
            str(os.getpid()),
            '--client',
            '127.0.0.1',
            '--multiprocess',
            '--skip-notify-stdin',
            '--protocol-quoted-line',
            '--file',
            'test',
        ]
    finally:
        SetupHolder.setup = original


def test_monkey_patch_args_no_indc():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0'}
        check = ['C:\\bin\\python.exe', 'connect(\\"127.0.0.1\\")', 'with spaces']
        from _pydevd_bundle.pydevd_command_line_handling import get_pydevd_file
        assert pydev_monkey.patch_args(check) == [
            'C:\\bin\\python.exe',
            get_pydevd_file(),
            '--port',
            '0',
            '--ppid',
            str(os.getpid()),
            '--client',
            '127.0.0.1',
            '--protocol-quoted-line',
            '--file',
            '"connect(\\\\\\"127.0.0.1\\\\\\")"' if sys.platform == 'win32' else 'connect(\\"127.0.0.1\\")',
            '"with spaces"'  if sys.platform == 'win32' else 'with spaces',
        ]
    finally:
        SetupHolder.setup = original


def test_monkey_patch_args_no_indc_with_pydevd():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0'}
        check = ['C:\\bin\\python.exe', 'pydevd.py', 'connect(\\"127.0.0.1\\")', 'bar']

        assert pydev_monkey.patch_args(check) == [
            'C:\\bin\\python.exe', 'pydevd.py', 'connect(\\"127.0.0.1\\")', 'bar']
    finally:
        SetupHolder.setup = original


def test_monkey_patch_args_no_indc_without_pydevd():
    original = SetupHolder.setup
    from _pydevd_bundle.pydevd_command_line_handling import get_pydevd_file

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0'}
        check = ['C:\\bin\\python.exe', 'target.py', 'connect(\\"127.0.0.1\\")', 'bar']
        assert pydev_monkey.patch_args(check) == [
            'C:\\bin\\python.exe',
            get_pydevd_file(),
            '--port',
            '0',
            '--ppid',
            str(os.getpid()),
            '--client',
            '127.0.0.1',
            '--protocol-quoted-line',
            '--file',
            'target.py',
            '"connect(\\\\\\"127.0.0.1\\\\\\")"' if sys.platform == 'win32' else 'connect(\\"127.0.0.1\\")',
            'bar',
        ]
    finally:
        SetupHolder.setup = original


@pytest.mark.parametrize('use_bytes', [True, False])
def test_monkey_patch_c_program_arg(use_bytes):
    original = SetupHolder.setup
    from _pydevd_bundle.pydevd_command_line_handling import get_pydevd_file

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0'}
        check = ['C:\\bin\\python.exe', '-u', 'target.py', '-c', '-áéíóú']

        encode = lambda s:s
        if use_bytes:
            if not IS_PY2:
                check = [c.encode('utf-8') for c in check]
                encode = lambda s:s.encode('utf-8')
        else:
            if IS_PY2:
                check = [c.decode('utf-8') for c in check]
                encode = lambda s:s.decode('utf-8')

        assert pydev_monkey.patch_args(check) == [
            encode('C:\\bin\\python.exe'),
            encode('-u'),
            get_pydevd_file(),
            '--port',
            '0',
            '--ppid',
            str(os.getpid()),
            '--client',
            '127.0.0.1',
            '--protocol-quoted-line',
            '--file',
            encode('target.py'),
            encode('-c'),
            encode('-áéíóú')
        ]
    finally:
        SetupHolder.setup = original


def test_monkey_patch_args_module_single_arg():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0', 'multiprocess': True}
        check = ['C:\\bin\\python.exe', '-mtest', 'bar']
        from _pydevd_bundle.pydevd_command_line_handling import get_pydevd_file
        assert pydev_monkey.patch_args(check) == [
            'C:\\bin\\python.exe',
            get_pydevd_file(),
            '--module',
            '--port',
            '0',
            '--ppid',
            str(os.getpid()),
            '--client',
            '127.0.0.1',
            '--multiprocess',
            '--protocol-quoted-line',
            '--file',
            'test',
            'bar',
        ]
    finally:
        SetupHolder.setup = original


def test_monkey_patch_args_stdin():
    original = SetupHolder.setup

    try:
        SetupHolder.setup = {'client': '127.0.0.1', 'port': '0', 'multiprocess': True}
        check = ['C:\\bin\\python.exe', '-Xfaulthandler' , '-']
        # i.e.: we don't deal with the stdin.
        assert pydev_monkey.patch_args(check) == check
    finally:
        SetupHolder.setup = original
