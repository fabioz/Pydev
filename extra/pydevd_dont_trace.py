"""
Support for a tag that allows skipping over functions while debugging.
"""
import inspect
import linecache
import pydevd
import re
import weakref

# To suppress tracing a method, add the tag @DontTrace
# to a comment either preceding or on the same line as
# the method definition
#
# E.g.:
# #@DontTrace
# def test1():
#     pass
#
#  ... or ...
#
# def test2(): #@DontTrace
#     pass
DONT_TRACE_TAG = '@DontTrace'

# Regular expression to match a decorator (at the beginning
# of a line).
RE_DECORATOR = re.compile(r'^\s*@')

# Mapping from code object to bool.
# If the key exists, the value is the cached result of should_trace_hook
_code_trace_cache = weakref.WeakKeyDictionary()

def should_trace_hook(frame, event, arg):
    """
    Return True if this frame should be traced, False if tracing should be blocked.
    """
    # First, check whether this code object has a cached value
    try:
        co = frame.f_code
        result = _code_trace_cache.get(co)
        if result is not None:
            return result
    except:
        # If the frame doesn't have a code object, it doesn't matter much what we do
        return True

    # By default, trace all methods
    result = True

    # Now, look up that line of code and check for a @DontTrace
    # preceding or on the same line as the method.
    # E.g.:
    # #@DontTrace
    # def test():
    #     pass
    #  ... or ...
    # def test(): #@DontTrace
    #     pass
    try:
        comments = inspect.getcomments(co)
        if comments is not None and DONT_TRACE_TAG in comments:
            result = False
        else:
            lines, _ = inspect.getsourcelines(co)
            for line in lines:
                if DONT_TRACE_TAG in line:
                    result = False
                    break
                if not RE_DECORATOR.match(line):
                    break
    except:
        # If there is any exception, keep the default behavior which is to trace.
        pass

    # Cache the result for next time
    _code_trace_cache[co] = result
    return result

def clear_trace_filter_cache():
    """
    Clear the trace filter cache.
    Call this after reloading.
    """
    try:
        # Need to temporarily disable a hook because otherwise
        # _code_trace_cache.clear() will never complete.
        old_hook = pydevd.should_trace_hook
        pydevd.should_trace_hook = None
    
        # Clear the linecache
        linecache.clearcache()
        _code_trace_cache.clear()

    finally:
        pydevd.should_trace_hook = old_hook

def trace_filter(mode:bool=None):
    """
    Set the trace filter mode.
    mode: Whether to enable the trace hook.
      True: Trace filtering on (skipping methods tagged @DontTrace)
      False: Trace filtering off (trace methods tagged @DontTrace)
      None/default: Toggle trace filtering.
    """    
    if mode is None:
        mode = pydevd.should_trace_hook is None

    if mode:
        pydevd.should_trace_hook = should_trace_hook
    else:
        pydevd.should_trace_hook = None

    return mode

