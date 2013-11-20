from pydevd_constants import *

import _pydevd_bytecode
import os
import sys
import re
import inspect

# PyDev supports breaking on exceptions, but sometimes exceptions are expected
# or cannot be prevented.
#
# Breaking on an exception can be suppressed in the UI, by going to:
# Pydev > Manage Exception Breakpoints > Excluded exceptions, but this is only
# useful in code that does not change frequently, because the line numbers in
# the exclusion list will not update when the underlying code changes.
#
# For code that changes frequently, you can use the @IgnoreThrow tag to ignore
# exceptions thrown from a given line, and @IgnoreException to ignore exceptions
# that will be caught on a given line.
#
# E.g.:
# def expected_exception():
#     return 1 / 0 #@IgnoreException
# 
# def raises_exception():
#     return 1 / 0
# 
# def ignore_catch():
# try:
#     raises_exception() #@IgnoreException
# except:
#     pass 
IGNORE_EXCEPTION_TAG = re.compile('[^#]*#.*@IgnoreException')

# A mapping of function names to tuples of exceptions to ignore.  Exceptions
# of the given types thrown from any methods with these names will be skipped.
IGNORE_FUNCTION_EXCEPTIONS = {
          '__getattr__' : (AttributeError,),
          '__getitem__' : (IndexError,),
          '__getitem__' : (KeyError,),
    }

def get_catch_location(exc_info=None):
    """
    Return the (code object, line number) that will handle the given (or current) exception.
    Returns (None, None) if the exception will not be handled.
    """
    if exc_info is None:
        exc_info = sys.exc_info()
    exc_type, _, exc_traceback = exc_info
    if exc_type is None:
        raise ValueError("exc_type cannot be None")
    if exc_traceback is None:
        raise ValueError("exc_traceback cannot be None")

    if hasattr(_pydevd_bytecode, 'get_handled_exceptions'):
        # The traceback is a linked list, starting with the top of the stack.
        current = exc_traceback.tb_frame
        while current is not None:
            handlers = _pydevd_bytecode.get_handled_exceptions(current.f_code, current.f_lineno)
    
            if handlers.will_handle(exc_type):
                return current.f_code, current.f_lineno
    
            current = current.f_back

    return None, None

def filename_to_module_fqn(filename):
    """
    Parse a filename of a python code file and return a string representing
    the fully-qualified module.
    """
    prefix_list = sorted([os.path.commonprefix([os.path.abspath(m), filename]) \
                              for m in sys.path if filename.startswith(os.path.abspath(m))], \
                              key=len, reverse=True)
    if not prefix_list:
        sys.stderr.write('Path %s not under sys.path: %s \n' % (filename, sys.path))
        return None

    prefix = prefix_list[0]
    rel_path = os.path.relpath(filename, prefix)
    norm_path = os.path.normpath(rel_path)
    module_name = norm_path.replace('.py', '')
    module_name = module_name.replace('\\__init__', '')

    if IS_PY3K:
        fqn = module_name.translate(str.maketrans('\\/', '..'))
    else:
        import string
        fqn = module_name.translate(string.maketrans('\\/', '..'))
        
    fqn = fqn.strip('.')
    return fqn

def _check_ignore_list(co, lineno, ignore_set, tag_cache, tag_re):
    """
    Return True if the given code object and line are in the ignore_set.
    """
    if co.co_filename is not None:
        module_fqn = filename_to_module_fqn(co.co_filename)
        if module_fqn is None:
            return False
        key = (module_fqn, lineno)
        if key in ignore_set:
            return True
        if key in tag_cache:
            return tag_cache[key]

        # Look at the source (if available) and see whether an annotation
        # exists to ignore exceptions thrown or caught by this line.
        # Cache the result so that we don't have to perform this check
        # in the future.
        ignore_line = False
        try:
            lines, first_line = inspect.getsourcelines(co)
            index = lineno - first_line
            if 0 <= index < len(lines):
                src = lines[index].strip()
                sys.stderr.write("Exception src is '{}'\n".format(src))
                ignore_line = tag_re.match(src) is not None
        except:
            pass
        tag_cache[key] = ignore_line
        return ignore_line

    return False

def _except_hook_force_ignore(exc_type, exc_value, exc_traceback):
    """
    Custom PyDev exception hook filter for ignoring exceptions.
    
    Receives arguments as given by sys.exc_info()
    exc_type: The type of the exception that is being thrown.
    exc_value: The exception instance that is currently being thrown.
    exc_traceback: The traceback of the thrown exception.
    
    Return True if this exception should be ignored even if in the filter list.
    """
    import pydevd
    main_debugger = pydevd.GetGlobalDebugger()

    frame_top = exc_traceback.tb_frame

    # Check whether this throw location is in the ignore list.
    if hasattr(frame_top, 'f_code'):
        co = frame_top.f_code
        if _check_ignore_list(co, frame_top.f_lineno,
                              main_debugger.ignore_throw_locations,
                              main_debugger.ignore_throw_locations_tag_cache,
                              IGNORE_EXCEPTION_TAG):
            sys.stderr.write("Ignoring exception <%s> thrown from '%s' [%s] \n" % (exc_type.__name__, co.co_filename, frame_top.f_lineno))
            return True

    # Check for exceptions to ignore when thrown at specific locations.
    ignore_exceptions = IGNORE_FUNCTION_EXCEPTIONS.get(frame_top.f_code.co_name, None)
    if ignore_exceptions and issubclass(exc_type, ignore_exceptions):
        sys.stderr.write("Skipping exception <%s> at '%s' [%s] \n" % (exc_value, frame_top.f_code.co_name, frame_top.f_lineno))
        return True

    # Scan the callstack to determine if and where this exception will be caught.
    catch_co, catch_line = get_catch_location(exc_info=(exc_type, exc_value, exc_traceback))

    # Debug logging
    if __debug__:
        if catch_co is None:
            sys.stderr.write("Could not determine catch location for <%s> \n" % (exc_type.__name__))
        else:
            sys.stderr.write("Will catch <%s> at '%s' [%s] \n" % (exc_type.__name__, catch_co.co_filename, catch_line))

    # Check for exceptions to ignore when caught at specific locations.
    if catch_co is not None:
        if _check_ignore_list(catch_co, catch_line,
                              main_debugger.ignore_catch_locations,
                              main_debugger.ignore_catch_locations_tag_cache,
                              IGNORE_EXCEPTION_TAG):
            sys.stderr.write("Ignoring exception <%s> that will be caught at '%s' [%s] \n" % (exc_type.__name__, catch_co.co_filename, catch_line))
            return True

    # Check whether this exception is caught in the same code object as it is thrown.
    if  main_debugger.ignore_same_frame_exception and catch_co is not None:       
        # If the exception is going to be caught, check whether it is caught in the current frame.
        # If so, don't break.
        if catch_co is frame_top.f_code:
            return True

    return False

