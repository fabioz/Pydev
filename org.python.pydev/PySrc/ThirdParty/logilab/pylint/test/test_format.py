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
""" Copyright (c) 2000-2003 LOGILAB S.A. (Paris, FRANCE).
 http://www.logilab.fr/ -- mailto:contact@logilab.fr

Check format checker helper functions
"""

__revision__ = '$Id: test_format.py,v 1.1 2005-01-21 17:46:21 fabioz Exp $'

import unittest
import sys
from os import linesep

from logilab.pylint.checkers.format import *
from utils import TestReporter

REPORTER = TestReporter()

class StringRgxTest(unittest.TestCase):
    """test the STRING_RGX regular expression"""
    
    def test_known_values_1(self):
        self.assertEqual(STRING_RGX.sub('', '"yo"'), '')
        
    def test_known_values_2(self):
        self.assertEqual(STRING_RGX.sub('', "'yo'"), '')
        
    def test_known_values_tq_1(self):
        self.assertEqual(STRING_RGX.sub('', '"""yo"""'), '')
        
    def test_known_values_tq_2(self):
        self.assertEqual(STRING_RGX.sub('', '"""yo\n'), '')
        
    def test_known_values_ta_1(self):
        self.assertEqual(STRING_RGX.sub('', "'''yo'''"), '')
        
    def test_known_values_ta_2(self):
        self.assertEqual(STRING_RGX.sub('', "'''yo\n"), '')
        
    def test_known_values_5(self):
        self.assertEqual(STRING_RGX.sub('', r'"yo\"yo"'), '')
        
    def test_known_values_6(self):
        self.assertEqual(STRING_RGX.sub('', r"'yo\'yo'"), '')
        
    def test_known_values_7(self):
        self.assertEqual(STRING_RGX.sub('', '"yo"upi"yo"upi'), 'upiupi')

    def test_known_values_8(self):
        self.assertEqual(STRING_RGX.sub('', "'yo\\'yo\\"), '')
        
    def test_known_values_9(self):
        self.assertEqual(STRING_RGX.sub('', '"yoyo\\'), '')

    def test_known_values_10(self):
        self.assertEqual(STRING_RGX.sub('', 'self.filterFunc = eval(\'lambda %s: %s\'%(\',\'.join(variables),formula),{},{})'),
                         'self.filterFunc = eval(%(.join(variables),formula),{},{})')
        
    def test_known_values_11(self):
        self.assertEqual(STRING_RGX.sub('', 'cond_list[index] = OLD_PROG.sub(r\'getattr(__old__,"\1")\',cond)'),
                         'cond_list[index] = OLD_PROG.sub(r,cond)')



if linesep != '\n':
    import re
    LINE_RGX = re.compile(linesep)
    def ulines(strings):
        return strings[0], LINE_RGX.sub('\n', strings[1])
else:
    def ulines(strings):
        return strings
    
class ChecklineFunctionTest(unittest.TestCase):
    """test the check_line method"""
    
    def test_known_values_opspace_1(self):
        self.assertEqual(ulines(check_line('a=1', REPORTER)), ('C0322', 'a=1\n ^'))
        
    def test_known_values_opspace_2(self):
        self.assertEqual(ulines(check_line('a= 1', REPORTER)), ('C0322', 'a= 1\n ^') )
        
    def test_known_values_opspace_3(self):
        self.assertEqual(ulines(check_line('a =1', REPORTER)), ('C0323', 'a =1\n  ^'))

    def test_known_values_opspace_4(self):
        self.assertEqual(check_line('f(a=1)', REPORTER), None)

    def test_known_values_opspace_4(self):
        self.assertEqual(check_line('f(a=1)', REPORTER), None)

        
##     def test_known_values_colonnl_1(self):
##         self.assertEqual(check_line('if a: a = 1', REPORTER),
##                          ('W0321', 'if a: a = 1\n    ^^^^^^^'))
        
##     def test_known_values_colonnl_2(self):
##         self.assertEqual(check_line('a[:1]', REPORTER), None)
        
##     def test_known_values_colonnl_3(self):
##         self.assertEqual(check_line('a[1:]', REPORTER), None)
        
##     def test_known_values_colonnl_4(self):
##         self.assertEqual(check_line('a[1:2]', REPORTER), None)

##     def test_known_values_colonnl_5(self):
##         self.assertEqual(check_line('def intersection(list1, list2):', REPORTER), None)

##     def test_known_values_colonnl_6(self):
##         self.assertEqual(check_line('def intersection(list1, list2):\n', REPORTER), None)

##     def test_known_values_colonnl_7(self):
##         self.assertEqual(check_line('if file[:pfx_len] == path:\n', REPORTER), None)

##     def test_known_values_colonnl_8(self):
##         self.assertEqual(check_line('def intersection(list1, list2): pass\n', REPORTER),
##                          ('W0321',
## 'def intersection(list1, list2): pass\n                              ^^^^^^') )

##     def test_known_values_colonnl_9(self):
##         self.assertEqual(check_line('if file[:pfx_len[1]] == path:\n', REPORTER), None)

##     def test_known_values_colonnl_10(self):
##         self.assertEqual(check_line('if file[pfx_len[1]] == path:\n', REPORTER), None)

        
    def test_known_values_commaspace_1(self):
        self.assertEqual(ulines(check_line('a, b = 1,2', REPORTER)),
                         ('C0324', 'a, b = 1,2\n        ^^'))

        
    def test_known_values_instring_1(self):
        self.assertEqual(check_line('f("a=1")', REPORTER), None)
        
    def test_known_values_instring_2(self):
        self.assertEqual(ulines(check_line('print >>1, ("a:1")', REPORTER)),
                         ('C0323', 'print >>1, ("a:1")\n       ^'))

    def test_known_values_all_1(self):
        self.assertEqual(ulines(check_line("self.filterFunc = eval('lambda %s: %s'%(','.join(variables),formula),{},{})", REPORTER)),
                         ('C0324', "self.filterFunc = eval('lambda %s: %s'%(','.join(variables),formula),{},{})\n                                                           ^^"))

    def test_known_values_tqstring(self):
        self.assertEqual(check_line('print """<a="=")', REPORTER), None)
        
    def test_known_values_tastring(self):
        self.assertEqual(check_line("print '''<a='=')", REPORTER), None)
        
if __name__ == '__main__':
    unittest.main()
