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
""" Copyright (c) 2003-2005 LOGILAB S.A. (Paris, FRANCE).
 http://www.logilab.fr/ -- mailto:contact@logilab.fr
"""

__revision__ = '$Id: unittest_lint.py,v 1.3 2005-02-24 18:28:47 fabioz Exp $'

import unittest
import sys
import os
import tempfile
from os.path import join
from cStringIO import StringIO

from logilab.pylint.config import get_note_message
from logilab.pylint.lint import PyLinter, Run, sort_checkers, UnknownMessage
from logilab.pylint.utils import sort_msgs
from logilab.pylint import checkers

class SortMessagesTC(unittest.TestCase):
    
    def test(self):
        l = ['E0501', 'E0503', 'F0002', 'I0201', 'W0540',
             'R0202', 'F0203', 'R0220', 'W0321', 'I0001']
        self.assertEquals(sort_msgs(l), ['I0001', 'I0201',
                                         'R0202', 'R0220',
                                         'W0321', 'W0540',
                                         'E0501', 'E0503',
                                         'F0002', 'F0203'])
try:
    optimized = True
    raise AssertionError
except AssertionError:
    optimized = False
    
class GetNoteMessageTC(unittest.TestCase):
    def test(self):
        msg = None
        for note in range(-1, 11):
            note_msg = get_note_message(note)
            self.assertNotEquals(msg, note_msg)
            msg = note_msg
        if optimized:
            self.assertRaises(AssertionError, get_note_message, 11)
            
class RunTC(unittest.TestCase):
    
    def test_no_args(self):
        sys.stdout = StringIO()
        try:
            try:
                Run([])
            except SystemExit, ex:
                self.assertEquals(ex.code, 1)
            else:
                self.fail()
        finally:
            sys.stdout = sys.__stdout__
            
class PyLinterTC(unittest.TestCase):
    
    def setUp(self):
        self.linter = PyLinter()
        self.linter.disable_message_category('I')
        self.linter.config.persistent = 0
        # register checkers
        checkers.initialize(self.linter)
        
    def test_disable_all(self):
        self.linter.disable_all_checkers()
        checkers = sort_checkers(self.linter._checkers, enabled_only=0)
        self.assert_(len(checkers) > 1)
        checkers = sort_checkers(self.linter._checkers, enabled_only=1)
        self.assertEquals(checkers, [self.linter])
        
    def test_message_help(self):
        msg = self.linter.get_message_help('F0001')
        expected = 'F0001:\n  Used when an error occured preventing the analyzing of a module (unable to\n  find it for instance). This message belongs to the master checker.'
        self.assertEquals(' '.join(msg.splitlines()), ' '.join(expected.splitlines()))
        self.assertRaises(UnknownMessage, self.linter.get_message_help, 'YB12')
        
    def test_enable_message(self):
        linter = self.linter
        linter.open()
        linter.set_current_module('toto')
        self.assert_(linter.is_message_enabled('W0101'))
        self.assert_(linter.is_message_enabled('W0102'))
        linter.disable_message('W0101', scope='package')
        linter.disable_message('W0102', scope='module')
        self.assert_(not linter.is_message_enabled('W0101'))
        self.assert_(not linter.is_message_enabled('W0102'))
        linter.set_current_module('tutu')
        self.assert_(not linter.is_message_enabled('W0101'))
        self.assert_(linter.is_message_enabled('W0102'))        
        linter.enable_message('W0101', scope='package')
        linter.enable_message('W0102', scope='module')
        self.assert_(linter.is_message_enabled('W0101'))
        self.assert_(linter.is_message_enabled('W0102'))

    def test_enable_message_category(self):
        linter = self.linter
        linter.open()
        linter.set_current_module('toto')
        self.assert_(linter.is_message_enabled('W0101'))
        self.assert_(linter.is_message_enabled('R0102'))
        linter.disable_message_category('W', scope='package')
        linter.disable_message_category('REFACTOR', scope='module')
        self.assert_(not linter.is_message_enabled('W0101'))
        self.assert_(not linter.is_message_enabled('R0102'))
        linter.set_current_module('tutu')
        self.assert_(not linter.is_message_enabled('W0101'))
        self.assert_(linter.is_message_enabled('R0102'))        
        linter.enable_message_category('WARNING', scope='package')
        linter.enable_message_category('R', scope='module')
        self.assert_(linter.is_message_enabled('W0101'))
        self.assert_(linter.is_message_enabled('R0102'))


    def test_list_messages(self):
        sys.stdout = StringIO()
        try:
            # just invoke it, don't check the output
            self.linter.list_messages()
        finally:
            sys.stdout = sys.__stdout__

    def test_lint_ext_module_with_file_output(self):
        self.linter.config.files_output = True
        try:
            self.linter.check('StringIO')
            self.assert_(os.path.exists('pylint_StringIO.txt'))
            self.assert_(os.path.exists('pylint_global.txt'))
        finally:
            try:
                os.remove('pylint_StringIO.txt')
                os.remove('pylint_global.txt')
            except:
                pass

    def test_enable_report(self):
        self.assertEquals(self.linter.is_report_enabled('R0001'), True)
        self.linter.disable_report('R0001')
        self.assertEquals(self.linter.is_report_enabled('R0001'), False)
        self.linter.enable_report('R0001')
        self.assertEquals(self.linter.is_report_enabled('R0001'), True)
        

from logilab.pylint import config

class ConfigTC(unittest.TestCase):

    def test_pylint_home(self):
        uhome = os.path.expanduser('~')
        if uhome == '~':
            expected = '.pylint.d'
        else:
            expected = os.path.join(uhome, '.pylint.d')
        self.assertEquals(config.PYLINT_HOME, expected)

        try:
            pylintd = join(tempfile.gettempdir(), '.pylint.d')
            os.environ['PYLINTHOME'] = pylintd
            try:
                reload(config)
                self.assertEquals(config.PYLINT_HOME, pylintd)
            finally:
                try:
                    os.remove(pylintd)
                except:
                    pass
        finally:
            del os.environ['PYLINTHOME']
        
    def test_pylintrc(self):
        try:
            self.assertEquals(config.PYLINTRC, None)
            os.environ['PYLINTRC'] = join(tempfile.gettempdir(), '.pylintrc')
            reload(config)
            self.assertEquals(config.PYLINTRC, None)
            os.environ['PYLINTRC'] = '.'
            reload(config)
            self.assertEquals(config.PYLINTRC, '.')
        finally:
            del os.environ['PYLINTRC']
        
if __name__ == '__main__':
    unittest.main()
