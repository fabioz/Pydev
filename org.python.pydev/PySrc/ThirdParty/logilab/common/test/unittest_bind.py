"""unit tests for logilab.common.bind module"""

__revision__ = '$Id: unittest_bind.py,v 1.2 2005-04-19 14:39:13 fabioz Exp $'

import unittest
from sets import Set

from logilab.common import bind

HELLO = 'Hello'
def f():
    return HELLO

def modify_hello():
    global HELLO
    HELLO = 'hacked !'

import foomod

class BindTC(unittest.TestCase):
    """Test suite for bind module"""

    def test_simple_bind(self):
        """tests a simple global variable becomes a local one"""
        self.assertEquals(f(), HELLO)
        d = {'HELLO' : HELLO}
        new_f = bind.bind(f, d)
        self.assertEquals(new_f(), f())
        f_consts = f.func_code.co_consts
        newf_consts = new_f.func_code.co_consts
        self.assertEquals(f_consts, (None,))
        self.assert_(newf_consts, (None, HELLO))

    def test_optimize_on_a_func(self):
        """make sure optimize only workds for modules"""
        self.assertRaises(TypeError, bind.optimize_module, f, ('c1', 'c2'))
        self.assertRaises(TypeError, bind.optimize_module_2, f, ('c1', 'c2'))
        self.assertRaises(TypeError, bind.optimize_module, [], ('c1', 'c2'))
        self.assertRaises(TypeError, bind.optimize_module_2, [], ('c1', 'c2'))

    def test_analyze_code(self):
        """tests bind.analyze_code()"""
        consts_dict, consts_list = {}, []
        globs = {'HELLO' : "some global value"}
        modified = bind.analyze_code(modify_hello.func_code, globs,
                                     consts_dict, consts_list)
        self.assertEquals(consts_list, [None, 'hacked !'])
        self.assertEquals(modified, ['HELLO'])
    
    def test_optimize_module2(self):
        """test optimize_module_2()"""
        f1_consts = Set(foomod.f1.func_code.co_consts)
        f2_consts = Set(foomod.f2.func_code.co_consts)
        f3_consts = Set(foomod.f3.func_code.co_consts)
        bind.optimize_module_2(foomod, ['f1', 'f2', 'f3'])
        newf1_consts = Set(foomod.f1.func_code.co_consts)
        newf2_consts = Set(foomod.f2.func_code.co_consts)
        newf3_consts = Set(foomod.f3.func_code.co_consts)
        self.assert_(newf1_consts == newf2_consts == newf3_consts)
        self.assertEquals(newf1_consts, f1_consts | f2_consts | f3_consts)
    
if __name__ == '__main__':
    unittest.main()
    
