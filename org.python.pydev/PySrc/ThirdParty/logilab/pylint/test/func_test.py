# Copyright (c) 2002-2004 LOGILAB S.A. (Paris, FRANCE).
# http://www.logilab.fr/ -- mailto:contact@logilab.fr
#
# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation; either version 2 of the License, or (at your option) any later
# version.
#
# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along with
# this program; if not, write to the Free Software Foundation, Inc.,
# 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
"""functional/non regression tests for pylint"""

__revision__ = '$Id: func_test.py,v 1.2 2005-02-16 16:45:43 fabioz Exp $'

import unittest
import sys
import re
from os import linesep
from os.path import exists

from logilab.common import testlib

from utils import get_tests_info, fix_path, TestReporter

from logilab.pylint.lint import PyLinter
from logilab.pylint import checkers

test_reporter = TestReporter()
linter = PyLinter()
linter.set_reporter(test_reporter)
linter.config.persistent = 0
linter.quiet = 1
checkers.initialize(linter)

PY23 = sys.version_info >= (2, 3)


if linesep != '\n':
    LINE_RGX = re.compile(linesep)
    def ulines(string):
        return LINE_RGX.sub('\n', string)
else:
    def ulines(string):
        return string

INFO_TEST_RGX = re.compile('^func_i\d\d\d\d$')

class LintTest(testlib.TestCase):            
                
    def test_functionality(self):
        tocheck = ['input.'+self.module]
        if self.depends:
            tocheck += ['input.%s' % name.replace('.py', '')
                        for name, file in self.depends]
        self._test(tocheck)
        
    def _test(self, tocheck):
        if INFO_TEST_RGX.match(self.module):
            linter.enable_message_category('I')
        else:
            linter.disable_message_category('I')
        
        linter.check(tocheck)
        if self.module.startswith('func_noerror_'):
            expected = ''
        else:
            output = open(self.output)
            expected = output.read().strip()
            output.close()
        got = linter.reporter.finalize().strip()
        try:
            self.assertLinesEquals(got, expected)
        except Exception, ex:
            raise AssertionError('%s: %r\n!=\n%r\n\n%s' % (self.module, got, expected, ex))

class LintTest2(LintTest):            
                
    def test_functionality(self):
        tocheck = ['input/' + self.module + '.py']
        if self.depends:
            tocheck += ['input/%s' % name for name, file in self.depends]
        self._test(tocheck)


class TestTests(unittest.TestCase):
    """check that all testable messages have been checked"""
    def test(self):
        todo = linter._messages.keys()
        for msg_id in test_reporter.message_ids.keys():
            todo.remove(msg_id)
        todo.sort()
        if PY23:
            self.assertEqual(todo, ['E0503', 'F0002', 'F0201', 'F0202', 'F0203', 'F0321', 'I0001'])
        else:
            self.assertEqual(todo, ['F0002', 'F0201', 'F0202', 'F0203', 'F0321', 'I0001'])

        
def make_tests():
    """generate tests classes from test info
    
    return the list of generated test classes
    """
    tests = []
    for module_file, messages_file in get_tests_info('func_', '.py') + [('nonexistant', 'messages/nonexistant.txt')]:
        # skip those tests with python >= 2.3 since py2.3 detects them by itself
        if PY23 and module_file  in ("func_unknown_encoding.py",
                                     ):#"func_nonascii_noencoding.py"):
            continue
        base = module_file.replace('func_', '').replace('.py', '')
        dependancies = get_tests_info(base, '.py')
        
        class LintTestSubclass(LintTest):
            module = module_file.replace('.py', '')
            output = messages_file
            depends = dependancies or None
        tests.append(LintTestSubclass)
        
        class LintTest2Subclass(LintTest2):
            module = module_file.replace('.py', '')
            output = exists(messages_file + '2') and (messages_file + '2') or messages_file
            depends = dependancies or None
        tests.append(LintTest2Subclass)
        
##     # special test for f0003
##     module_file, messages_file in get_tests_info('func_f0003', '.pyc')
##     class LintTestSubclass(LintTest):
##         module = module_file.replace('.pyc', '')
##         output = messages_file
##         depends = dependancies or None
##     tests.append(LintTestSubclass)
        
    class LintBuiltinModuleTest(LintTest):
        output = 'messages/builtin_module.txt'
        module = 'sys'
        def test_functionality(self):
            self._test(['sys'])
            
    tests.append(LintBuiltinModuleTest)
    
    # test all features are tested :)    
    tests.append(TestTests)

    return tests

def suite():
    return unittest.TestSuite([unittest.makeSuite(test)
                               for test in make_tests()])

if __name__=='__main__':
    unittest.main(defaultTest='suite')


