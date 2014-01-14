"""
Based on the python xreload.

Changes:

1. we don't recreate the old namespace from new classes. Rather, we keep the existing namespace, load a new version of
it and update only some of the things we can inplace. That way, we don't break things such as singletons or end up with
a second representation of the same class in memory.

2. If we find it to be a __metaclass__, we try to update it as a regular class.

3. We don't remove old attributes (and leave them lying around even if they're no longer used).

These changes make it more stable, especially in the common case (where in a debug session only the contents of a
function are changed).




Original: http://svn.python.org/projects/sandbox/trunk/xreload/xreload.py
Note: it seems https://github.com/plone/plone.reload/blob/master/plone/reload/xreload.py enhances it (to check later)

Interesting alternative: https://code.google.com/p/reimport/

Alternative to reload().

This works by executing the module in a scratch namespace, and then
patching classes, methods and functions in place.  This avoids the
need to patch instances.  New objects are copied into the target
namespace.

Some of the many limitations include:

- Global mutable objects other than classes are simply replaced, not patched

- Code using metaclasses is not handled correctly

- Code creating global singletons is not handled correctly

- Functions and methods using decorators (other than classmethod and
  staticmethod) is not handled correctly

- Renamings are not handled correctly

- Dependent modules are not reloaded

- When a dependent module contains 'from foo import bar', and
  reloading foo deletes foo.bar, the dependent module continues to use
  the old foo.bar object rather than failing

- Frozen modules and modules loaded from zip files aren't handled
  correctly

- Classes involving __slots__ are not handled correctly
"""

import imp
import sys
import types
from pydev_imports import Exec
import pydevd_dont_trace
import traceback

NO_DEBUG = 0
LEVEL1 = 1
LEVEL2 = 2

#===================================================================================================================
# Helper
#=======================================================================================================================
class Helper:

    DEBUG = NO_DEBUG

    def write(*args):
        new_lst = []
        for a in args:
            new_lst.append(str(a))

        msg = ' '.join(new_lst)
        sys.stdout.write('%s\n' % (msg,))
    write = staticmethod(write)

    def info(*args):
        if Helper.DEBUG >= LEVEL1:
            Helper.write(*args)
    info = staticmethod(info)

    def info2(*args):
        if Helper.DEBUG >= LEVEL2:
            Helper.write(*args)
    info2 = staticmethod(info2)



#=======================================================================================================================
# code_objects_equal
#=======================================================================================================================
def code_objects_equal(code0, code1):
    for d in dir(code0):
        if d.startswith('_') or 'lineno' in d:
            continue
        if getattr(code0, d) != getattr(code1, d):
            return False
    return True


#=======================================================================================================================
# xreload
#=======================================================================================================================
def xreload(mod):
    """Reload a module in place, updating classes, methods and functions.

    mod: a module object
    """
    r = Reload(mod)
    r.apply()
    r = None
    pydevd_dont_trace.clear_trace_filter_cache()


