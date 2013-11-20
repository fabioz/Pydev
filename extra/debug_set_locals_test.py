"""
Unit tests for setting locals.
"""
import unittest
import ctypes
import inspect

# Max static block nesting within a function
CO_MAXBLOCKS = 20

# Maximum number of locals we can index.
MAX_LOCALS = 256

# Should be set to True if Py_TRACE_REFS is true, which is the case if linking
# against the debug Python libraries.  This could potentially be detected by
# using sys.getsizeof on a stack frame object.
HEAD_EXTRA = False

class PyTryBlockWrapper(ctypes.Structure):
    _fields_ = [('b_type', ctypes.c_int), # int -- what kind of block this is
                ('b_handler', ctypes.c_int), # int -- where to jump to find handler
                ('b_level', ctypes.c_int), # int -- value stack level to pop to
                ]

class PyObjectHeadWrapper(ctypes.Structure):
    if HEAD_EXTRA:
        _fields_ = [('ob_next', ctypes.c_void_p), # PyObject *
                    ('ob_prev', ctypes.c_void_p), # PyObject *
                    ('ob_refcnt', ctypes.c_ssize_t), # Py_ssize_t
                    ('ob_type', ctypes.c_void_p), # _typeobject *
                    ]
    else:
        _fields_ = [('ob_refcnt', ctypes.c_ssize_t), # Py_ssize_t
                    ('ob_type', ctypes.c_void_p), # _typeobject *
                    ]

class PyObjectHeadWrapperVar(ctypes.Structure):
    _anonymous_ = ['_base_']
    _fields_ = [('_base_', PyObjectHeadWrapper),
                ('ob_size', ctypes.c_ssize_t), # Py_ssize_t
                ]

class FrameWrapper(ctypes.Structure):
    _anonymous_ = ['_base_']
    _fields_ = [('_base_', PyObjectHeadWrapperVar),
                ('f_back', ctypes.c_void_p), # _frame * -- previous frame
                
                # Warning - STACKLESS only (f_code when STACKLESS is false)
                ('f_execute', ctypes.c_void_p), # PyFrame_ExecFunc * - Stackless only, PyCodeObject *f_code otherwise
                
                ('f_builtins', ctypes.py_object), # PyObject * -- builtin symbol table (PyDictObject)
                ('f_globals', ctypes.py_object), # PyObject * -- global symbol table (PyDictObject)
                ('f_locals', ctypes.py_object), # PyObject * -- local symbol table (any mapping)
                ('f_valuestack', ctypes.POINTER(ctypes.c_void_p)), # PyObject ** -- points after the last local

                 # Frame creation sets to f_valuestack.
                 # Frame evaluation usually NULLs it, but a frame that yields sets it
                 # to the current stack top
                ('f_stacktop', ctypes.POINTER(ctypes.c_void_p)), # PyObject ** -- Next free slot in f_valuestack.

                ('f_trace', ctypes.c_void_p), # PyObject * -- Trace function

                # In a generator, we need to be able to swap between the exception
                # state inside the generator and the exception state of the calling
                # frame (which shouldn't be impacted when the generator "yields"
                # from an except handler).
                # These three fields exist exactly for that, and are unused for
                # non-generator frames. See the SAVE_EXC_STATE and SWAP_EXC_STATE
                # macros in ceval.c for details of their use.
                ('f_exc_type', ctypes.c_void_p), # PyObject *
                ('f_exc_value', ctypes.c_void_p), # PyObject *
                ('f_exc_traceback', ctypes.c_void_p), # PyObject *

                ('f_tstate', ctypes.c_void_p), # PyThreadState *
                ('f_lasti', ctypes.c_int), # int -- Last instruction if called

                # Call PyFrame_GetLineNumber() instead of reading this field
                # directly.  As of 2.3 f_lineno is only valid when tracing is
                # active (i.e. when f_trace is set).  At other times we use
                # PyCode_Addr2Line to calculate the line from the current
                # bytecode index.
                ('f_lineno', ctypes.c_int),  # int -- Current line number, but see above
                ('f_iblock', ctypes.c_int), # int -- index in f_blockstack
                ('f_blockstack', PyTryBlockWrapper * CO_MAXBLOCKS), # PyTryBlock[CO_MAXBLOCKS] -- for try and loop blocks
                
                # Warning - STACKLESS only
                ('f_code', ctypes.c_void_p), # PyCodeObject * -- code segment - Stackless only!
                
                # Technically this is variable length, but to eliminate the need
                # to create multiple frame wrappers we'll set it to a constant.
                ('f_localsplus', ctypes.py_object * MAX_LOCALS), # PyObject *[N] -- locals+stack, dynamically sized
                ]

def _pretty_print(obj, header=None, exclude=None, include_dunder=False):
    """
    Print the fields of an object.
    For debugging only.
    """
    if header is not None:
        print("{}:".format(header))
        
    for name in dir(obj):
        if name.startswith('__') and not include_dunder:
            continue
        if exclude is None or name not in exclude:
            value = getattr(obj, name)
        else:
            value = "<redacted>"
        print(" - {}: {}".format(name, value))

def get_variable_index(co, name):
    """
    Get the index of the given argument or local in the given code object. 
    """
    if name in co.co_varnames:
        return co.co_varnames.index(name)
    return None

def save_locals(locals_dict, frame):
    """
    Copy values from locals_dict into the fast stack slots in the given frame.
    """
    co = frame.f_code
    frame_pointer = ctypes.c_void_p(id(frame))
    frame_wrapper = ctypes.cast(frame_pointer, ctypes.POINTER(FrameWrapper))

    for i, name in enumerate(co.co_varnames):
        if name in locals_dict:
            frame_wrapper[0].f_localsplus[i] = locals_dict[name]

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
        self.assertEqual(x, 1) # Expected to fail

    def test_set_locals_using_ctypes(self):
        x = test_method(use_ctypes)
        self.assertEqual(x, 2) # Expected to succeed

    def test_set_locals_using_save_locals(self):
        x = test_method(use_save_locals)
        self.assertEqual(x, 2) # Expected to succeed
