
from pydevd_comm import GetGlobalDebugger
from pydevd_constants import * #@UnusedWildImport
import pydevd_tracing

def replace_builtin_property():
    if not IS_PY3K:
        try:
            import __builtin__
            __builtin__.__dict__['property'] = DebugProperty
        except:
            if DEBUG_TRACE_LEVEL:
                import traceback;traceback.print_exc() #@Reimport
    else:
        try:
            import builtins #Python 3.0 does not have the __builtin__ module @UnresolvedImport
            builtins.__dict__['property'] = DebugProperty
            print(builtins.__dict__['property'])
        except:
            if DEBUG_TRACE_LEVEL:
                import traceback;traceback.print_exc() #@Reimport
        
class DebugProperty(object):
    """A custom property which allows python property to get  
    controlled by the debugger and selectively disable/re-enable
    the tracing.
    """

    def __init__(self, fget=None, fset=None, fdel=None, doc=None):
        self.fget = fget
        self.fset = fset
        self.fdel = fdel
        self.__doc__ = doc

    def __get__(self, obj, objtype=None):
        try:
            pydevd_tracing.SetTrace(None)
            if obj is None:
                return self         
            if self.fget is None:
                raise AttributeError("unreadable attribute")
            return self.fget(obj)
        finally:
            pydevd_tracing.SetTrace(GetGlobalDebugger().trace_dispatch)

    def __set__(self, obj, value):
        try:
            pydevd_tracing.SetTrace(None)
            if self.fset is None:
                raise AttributeError("can't set attribute")
            self.fset(obj, value)
        finally:
            pydevd_tracing.SetTrace(GetGlobalDebugger().trace_dispatch)


    def __delete__(self, obj):
        try:
            pydevd_tracing.SetTrace(None)
            if self.fdel is None:
                raise AttributeError("can't delete attribute")
            self.fdel(obj)
        finally:
            pydevd_tracing.SetTrace(GetGlobalDebugger().trace_dispatch)

    def setter(self, fset):
        """Overriding setter decorator for the property
        """
        if self.fset is None:
            self.fset = fset

    def deleter(self, fdel):
        """Overriding deleter decorator for the property
        """
        if self.fdel is None:
            self.fdel = fdel
