'''
Tests if the folders that are passed in the run appear in the pythonpath.
'''

from org.python.pydev.core import TestDependent
import sys

pathExpected = TestDependent.TEST_PYDEV_PLUGIN_LOC+"tests/jysrc/tests"
syspaths = [p.replace('\\', '/') for p in sys.path]
assert pathExpected in syspaths, 'The path %s is not in the pythonpath (%s)' % (pathExpected, syspaths)
