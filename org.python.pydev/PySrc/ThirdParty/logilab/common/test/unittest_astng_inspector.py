# Copyright (c) 2000-2002 LOGILAB S.A. (Paris, FRANCE).
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
"""
unittest for the visitors.diadefs module
"""

__revision__ = "$Id: unittest_astng_inspector.py,v 1.3 2005-04-19 14:39:13 fabioz Exp $"

import unittest
import sys

from logilab.common.astng import ASTNGManager, astng, inspector

def astng_wrapper(func, modname):
    return func(modname)

        
class LinkerTC(unittest.TestCase):
    
    def setUp(self):
        self.project = ASTNGManager().project_from_files(['data2'], astng_wrapper) 
        self.linker = inspector.Linker(self.project)
        self.linker.visit(self.project)

    def test_class_ancestors(self):
        klass = self.project.get_module('data2.clientmodule_test').locals['Specialization']
        self.assert_(hasattr(klass, 'baseobjects'))
        self.assertEqual(len(klass.baseobjects), 1)
        self.assert_(isinstance(klass.baseobjects[0], astng.Class))
        self.assertEqual(klass.baseobjects[0].name, "Ancestor")
        
    def test_class_implements(self):
        klass = self.project.get_module('data2.clientmodule_test').locals['Ancestor']
        self.assert_(hasattr(klass, 'implements'))
        self.assertEqual(len(klass.implements), 1)
        self.assert_(isinstance(klass.implements[0], astng.Class))
        self.assertEqual(klass.implements[0].name, "Interface")
        klass = self.project.get_module('data2.clientmodule_test').locals['Specialization']
        self.assert_(hasattr(klass, 'implements'))
        self.assertEqual(len(klass.implements), 0)
        
    def test_locals_assignment_resolution(self):
        klass = self.project.get_module('data2.clientmodule_test').locals['Specialization']
        self.assert_(hasattr(klass, 'locals_type'))
        type_dict = klass.locals_type
        self.assertEqual(len(type_dict), 2)
        keys = type_dict.keys()
        keys.sort()
        self.assertEqual(keys, ['TYPE', 'top'])
        self.assertEqual(type_dict['TYPE'], 'final class')
        self.assertEqual(type_dict['top'], 'class')
        
    def test_instance_attrs_resolution(self):
        klass = self.project.get_module('data2.clientmodule_test').locals['Specialization']
        self.assert_(hasattr(klass, 'instance_attrs_type'))
        type_dict = klass.instance_attrs_type
        self.assertEqual(len(type_dict), 3)
        keys = type_dict.keys()
        keys.sort()
        self.assertEqual(keys, ['_id', 'relation', 'toto'])
        self.assert_(isinstance(type_dict['_id'], astng.Function))
        self.assertEqual(type_dict['_id'].name, '__init__')
        self.assert_(isinstance(type_dict['relation'], astng.Class), type_dict['relation'])
        self.assertEqual(type_dict['relation'].name, 'DoNothing')
        self.assert_(isinstance(type_dict['toto'], astng.Class), type_dict['toto'])
        self.assertEqual(type_dict['toto'].name, 'Toto')


class LinkerTC2(LinkerTC):
    
    def setUp(self):
        self.project = ASTNGManager().from_directory('data2') 
        self.linker = inspector.Linker(self.project)
        self.linker.visit(self.project)

        
if __name__ == '__main__':
    unittest.main()
