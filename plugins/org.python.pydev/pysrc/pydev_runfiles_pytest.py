from _pytest import runner  # @UnresolvedImport
from _pytest import unittest as pytest_unittest  # @UnresolvedImport
import os
from py._code import code  # @UnresolvedImport
import pydev_runfiles_xml_rpc
from pydevd_file_utils import _NormFile
import pytest
import time


#=======================================================================================================================
# _CollectTestsFromUnittestCase
#=======================================================================================================================
class _CollectTestsFromUnittestCase:

    def __init__(self, found_methods_starting, unittest_case):
        self.found_methods_starting = found_methods_starting
        self.unittest_case = unittest_case


    def __call__(self):
        for name in self.found_methods_starting:
            yield pytest_unittest.TestCaseFunction(name, parent=self.unittest_case)


#=======================================================================================================================
# PydevPlugin
#=======================================================================================================================
class PydevPlugin:

    def __init__(self, py_test_accept_filter):
        self.py_test_accept_filter = py_test_accept_filter
        self._original_pytest_collect_makeitem = pytest_unittest.pytest_pycollect_makeitem
        self._using_xdist = False

    def reportCond(self, cond, filename, test, captured_output, error_contents, delta):
        '''
        @param filename: 'D:\\src\\mod1\\hello.py'
        @param test: 'TestCase.testMet1'
        @param cond: fail, error, ok
        '''
        time_str = '%.2f' % (delta,)
        pydev_runfiles_xml_rpc.notifyTest(cond, captured_output, error_contents, filename, test, time_str)


    def pytest_runtest_setup(self, item):
        if not self.py_test_accept_filter:
            return #Keep on going (nothing to filter)

        f = _NormFile(item.parent.fspath)
        name = item.name

        if f not in self.py_test_accept_filter:
            pytest.skip() # Skip the file

        accept_tests = self.py_test_accept_filter[f]
        found_methods_starting = []

        if item.cls is not None:
            class_name = item.cls.__name__
        else:
            class_name = None
        for test in accept_tests:
            if test == name:
                #Direct match of the test (just go on with the default loading)
                return

            if class_name is not None:
                if test == class_name + '.' + name:
                    return

                if class_name == test:
                    return

        # If we had a match it'd have returned already.
        pytest.skip() # Skip the test



    def _MockFileRepresentation(self):
        code.ReprFileLocation._original_toterminal = code.ReprFileLocation.toterminal

        def toterminal(self, tw):
            # filename and lineno output for each entry,
            # using an output format that most editors understand
            msg = self.message
            i = msg.find("\n")
            if i != -1:
                msg = msg[:i]

            tw.line('File "%s", line %s\n%s' %(os.path.abspath(self.path), self.lineno, msg))

        code.ReprFileLocation.toterminal = toterminal


    def _UninstallMockFileRepresentation(self):
        code.ReprFileLocation.toterminal = code.ReprFileLocation._original_toterminal #@UndefinedVariable


    def pytest_cmdline_main(self, config):
        if hasattr(config.option, 'numprocesses'):
            if config.option.numprocesses:
                self._using_xdist = True
                pydev_runfiles_xml_rpc.notifyTestRunFinished('Unable to show results (py.test xdist plugin not compatible with PyUnit view)')


    def pytest_runtestloop(self, session):
        if self._using_xdist:
            #Yes, we don't have the hooks we'd need to show the results in the pyunit view...
            #Maybe the plugin maintainer may be able to provide these additional hooks?
            return None

        #This mock will make all file representations to be printed as Pydev expects,
        #so that hyperlinks are properly created in errors. Note that we don't unmock it!
        self._MockFileRepresentation()

        #Based on the default run test loop: _pytest.session.pytest_runtestloop
        #but getting the times we need, reporting the number of tests found and notifying as each
        #test is run.

        start_total = time.time()
        try:

            new_items = []
            from pytest import skip
            for item in session.session.items:
                try:
                    self.pytest_runtest_setup(item)
                except skip.Exception:
                    pass #Ignore skips.
                else:
                    new_items.append(item)

            items = new_items

            pydev_runfiles_xml_rpc.notifyTestsCollected(len(items))

            if session.config.option.collectonly:
                return True

            for item in items:

                filename = item.fspath.strpath
                test = item.location[2]
                start = time.time()

                pydev_runfiles_xml_rpc.notifyStartTest(filename, test)

                #Don't use this hook because we need the actual reports.
                #item.config.hook.pytest_runtest_protocol(item=item)
                reports = runner.runtestprotocol(item)
                delta = time.time() - start

                captured_output = ''
                error_contents = ''


                status = 'ok'
                for r in reports:
                    if r.when == 'setup' and r.outcome == 'skipped':
                        status = 'skip'
                        break

                    if r.outcome not in ('passed', 'skipped'):
                        #It has only passed, skipped and failed (no error), so, let's consider error if not on call.
                        if r.when == 'setup':
                            if status == 'ok':
                                status = 'error'

                        elif r.when == 'teardown':
                            if status == 'ok':
                                status = 'error'

                        else:
                            #any error in the call (not in setup or teardown) is considered a regular failure.
                            status = 'fail'

                    if hasattr(r, 'longrepr') and r.longrepr:
                        rep = r.longrepr
                        if hasattr(rep, 'reprcrash'):
                            reprcrash = rep.reprcrash
                            error_contents += str(reprcrash)
                            error_contents += '\n'

                        if hasattr(rep, 'reprtraceback'):
                            error_contents += str(rep.reprtraceback)

                        if hasattr(rep, 'sections'):
                            for name, content, sep in rep.sections:
                                error_contents += sep * 40
                                error_contents += name
                                error_contents += sep * 40
                                error_contents += '\n'
                                error_contents += content
                                error_contents += '\n'

                if status != 'skip': #I.e.: don't event report skips...
                    self.reportCond(status, filename, test, captured_output, error_contents, delta)

                if session.shouldstop:
                    raise session.Interrupted(session.shouldstop)
        finally:
            pydev_runfiles_xml_rpc.notifyTestRunFinished('Finished in: %.2f secs.' % (time.time() - start_total,))
        return True

