import unittest
import sys
import os
from nose.tools import eq_
from pprint import pprint
from pydev_imports import StringIO

# make it as if we were executing from the directory above this one
sys.argv[0] = os.path.dirname(sys.argv[0])
# twice the dirname to get the previous level from this file.
sys.path.insert(1, os.path.join(os.path.dirname(sys.argv[0])))

# PyDevFrontEnd depends on singleton in IPython, so you
# can't make multiple versions. So we reuse front_end for
# all the tests

orig_stdout = sys.stdout
orig_stderr = sys.stderr

stdout = sys.stdout = StringIO()
stderr = sys.stderr = StringIO()

from pydev_ipython_console_011 import PyDevFrontEnd
front_end = PyDevFrontEnd()


def addExec(code, expected_more=False):
    more = front_end.addExec(code)
    eq_(expected_more, more)

class TestBase(unittest.TestCase):
    def setUp(self):
        front_end.input_splitter.reset()
        stdout.truncate(0)
        stdout.seek(0)
        stderr.truncate(0)
        stderr.seek(0)
    def tearDown(self):
        pass


class TestPyDevFrontEnd(TestBase):
    def testAddExec_1(self):
        addExec('if True:', True)
    def testAddExec_2(self):
        addExec('if True:\n    testAddExec_a = 10\n', True)
    def testAddExec_3(self):
        assert 'testAddExec_a' not in front_end.getNamespace()
        addExec('if True:\n    testAddExec_a = 10\n\n')
        assert 'testAddExec_a' in front_end.getNamespace()
        eq_(front_end.getNamespace()['testAddExec_a'], 10)

    def testGetNamespace(self):
        assert 'testGetNamespace_a' not in front_end.getNamespace()
        addExec('testGetNamespace_a = 10')
        assert 'testGetNamespace_a' in front_end.getNamespace()
        eq_(front_end.getNamespace()['testGetNamespace_a'], 10)

    def testComplete(self):
        unused_text, matches = front_end.complete('%')
        assert len(matches) > 1, 'at least one magic should appear in completions'

        addExec('testComplete_a = 5')
        addExec('testComplete_b = 10')
        addExec('testComplete_c = 15')
        unused_text, matches = front_end.complete('testComplete_')
        assert len(matches) == 3
        eq_(set(['testComplete_a', 'testComplete_b', 'testComplete_c']), set(matches))


class TestRunningCode(TestBase):
    def testPrint(self):
        addExec('print("output")')
        eq_(stdout.getvalue(), 'output\n')

    def testQuestionMark_1(self):
        addExec('?')
        assert len(stdout.getvalue()) > 1000, 'IPython help should be pretty big'

    def testQuestionMark_2(self):
        addExec('int?')
        assert stdout.getvalue().find('Convert') != -1

    def testGui(self):
        from pydev_ipython.inputhook import get_inputhook, set_stdin_file
        set_stdin_file(sys.stdin)
        assert get_inputhook() is None
        addExec('%gui tk')
        # we can't test the GUI works here because we aren't connected to XML-RPC so
        # nowhere for hook to run
        assert get_inputhook() is not None
        addExec('%gui none')
        assert get_inputhook() is None

if __name__ == '__main__':
    unittest.main()
