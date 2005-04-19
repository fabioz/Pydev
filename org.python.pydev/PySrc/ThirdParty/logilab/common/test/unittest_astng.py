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
"""tests for the astng

Copyright (c) 2003-2004 LOGILAB S.A. (Paris, FRANCE).
http://www.logilab.fr/ -- mailto:contact@logilab.fr
"""

__revision__ = "$Id: unittest_astng.py,v 1.3 2005-04-19 14:39:13 fabioz Exp $"

import unittest
import sys
import parser
from os import getcwd
from os.path import join

from logilab.common.astng import builder, astng, ResolveError, NotFoundError, \
     ASTNGManager
from logilab.common.modutils import load_module_from_name

import data
from data import module as test_module

manager = ASTNGManager()
abuilder = builder.ASTNGBuilder(manager) 
MODULE = abuilder.build_from_module(test_module)
MODULE2 = abuilder.file_build('data/module2.py', 'data.module2')
NONREGR = abuilder.file_build('data/nonregr.py', 'data.nonregr')

class ModuleTC(unittest.TestCase):
    
    def test_wildard_import_names(self):
        m = abuilder.file_build('astng_data/all.py', 'all')
        self.assertEquals(m.wildcard_import_names(), ['Aaa', '_bla', 'name'])
        m = abuilder.file_build('astng_data/notall.py', 'notall')
        res = m.wildcard_import_names()
        res.sort()
        self.assertEquals(res, ['Aaa', 'func', 'name', 'other'])
        