#=======================================================================================================================
# Reload
#=======================================================================================================================
class Reload:

    def __init__(self, mod):
        self.mod = mod

    def apply(self):
        mod = self.mod
        self._on_finish_callbacks = []
        try:
            # Get the module name, e.g. 'foo.bar.whatever'
            modname = mod.__name__
            # Get the module namespace (dict) early; this is part of the type check
            modns = mod.__dict__
            # Parse it into package name and module name, e.g. 'foo.bar' and 'whatever'
            i = modname.rfind(".")
            if i >= 0:
                pkgname, modname = modname[:i], modname[i + 1:]
            else:
                pkgname = None
            # Compute the search path
            if pkgname:
                # We're not reloading the package, only the module in it
                pkg = sys.modules[pkgname]
                path = pkg.__path__  # Search inside the package
            else:
                # Search the top-level module path
                pkg = None
                path = None  # Make find_module() uses the default search path
            # Find the module; may raise ImportError
            (stream, filename, (suffix, mode, kind)) = imp.find_module(modname, path)
            # Turn it into a code object
            try:
                # Is it Python source code or byte code read from a file?
                if kind not in (imp.PY_COMPILED, imp.PY_SOURCE):
                    # Fall back to built-in reload()
                    Helper.info('Not patching in place (could not find source)')
                    return reload(mod)
                if kind == imp.PY_SOURCE:
                    source = stream.read()
                    code = compile(source, filename, "exec")
                else:
                    import marshal
                    code = marshal.load(stream)
            finally:
                if stream:
                    stream.close()
            # Execute the code.  We copy the module dict to a temporary; then
            # clear the module dict; then execute the new code in the module
            # dict; then swap things back and around.  This trick (due to
            # Glyph Lefkowitz) ensures that the (readonly) __globals__
            # attribute of methods and functions is set to the correct dict
            # object.
            new_namespace = modns.copy()
            new_namespace.clear()
            new_namespace["__name__"] = modns["__name__"]
            Exec(code, new_namespace)
            # Now we get to the hard part
            oldnames = set(modns)
            newnames = set(new_namespace)

            # Update in-place what we can
            for name in oldnames & newnames:
                self._update(modns[name], new_namespace[name])

            # Create new tokens (note: not deleting existing)
            for name in newnames - oldnames:
                Helper.info('Created:', new_namespace[name])
                modns[name] = new_namespace[name]

            for c in self._on_finish_callbacks:
                c()
            del self._on_finish_callbacks[:]
        except:
            traceback.print_exc()


    def _update(self, oldobj, newobj):
        """Update oldobj, if possible in place, with newobj.

        If oldobj is immutable, this simply returns newobj.

        Args:
          oldobj: the object to be updated
          newobj: the object used as the source for the update
        """
        Helper.info2('Updating: ', oldobj)
        if oldobj is newobj:
            # Probably something imported
            return newobj

        if type(oldobj) is not type(newobj):
            # Cop-out: if the type changed, give up
            return newobj

        if hasattr(newobj, "__reload_update__"):
            # Provide a hook for updating
            return newobj.__reload_update__(oldobj)

        if hasattr(types, 'ClassType'):
            classtype = types.ClassType
        else:
            classtype = type

        if isinstance(newobj, classtype):
            return self._update_class(oldobj, newobj)

        if isinstance(newobj, types.FunctionType):
            return self._update_function(oldobj, newobj)

        if isinstance(newobj, types.MethodType):
            return self._update_method(oldobj, newobj)

        if isinstance(newobj, classmethod):
            return self._update_classmethod(oldobj, newobj)

        if isinstance(newobj, staticmethod):
            return self._update_staticmethod(oldobj, newobj)

        # New: dealing with metaclasses.
        if hasattr(newobj, '__metaclass__') and hasattr(newobj, '__class__') and newobj.__metaclass__ == newobj.__class__:
            return self._update_class(oldobj, newobj)

        # Not something we recognize, just give up
        return newobj


    # All of the following functions have the same signature as _update()


    def _update_function(self, oldfunc, newfunc):
        """Update a function object."""
        oldfunc.__doc__ = newfunc.__doc__
        oldfunc.__dict__.update(newfunc.__dict__)

        try:
            newfunc.__code__
            attr_name = '__code__'
        except AttributeError:
            newfunc.func_code
            attr_name = 'func_code'

        old_code = getattr(oldfunc, attr_name)
        new_code = getattr(newfunc, attr_name)
        if not code_objects_equal(old_code, new_code):
            Helper.info('Update function:', oldfunc)
            setattr(oldfunc, attr_name, new_code)

        try:
            oldfunc.__defaults__ = newfunc.__defaults__
        except AttributeError:
            oldfunc.func_defaults = newfunc.func_defaults

        return oldfunc


    def _update_method(self, oldmeth, newmeth):
        """Update a method object."""
        # XXX What if im_func is not a function?
        if hasattr(oldmeth, 'im_func') and hasattr(newmeth, 'im_func'):
            self._update(oldmeth.im_func, newmeth.im_func)
        elif hasattr(oldmeth, '__func__') and hasattr(newmeth, '__func__'):
            self._update(oldmeth.__func__, newmeth.__func__)
        return oldmeth


    def _update_class(self, oldclass, newclass):
        """Update a class object."""
        olddict = oldclass.__dict__
        newdict = newclass.__dict__

        oldnames = set(olddict)
        newnames = set(newdict)

        for name in newnames - oldnames:
            Helper.info('Created:', newdict[name], 'in', oldclass)
            setattr(oldclass, name, newdict[name])

        # Note: not removing old things...
        # for name in oldnames - newnames:
        #    Helper.info('Removed:', name, 'from', oldclass)
        #    delattr(oldclass, name)

        for name in oldnames & newnames - set(['__dict__', '__doc__']):
            self._update(olddict[name], newdict[name])

        if hasattr(oldclass, "__after_reload_update__"):
            # If a client wants to know about it, give him a chance.
            self._on_finish_callbacks.append(oldclass.__after_reload_update__)


    def _update_classmethod(self, oldcm, newcm):
        """Update a classmethod update."""
        # While we can't modify the classmethod object itself (it has no
        # mutable attributes), we *can* extract the underlying function
        # (by calling __get__(), which returns a method object) and update
        # it in-place.  We don't have the class available to pass to
        # __get__() but any object except None will do.
        self._update(oldcm.__get__(0), newcm.__get__(0))


    def _update_staticmethod(self, oldsm, newsm):
        """Update a staticmethod update."""
        # While we can't modify the staticmethod object itself (it has no
        # mutable attributes), we *can* extract the underlying function
        # (by calling __get__(), which returns it) and update it in-place.
        # We don't have the class available to pass to __get__() but any
        # object except None will do.
        self._update(oldsm.__get__(0), newsm.__get__(0))
