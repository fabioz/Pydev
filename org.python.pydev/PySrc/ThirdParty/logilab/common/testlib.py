
# modified copy of some functions from test/regrtest.py from PyXml

""" Copyright (c) 2002-2003 LOGILAB S.A. (Paris, FRANCE).
http://www.logilab.fr/ -- mailto:contact@logilab.fr  

Run tests.

This will find all modules whose name match a given prefix in the test
directory, and run them.  Various command line options provide
additional facilities.

Command line options:

-v: verbose -- run tests in verbose mode with output to stdout
-q: quiet -- don't print anything except if a test fails
-t: testdir -- directory where the tests will be found
-x: exclude -- add a test to exclude

If no non-option arguments are present, prefixes used are 'test',
'regrtest', 'smoketest' and 'unittest'.

"""

__revision__ = "$Id: testlib.py,v 1.3 2005-01-21 17:42:05 fabioz Exp $"

import sys
import os
import getopt
import traceback
import unittest

try:
    from test import test_support
except ImportError:
    # not always available
    class TestSupport:
        def unload(self, test):
            pass
    test_support = TestSupport()
    
from logilab.common.modutils import load_module_from_name

__all__ = ['main', 'find_tests', 'run_test', 'spawn']

DEFAULT_PREFIXES = ('test', 'regrtest', 'smoketest', 'unittest', 'func', 'validation')

def main(testdir=os.getcwd()):
    """Execute a test suite.

    This also parses command-line options and modifies its behaviour
    accordingly.

    tests -- a list of strings containing test names (optional)
    testdir -- the directory in which to look for tests (optional)

    Users other than the Python test suite will certainly want to
    specify testdir; if it's omitted, the directory containing the
    Python test suite is searched for.

    If the tests argument is omitted, the tests listed on the
    command-line will be used.  If that's empty, too, then all *.py
    files beginning with test_ will be used.

    """

    try:
        opts, args = getopt.getopt(sys.argv[1:], 'vqx:t:')
    except getopt.error, msg:
        print msg
        print __doc__
        return 2
    verbose = 0
    quiet = 0
    exclude = []
    for o, a in opts:
        if o == '-v':
            verbose = verbose+1
        elif o == '-q':
            quiet = 1;
            verbose = 0
        elif o == '-x':
            exclude.append(a)
        elif o == '-t':
            testdir = a
        elif o == '-h':
            print __doc__
            sys.exit(0)
            
    for i in range(len(args)):
        # Strip trailing ".py" from arguments
        if args[i][-3:] == '.py':
            args[i] = args[i][:-3]
    if exclude:
        for i in range(len(exclude)):
            # Strip trailing ".py" from arguments
            if args[i][-3:] == '.py':
                exclude[i] = exclude[i][:-3]
    tests = find_tests(testdir, args or DEFAULT_PREFIXES, excludes=exclude)
    sys.path.insert(0, testdir)
    # Tell tests to be moderately quiet
    test_support.verbose = verbose
    good, bad, skipped, all_result = run_tests(tests, quiet, verbose)
    if not quiet:
        print '*'*80
        if all_result:
            print 'Ran %s test cases' % all_result.testsRun,
            if all_result.errors:
                print ', %s errors' % len(all_result.errors),
            if all_result.failures:
                print ', %s failed' % len(all_result.failures),
            print
        if good:
            if not bad and not skipped and len(good) > 1:
                print "All",
            print _count(len(good), "test"), "OK."
        if bad:
            print _count(len(bad), "test"), "failed:",
            print ', '.join(bad)
        if skipped:
            print _count(len(skipped), "test"), "skipped:",
            print ', '.join(['%s (%s)' % (test, msg) for test, msg in skipped])
    sys.exit(len(bad) + len(skipped))

def run_tests(tests, quiet, verbose, runner=None):
    """ execute a list of tests
    return a 3-uple with :
       _ the list of passed tests
       _ the list of failed tests
       _ the list of skipped tests
    """
    good = []
    bad = []
    skipped = []
    all_result = None
    for test in tests:
        if not quiet:
            print 
            print '-'*80
            print "Executing", test
        result = run_test(test, verbose, runner)
        if type(result) is type(''):
            # an unexpected error occured
            skipped.append( (test, result))
        else:
            if all_result is None:
                all_result = result
            else:
                all_result.testsRun += result.testsRun
                all_result.failures += result.failures
                all_result.errors += result.errors
            if result.errors or result.failures:
                bad.append(test)
                if verbose:
                    print "test", test, \
                          "failed -- %s errors, %s failures" % (
                        len(result.errors), len(result.failures))
            else:
                good.append(test)
            
    return good, bad, skipped, all_result
    