class ASTNGRepresentationTest(unittest.TestCase):

    def test_module_base_props(self):
        self.assertEquals(MODULE.name, 'data.module')
        self.assertEquals(MODULE.doc, "test module for astng\n")
        self.assertEquals(MODULE.object, test_module)
        self.assertEquals(MODULE.parent, None)
        self.assertEquals(MODULE.get_frame(), MODULE)
        self.assertEquals(MODULE.root(), MODULE)
        self.assertEquals(MODULE.file, join(data.__path__[0], 'module.py'))
        self.assertEquals(MODULE.pure_python, 1)
        self.assertEquals(MODULE.package, 0)
        self.assert_(not MODULE.is_statement())
        self.assertEquals(MODULE.get_statement(), MODULE)
        self.assertEquals(MODULE.node.get_statement(), MODULE)

        
    def test_module_locals(self):
        _locals = MODULE.locals
        keys = _locals.keys()
        keys.sort()
        self.assert_(_locals is MODULE.globals)
        self.assertEquals(len(_locals), 13)
        # __builtins__ is introduced by "from toto import *"
        self.assertEquals(keys, ['MY_DICT', 'YO', 'YOUPI',
                                 '__revision__',
                                'clean', 'cvrtr', 'debuild', 'global_access',
                                'modutils', 'nested_args', 'os', 'redirect', 'spawn'])


    def test_import_methods(self):
        from_ = MODULE.locals['modutils']
        self.assertEquals(from_.get_real_name('spawn'), 'Execute')
        self.assertEquals(from_.get_module_object('spawn'), ('logilab.common', 'Execute'))

    def test_import_node(self):
        imp_ = MODULE.locals['os']
        self.assertEquals(imp_.get_real_name('os'), 'os')
        self.assertRaises(NotFoundError, imp_.get_real_name, 'os.path')
        imp_ = MODULE.locals['spawn']
        self.assertEquals(imp_.get_real_name('spawn'), 'Execute')
        self.assertRaises(NotFoundError, imp_.get_real_name, 'Execute')
        imp_ = MODULE2.locals['YO']
        self.assertEquals(imp_.get_real_name('YO'), 'YO')
        self.assertRaises(NotFoundError, imp_.get_real_name, 'data')
        
    def test_function_base_props(self):
        function = MODULE.locals['global_access']

        self.assertEquals(function.name, 'global_access')
        self.assertEquals(function.doc, 'function test')
        self.assertEquals(function.object, test_module.global_access)
        self.assert_(function.parent)
        self.assertEquals(function.get_frame(), function)
        self.assertEquals(function.parent.get_frame(), MODULE)
        self.assertEquals(function.root(), MODULE)
        self.assertEquals(function.argnames, ['key', 'val'])
        self.assertEquals(function.is_method(), 0)


    def test_function_locals(self):
        locals = MODULE.locals['global_access'].locals
        keys = locals.keys()
        keys.sort()

        self.assertEquals(len(locals), 4)
        self.assertEquals(keys, ['i', 'key', 'local', 'val'])


    def test_function_navigation(self):
        function = MODULE.locals['global_access']
        self.assertEquals(function.get_statement(), function)
        l_sibling = function.previous_sibling()
        self.assert_(isinstance(l_sibling, astng.Assign))
        r_sibling = function.next_sibling()
        self.assert_(isinstance(r_sibling, astng.Class))
        self.assertEquals(r_sibling.name, 'YO')
        last = r_sibling.next_sibling().next_sibling().next_sibling()
        self.assert_(isinstance(last, astng.Assign))
        self.assertEquals(last.next_sibling(), None)
        first = l_sibling.previous_sibling().previous_sibling().previous_sibling().previous_sibling()
        self.assertEquals(first.previous_sibling(), None)
        
    def test_class_base_props(self):
        klass = MODULE.locals['YO']

        self.assertEquals(klass.name, 'YO')
        self.assertEquals(klass.doc, 'hehe')
        self.assertEquals(klass.object, test_module.YO)
        self.assert_(klass.parent)
        self.assertEquals(klass.get_frame(), klass)
        self.assertEquals(klass.parent.get_frame(), MODULE)
        self.assertEquals(klass.root(), MODULE)
        self.assertEquals(klass.basenames, [])


    def test_class_locals(self):
        klass1 = MODULE.locals['YO']
        klass2 = MODULE.locals['YOUPI']
        locals1 = klass1.locals
        locals2 = klass2.locals
        keys = locals2.keys()
        keys.sort()

        self.assertEquals(locals1.keys(), ['a', '__init__'])
        self.assertEquals(keys, ['__init__', 'class_attr', 'class_method',
                                'method', 'static_method'])


    def test_class_navigation(self):
        klass = MODULE.locals['YO']
        self.assertEquals(klass.get_statement(), klass)
        l_sibling = klass.previous_sibling()
        self.assert_(isinstance(l_sibling, astng.Function), l_sibling)
        self.assertEquals(l_sibling.name, 'global_access')
        r_sibling = klass.next_sibling()
        self.assert_(isinstance(r_sibling, astng.Class))
        self.assertEquals(r_sibling.name, 'YOUPI')

    def test_class_members(self):
        klass1 = MODULE.locals['YO']
        klass2 = MODULE.locals['YOUPI']

        self.assertEquals(klass1.instance_attrs.keys(), ['yo'])
        self.assertEquals(klass2.instance_attrs.keys(), ['member'])


    def test_class_inheritance(self):
        klass1 = MODULE.locals['YO']
        klass2 = MODULE.locals['YOUPI']

        self.assertEquals(klass1.basenames, [])
        self.assertEquals(klass2.basenames, ['YO'])

    def test_class_ancestor_method(self):
        klass1 = MODULE.locals['YO']
        klass2 = MODULE.locals['YOUPI']
        anc_klass = klass2.get_ancestor_for_method('__init__')
        self.assert_(isinstance(anc_klass, astng.Class))
        self.assertEquals(anc_klass.name, 'YO')

    def test_class_ancestor_attr(self):
        klass1 = MODULE.locals['YO']
        klass2 = MODULE.locals['YOUPI']
        anc_klass = klass2.get_ancestor_for_attribute('yo')
        self.assert_(isinstance(anc_klass, astng.Class))
        self.assertEquals(anc_klass.name, 'YO')
        
    def test_class_methods(self):
        klass2 = MODULE.locals['YOUPI']
        methods = [m.name for m in klass2.methods()]
        methods.sort()
        self.assertEquals(methods, ['__init__', 'class_method',
                                   'method', 'static_method'])
        methods = [m.name for m in klass2.methods(inherited=False)]
        methods.sort()
        self.assertEquals(methods, ['__init__', 'class_method',
                                   'method', 'static_method'])
        klass2 = MODULE2.locals['Specialization']
        methods = [m.name for m in klass2.methods(False)]
        methods.sort()
        self.assertEquals(methods, [])
        self.assertEquals(klass2.get_method('method').name, 'method')
        self.assertRaises(NotFoundError, klass2.get_method, 'nonexistant')
        methods = [m.name for m in klass2.methods(inherited=True)]
        methods.sort()
        self.assertEquals(methods, ['__init__', 'class_method',
                                   'method', 'static_method'])
        
    def test_method_base_props(self):
        klass2 = MODULE.locals['YOUPI']
        method = klass2.locals['method']

        self.assertEquals(method.name, 'method')
        self.assertEquals(method.argnames, ['self'])
        self.assertEquals(method.doc, 'method test')
        self.assertEquals(method.is_method(), 1)
        self.assertEquals(method.is_static_method(), 0)
        self.assertEquals(method.is_class_method(), 0)


    def test_method_locals(self):
        klass2 = MODULE.locals['YOUPI']
        method = klass2.locals['method']
        locals = method.locals
        keys = locals.keys()
        keys.sort()
        self.assertEquals(len(locals), 6)
        self.assertEquals(keys, ['MY_DICT', 'a', 'autre', 'b', 'local', 'self'])
        
    def test_static_method_base_props(self):
        klass2 = MODULE.locals['YOUPI']
        method = klass2.locals['static_method']

        self.assertEquals(method.argnames, [])
        self.assertEquals(method.is_method(), 1)
        self.assertEquals(method.is_static_method(), 1)
        self.assertEquals(method.is_class_method(), 0)
        
    def test_class_method_base_props(self):
        klass2 = MODULE.locals['YOUPI']
        method = klass2.locals['class_method']

        self.assertEquals(method.argnames, ['cls'])
        self.assertEquals(method.is_method(), 1)
        self.assertEquals(method.is_static_method(), 0)
        self.assertEquals(method.is_class_method(), 1)

    def test_func_nested_args(self):
        func = MODULE.locals['nested_args']
        self.assertEquals(func.argnames, ['a', ('b', 'c', 'd')])
        local = func.locals.keys()
        local.sort()
        self.assertEquals(local, ['a', 'b', 'c', 'd'])
        self.assertEquals(func.is_method(), 0)
        self.assertEquals(func.is_static_method(), 0)
        self.assertEquals(func.is_class_method(), 0)
       
    def test_as_string(self):
        self.assert_(MODULE.as_string())
        self.assert_(MODULE2.as_string())

    def test_build_from_living(self):
        import time
        timeastng = abuilder.build_from_module(time)
        self.assert_(timeastng)

    def test_resolve_astng(self):
        yo = MODULE.resolve('YO')
        self.assert_(isinstance(yo, astng.Class))
        self.assertEquals(yo.name, 'YO')
        
        red = MODULE.resolve('redirect')
        self.assert_(isinstance(red, astng.Function))
        self.assertEquals(red.name, 'nested_args')
        
        spawn = MODULE.resolve('spawn')
        self.assert_(isinstance(spawn, astng.Class))
        self.assertEquals(spawn.name, 'Execute')

        my_dict = MODULE.locals['YOUPI'].resolve('MY_DICT')
        self.assert_(isinstance(my_dict, astng.Dict))
        method = MODULE.locals['YOUPI'].locals['method']
        my_dict = method.resolve('MY_DICT')
        self.assert_(isinstance(my_dict, astng.Dict))
        
        none = MODULE.resolve('None')
        self.assertEquals(none.value, None)
        none = MODULE.locals['YOUPI'].resolve('None')
        self.assertEquals(none.value, None)

