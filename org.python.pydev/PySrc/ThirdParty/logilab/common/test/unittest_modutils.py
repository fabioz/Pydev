# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation; either version 2 of the License, or (at your option) any later
# version.

# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

# You should have received a copy of the GNU General Public License along with
# this program; if not, write to the Free Software Foundation, Inc.,
# 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
"""
unit tests for module modutils (module manipulation utilities)

 Copyright (c) 2002-2004 LOGILAB S.A. (Paris, FRANCE).
 http://www.logilab.fr/ -- mailto:contact@logilab.fr
"""

__revision__ = "$Id: unittest_modutils.py,v 1.1 2005-01-21 17:46:21 fabioz Exp $"

import unittest
import sys

from logilab.common import modutils

from os import path
from logilab import common
from logilab.common import tree
    
class load_module_from_name_function_test(unittest.TestCase):
    
    def test_knownValues_load_module_from_name_1(self):
        """ load a python module from it's name """
        self.assertEqual(modutils.load_module_from_name('sys'), sys)

    def test_knownValues_load_module_from_name_2(self):
        """ load a python module from it's name """
        self.assertEqual(modutils.load_module_from_name('os.path'), path)
    
    def test_raise_load_module_from_name_1(self):
        self.assertRaises(ImportError, 
                          modutils.load_module_from_name, 'os.path', use_sys=0)    

class get_module_part_function_test(unittest.TestCase):
    
    def test_knownValues_get_module_part_1(self):
        """ load a python module from it's name """
        self.assertEqual(modutils.get_module_part('logilab.common.modutils'),
                         'logilab.common.modutils')

    def test_knownValues_get_module_part_2(self):
        """ load a python module from it's name """
        self.assertEqual(modutils.get_module_part('logilab.common.modutils.get_module_part'),
                         'logilab.common.modutils')
        
    def test_knownValues_get_module_part_3(self):
        """ load a python module from it's name """
        self.assertEqual(modutils.get_module_part('db.get_connexion', modutils.__file__),
                         'db')

    
class modpath_from_file_function_test(unittest.TestCase):
    
    def test_knownValues_modpath_from_file_1(self):
        """ given an absolute file path return the python module's path as a list """
        self.assertEqual(modutils.modpath_from_file(modutils.__file__),
                         ['logilab', 'common', 'modutils'])
    
    def test_raise_modpath_from_file_Exception(self):
        self.assertRaises(Exception, modutils.modpath_from_file, '/turlututu')

class file_from_modpath_function_test(unittest.TestCase):
    
    def test_knownValues_file_from_modpath_1(self):
        """ given an absolute file path return the python module's path as a list """
        self.assertEqual(modutils.file_from_modpath(['logilab', 'common', 'modutils']),
                         modutils.__file__.replace('.pyc', '.py'))
    
    def test_knownValues_file_from_modpath_2(self):
        """ given an absolute file path return the python module's path as a list """
        from os import path
        self.assertEqual(modutils.file_from_modpath(['os', 'path']).replace('.pyc', '.py'),
                         path.__file__.replace('.pyc', '.py'))
    
    def test_knownValues_file_from_modpath_3(self):
        """ given an absolute file path return the python module's path as a list """
        from xml.dom import ext
        self.assertEqual(modutils.file_from_modpath(['xml', 'dom', 'ext']).replace('.pyc', '.py'),
                         ext.__file__.replace('.pyc', '.py'))
    
    def test_raise_file_from_modpath_Exception(self):
        self.assertRaises(ImportError, modutils.file_from_modpath, ['turlututu'])

    
class is_standard_module_function_test(unittest.TestCase):
    
    def test_knownValues_is_standard_module_0(self):
        """
        return true if the module may be considered as a module from the standard
        library
        """
        self.assertEqual(modutils.is_standard_module('__builtin__'), True)
        
    def test_knownValues_is_standard_module_1(self):
        """
        return true if the module may be considered as a module from the standard
        library
        """
        self.assertEqual(modutils.is_standard_module('sys'), True)
        
    def test_knownValues_is_standard_module_2(self):
        """
        return true if the module may be considered as a module from the standard
        library
        """
        self.assertEqual(modutils.is_standard_module('logilab'), False)

    
class is_relative_function_test(unittest.TestCase):
    
    def test_knownValues_is_relative_1(self):
        """
        return true if the module may be considered as a module from the standard
        library
        """
        self.assertEqual(modutils.is_relative('modutils', common.__path__[0]), True)

    def test_knownValues_is_relative_2(self):
        """
        return true if the module may be considered as a module from the standard
        library
        """
        self.assertEqual(modutils.is_relative('modutils', tree.__file__), True)
        
    def test_knownValues_is_relative_3(self):
        """
        return true if the module may be considered as a module from the standard
        library
        """
        self.assertEqual(modutils.is_relative('logilab.common.modutils',
                                              common.__path__[0]), False)
    
class get_modules_function_test(unittest.TestCase):
    
    def test_knownValues_get_modules_1(self): #  XXXFIXME: TOWRITE
        """given a directory return a list of all available python modules, even
        in subdirectories

        """
        import data
        modules = modutils.get_modules('data', data.__path__[0])
        modules.sort()
        self.assertEqual(modules,
                         ['data.module', 'data.module2', 'data.noendingnewline',
                          'data.nonregr'])

def suite():
    """return the unitest suite"""
    loader = unittest.TestLoader()
    module = sys.modules[__name__]
    if __name__ == '__main__' and len(sys.argv) > 1:
        return loader.loadTestsFromNames(sys.argv[1:], module)
    return loader.loadTestsFromModule(module)
    
def Run():
    testsuite = suite()
    runner = unittest.TextTestRunner()
    return runner.run(testsuite)
    
if __name__ == '__main__':
    Run()