def find_tests(testdir,
               prefixes=DEFAULT_PREFIXES, suffix=".py",
               excludes=(),
               remove_suffix=1):
    """
    Return a list of all applicable test modules.
    """
    tests = []
    for name in os.listdir(testdir):
        if not suffix or name[-len(suffix):] == suffix:
            for prefix in prefixes:
                if name[:len(prefix)] == prefix:
                    if remove_suffix:
                        name = name[:-len(suffix)]
                    if name not in excludes:
                        tests.append(name)
    tests.sort()
    return tests


def run_test(test, verbose, runner=None):
    """
    Run a single test.

    test -- the name of the test
    verbose -- if true, print more messages
    """
    test_support.unload(test)
    try:
        m = load_module_from_name(test, path=sys.path)
#        m = __import__(test, globals(), locals(), sys.path)
        try:
            suite = m.suite
            if hasattr(suite, 'func_code'):
                suite = suite()
        except AttributeError, e:
            loader = unittest.TestLoader()
            suite = loader.loadTestsFromModule(m)
        if runner is None:
            runner = unittest.TextTestRunner()
        return runner.run(suite)
    except KeyboardInterrupt, v:
        raise KeyboardInterrupt, v, sys.exc_info()[2]
    except:
        type, value = sys.exc_info()[:2]
        msg = "test %s crashed -- %s : %s" % (test, type, value)
        if verbose:
            traceback.print_exc()
        return msg

def _count(n, word):
    """format word according to n"""
    if n == 1:
        return "%d %s" % (n, word)
    else:
        return "%d %ss" % (n, word)


# test utils ##################################################################

class TestCase(unittest.TestCase):
    """unittest.TestCase with some additional methods"""


    def assertDictEquals(self, d1, d2):
        d1 = d1.copy()
        for key, value in d2.items():
            try:
                if d1[key] != value:
                    self.fail('%r != %r for key %r' % (d1[key], value, key))
                del d1[key]
            except KeyError:
                self.fail('missing %r key' % key)
        if d1:
            self.fail('d2 is missing %r' % d1)
    
    def assertListEquals(self, l1, l2):
        l1 = l1[:]
        for value in l2:
            try:
                if l1[0] != value:
                    self.fail('%r != %r for index %d' % (l1[0], value,
                                                         l2.index(value)))
                del l1[0]
            except IndexError:
                self.fail('l1 has only %d elements, not %s (at least %r missing)' % (
                    l2.index(value), len(l2), value))
        if l1:
            self.fail('l2 is missing %r' % l1)
    
    def assertLinesEquals(self, l1, l2):
        self.assertListEquals(l1.splitlines(), l2.splitlines())
        
import doctest

class DocTest(unittest.TestCase):
    """trigger module doctest
    I don't know how to make unittest.main consider the DocTestSuite instance
    without this hack
    """
    def run(self, result=None):
        return doctest.DocTestSuite(self.module).run(result)
    
    def test(self):
        """just there to trigger test execution"""


MAILBOX = None

class MockSMTP:
    """fake smtplib.SMTP"""
    
    def __init__(self, host, port):
        self.host = host
        self.port = port
        global MAILBOX
        self.reveived = MAILBOX = []
        
    def set_debuglevel(self, debuglevel):
        """ignore debug level"""
    def sendmail(self, fromaddr, toaddres, body):
        """push sent mail in the mailbox"""
        self.reveived.append((fromaddr, toaddres, body))
    def quit(self):
        """ignore quit"""


class MockConfigParser:
    """fake ConfigParser.ConfigParser"""
    
    def __init__(self, options):
        self.options = options
        
    def get(self, section, option):
        """return option in section"""
        return self.options[section][option]
    def has_option(self, section, option):
        """ask if option exists in section"""
        try:
            return self.get(section, option) or 1
        except KeyError:
            return 0
    

class MockConnexion:
    """fake DB-API 2.0 connexion AND cursor (i.e. cursor() return self)"""
    
    def __init__(self, results):
        self.received = []
        self.results = results
        
    def cursor(self):
        return self
    def execute(self, query):
        self.received.append(query)
    def fetchone(self):
        return self.results[0]
    def fetchall(self):
        return self.result