##     def test_resolve_object(self):
##         yo = MODULE.resolve('YO')
##         self.assert_(yo is test_module.YO)
        
##         red = MODULE.resolve('redirect')
##         self.assert_(red is test_module.nested_args)
        
##         from logilab.common import Execute
##         spawn = MODULE.resolve('spawn')
##         self.assert_(spawn is Execute)
        
    def test_resolve_default(self):
        yo = MODULE.resolve('YO')
        self.assert_(isinstance(yo, astng.Class))
        self.assertEquals(yo.name, 'YO')

        method = MODULE.locals['YOUPI'].locals['method']
        my_dict = method.resolve('MY_DICT')
        self.assert_(isinstance(my_dict, astng.Dict))

    def test_resolve_builtin(self):
        yo = MODULE.resolve('object')
        self.assert_(isinstance(yo, astng.Class))
        self.assertEquals(yo.name, 'object')
        yo = MODULE.resolve('YO')
        _object = yo.resolve('object')
        self.assert_(isinstance(_object, astng.Class))
        self.assertEquals(_object.name, 'object')
        # check builtin function has argnames == None
        _object = yo.resolve('dict')
        self.assertEquals(_object.locals['has_key'].argnames, None)

    def test_resolve_raise(self):
        self.assertRaises(ResolveError, MODULE.resolve, 'YOAA')

    def test_resolve_nonregr(self):
        self.assertEquals(NONREGR.resolve('enumerate').name, 'enumerate')
        self.assertRaises(ResolveError, NONREGR.locals['toto'].resolve_dotted, 'v.get')
                          
    def test_resolve_package_redirection(self):
        sys.path.insert(1, 'astng_data')
        try:
            m = abuilder.file_build('astng_data/appl/myConnection.py', 'appl.myConnection')
            cnx = m.resolve_dotted('SSL1.Connection')
            self.assertEquals(cnx.__class__, astng.Class)
            self.assertEquals(cnx.name, 'Connection')
            self.assertEquals(cnx.root().name, 'Connection1')
        finally:
            del sys.path[1]
        
    def test_get_assigned_value(self):
        my_dict = MODULE.locals['MY_DICT']
        self.assert_(isinstance(my_dict.get_assigned_value(), astng.Dict))
        a = MODULE.locals['YO'].locals['a']
        value = a.get_assigned_value()
        self.assert_(isinstance(value, astng.Const))
        self.assertEquals(value.value, 1)
        
