import inspect
import unittest
import ctypes
from pydevd_save_locals import get_variable_index, save_locals, FrameWrapper


def use_locals_dict(name, value):
    """
    Attempt to set the local of the given name to value, using the locals dict.
    """
    frame = inspect.currentframe().f_back
    locals_dict = frame.f_locals
    locals_dict[name] = value


def use_ctypes(name, value):
    """
    Attempt to set the local of the given name to value, using ctypes.
    """
    frame = inspect.currentframe().f_back
    co = frame.f_code
    frame_pointer = ctypes.c_void_p(id(frame))
    frame_wrapper = ctypes.cast(frame_pointer, ctypes.POINTER(FrameWrapper))
    
    #_pretty_print(co, header="Code Object")
    #_pretty_print(frame, header="Frame", exclude=set(['f_builtins', 'f_globals']))
    #_pretty_print(frame_wrapper[0], header="Frame (wrapped)", exclude=set(['f_builtins', 'f_globals']))
    #for i, var_name in enumerate(('obj',) + co.co_varnames):
    #    print("Var {} [{}] = {}".format(var_name, i, frame_wrapper[0].f_localsplus[i]))

    index = get_variable_index(co, name)

    if index is None:
        raise KeyError("Variable {} not a name in {}".format(name, co))
    
    frame_wrapper[0].f_localsplus[index] = value


def use_save_locals(name, value):
    """
    Attempt to set the local of the given name to value, using locals_to_fast.
    """
    frame = inspect.currentframe().f_back
    locals_dict = frame.f_locals
    locals_dict[name] = value

    save_locals(locals_dict, frame)


def test_method(fn):
    """
    A harness for testing methods that attempt to modify the values of locals on the stack.
    """
    x = 1

    # The method 'fn' should attempt to set x = 2 in the current frame.
    fn('x', 2)
    
    return x



class TestSetLocals(unittest.TestCase):
    """
    Test setting locals in one function from another function using several approaches.
    """
    
    def test_set_locals_using_dict(self):
        x = test_method(use_locals_dict)
        self.assertEqual(x, 1)  # Expected to fail

    def test_set_locals_using_ctypes(self):
        x = test_method(use_ctypes)
        self.assertEqual(x, 2)  # Expected to succeed

    def test_set_locals_using_save_locals(self):
        x = test_method(use_save_locals)
        self.assertEqual(x, 2)  # Expected to succeed


if __name__ == '__main__':
    suite = unittest.TestSuite()
#    suite.addTest(TestSetLocals('test_set_locals_using_dict'))
#    #suite.addTest(Test('testCase10a'))
#    unittest.TextTestRunner(verbosity=3).run(suite)
    
    suite = unittest.makeSuite(TestSetLocals)
    unittest.TextTestRunner(verbosity=3).run(suite)