class AncestorsFTC(unittest.TestCase):
        
    def test1(self):
        klass = MODULE.locals['YOUPI']
        ancs = [a.name for a in klass.ancestors()]
        self.assertEquals(ancs, ['YO'])

    def test2(self):
        klass = MODULE2.locals['Specialization']
        ancs = [a.name for a in klass.ancestors()]
        self.assertEquals(ancs, ['YOUPI', 'YO', 'YO'])

from logilab.common.astng import utils

class UtilsTC(unittest.TestCase):
    def test_is_metaclass(self):
        klass = MODULE2.locals['Metaclass']
        self.assert_(utils.is_metaclass(klass))
        klass = MODULE2.locals['MyException']
        self.assert_(not utils.is_metaclass(klass))
        
    def test_is_interface(self):
        klass = MODULE2.locals['MyIFace']
        self.assert_(utils.is_interface(klass))
        klass = MODULE2.locals['MyException']
        self.assert_(not utils.is_interface(klass))
        
    def test_is_exception(self):
        klass = MODULE2.locals['MyException']
        self.assert_(utils.is_exception(klass))
        klass = MODULE2.locals['MyError']
        self.assert_(utils.is_exception(klass))
        klass = MODULE2.locals['MyIFace']
        self.assert_(not utils.is_exception(klass))
        
    def test_is_abstract(self):
        method = MODULE2.locals['AbstractClass'].locals['to_override']
        self.assert_(utils.is_abstract(method, pass_is_abstract=False))
        klass = MODULE2.locals['AbstractClass'].locals['return_something']
        self.assert_(not utils.is_abstract(klass, pass_is_abstract=False))
        # non regression : test raise "string" doesn't cause an exception in is_abstract
        func = MODULE2.locals['raise_string']
        self.assert_(not utils.is_abstract(func, pass_is_abstract=False))
        
    def test_get_interfaces(self):
        for klass, interfaces in (('Concrete0', ['MyIFace']),
                                  ('Concrete1', ['MyIFace', 'AnotherIFace']),
                                  ('Concrete2', ['MyIFace', 'AnotherIFace']),
                                  ('Concrete23', ['MyIFace', 'AnotherIFace'])):
            klass = MODULE2.locals[klass]
            self.assertEquals([i.name for i in utils.get_interfaces(klass)],
                              interfaces)
        
    def test_get_raises(self):
        method = MODULE2.locals['AbstractClass'].locals['to_override']
        self.assertEquals([str(term) for term in utils.get_raises(method)],
                          ["CallFunc(Name('NotImplementedError'), [], None, None)"] )
        
    def test_get_returns(self):
        method = MODULE2.locals['AbstractClass'].locals['return_something']
        # use string comp since Node doesn't handle __cmp__ 
        self.assertEquals([str(term) for term in utils.get_returns(method)],
                          ["Const('toto')", "Const(None)"])
    

class BuilderTC(unittest.TestCase):
    def test_noendingnewline(self):
        """check that a file with no trailing new line is parseable"""
        abuilder.file_build('data/noendingnewline.py', 'data.noendingnewline')
        
if __name__ == '__main__':
    unittest.main()
