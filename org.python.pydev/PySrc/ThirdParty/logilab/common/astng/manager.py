# Copyright (c) 2003-2005 Sylvain Thenault (thenault@gmail.com)
# Copyright (c) 2003-2005 LOGILAB S.A. (Paris, FRANCE).
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
"""astng manager: avoid multible astng build of a same module when
possible by providing a class responsible to get astng representation
from various source and using a cache of built modules)

:version:   $Revision: 1.5 $  
:author:    Sylvain Thenault
:copyright: 2003-2005 LOGILAB S.A. (Paris, FRANCE)
:copyright: 2003-2005 Sylvain Thenault
:contact:   http://www.logilab.fr/ -- mailto:python-projects@logilab.org
"""

__revision__ = "$Id: manager.py,v 1.5 2005-02-16 16:45:44 fabioz Exp $"
__doctype__ = "restructuredtext en"

import sys
import os
from os.path import dirname, basename, abspath, join, isdir, exists

from logilab.common.cache import Cache
from logilab.common.modutils import NoSourceFile, is_python_source, \
     file_from_modpath, get_module_files, load_module_from_name, get_source_file
from logilab.common.configuration import OptionsProviderMixIn
from logilab.common.astng import ASTNGBuildingException

def normalize_module_name(modname):
    """normalize a module name (i.e remove trailing __init__ if any)
    """
    parts = modname.split('.')
    if parts[-1] == '__init__':
        return '.'.join(parts[:-1])
    return modname

def astng_wrapper(func, modname):
    """wrapper to give to ASTNGManager.project_from_files"""
    print 'parsing %s...' % modname
    try:
        return func(modname)
    except ASTNGBuildingException, ex:
        print ex
    except KeyboardInterrupt:
        raise
    except Exception, ex:
        import traceback
        traceback.print_exc()


class ASTNGManager(OptionsProviderMixIn):
    """the astng manager, responsible to build astng from files
     or modules.

    Use the Borg pattern.
    """
    name = 'astng loader'
    options = (("ignore",
                {'action' :"append", 'type' : "string", 'metavar' : "<file>",
                 'dest' : "black_list", "default" : ('CVS',),
                 'help' : "add <file> (may be a directory) to the black list\
. It should be a base name, not a path. You may set this option multiple times\
."}),
               ("project",
                {'default': "No Name", 'type' : 'string', 'short': 'p',
                 'metavar' : '<project name>',
                 'help' : 'set the project name.'}),
               )
    brain = {}    
    def __init__(self):
        self.__dict__ = ASTNGManager.brain
        if not self.__dict__:
            OptionsProviderMixIn.__init__(self)
            self._cache = Cache(200)

    def set_cache_size(self, cache_size):
        """set the cache size
        """
        self._cache = Cache(cache_size)

    def from_directory(self, directory, modname=None):
        """given a module name, return the astng object"""
        modname = modname or basename(directory)
        directory = abspath(directory)
        return Package(directory, modname, self)

    def astng_from_file(self, filepath, modname=None, fallback=True):
        """given a module name, return the astng object"""
        try:
            filepath = get_source_file(filepath)
            source = True
        except NoSourceFile:
            source = False
        try:
            return self._cache[filepath]
        except KeyError:
            if source:
                try:
                    from logilab.common.astng.builder import ASTNGBuilder
                    astng = ASTNGBuilder(self).file_build(filepath, modname)
                except SyntaxError:
                    raise
                except Exception, ex:
                    #if __debug__:
                    #    import traceback
                    #    traceback.print_exc()
                    msg = 'Unable to load module %s (%s)' % (modname, ex)
                    raise ASTNGBuildingException(msg)
            elif fallback and modname:
                return self.astng_from_module_name(modname)
        self._cache[filepath] = astng
        return astng
    
    from_file = astng_from_file # backward compat
    
    def astng_from_module_name(self, modname, context_file=None):
        """given a module name, return the astng object"""
        try:
            filepath = file_from_modpath(modname.split('.'),
                                         context_file=context_file)
        except ImportError, ex:
            msg = 'Unable to load module %s (%s)' % (modname, ex)
            raise ASTNGBuildingException(msg)
        if filepath is None or not is_python_source(filepath):
            # FIXME: context
            return self.astng_from_module(load_module_from_name(modname),
                                          modname)
        return self.astng_from_file(filepath, modname, fallback=False)
                                 
    def astng_from_module(self, module, modname=None):
        """given an imported module, return the astng object"""
        try:
            # some builtin modules don't have __file__ attribute
            filepath = module.__file__
            if is_python_source(filepath):
                return self.astng_from_file(filepath, modname or module.__name__)
        except AttributeError:
            pass
        try:
            return self._cache[modname]
        except KeyError:
            from logilab.common.astng.builder import ASTNGBuilder
            astng = ASTNGBuilder(self).build_from_module(module, modname)
            # update caches
            self._cache[modname] = astng
            return astng
            
    def astng_from_class(self, klass, modname=None):
        """get astng for the given class"""
        if modname is None:
            try:
                modname = klass.__module__
            except AttributeError:
                raise ASTNGBuildingException(
                    'Unable to get module for class %s' % klass)
        modastng = self.astng_from_module_name(modname)
        return modastng.resolve(klass.__name__)

    def project_from_files(self, files, func_wrapper=astng_wrapper,
                           project_name=None, black_list=None):
        """return a Project from a list of files or modules"""
        # insert current working directory to the python path to have a correct
        # behaviour
        sys.path.insert(0, os.getcwd())
        try:
            # build the project representation
            project_name = project_name or self.config.project
            black_list = black_list or self.config.black_list
            project = Project(project_name)
            for something in files:
                if not exists(something):
                    fpath = file_from_modpath(something.split('.'))
                elif isdir(something):
                    fpath = join(something, '__init__.py')
                else:
                    fpath = something
                astng = func_wrapper(self.astng_from_file, fpath)
                if astng is None:
                    continue
                project.path = project.path or astng.file
                project.add_module(astng)
                base_name = astng.name
                # recurse in package except if __init__ was explicitly given
                if astng.package and not '__init__' in something:
                    # recurse on others packages / modules if this is a package
                    for fpath in get_module_files(dirname(astng.file),
                                                  black_list):
                        astng = func_wrapper(self.astng_from_file, fpath)
                        if astng is None or astng.name == base_name:
                            continue
                        project.add_module(astng)
            return project
        finally:
            sys.path.pop(0)
    


class Package:
    """a package using a dictionary like interface

    load submodules lazily, as they are needed
    """
    
    def __init__(self, path, name, manager):
        self.name = name
        self.path = abspath(path)
        self.manager = manager
        self.parent = None
        self.lineno = 0
        self.__keys = None
        self.__subobjects = None

    def fullname(self):
        """return the full name of the package (i.e. prefix by the full name
        of the parent package if any
        """
        if self.parent is None:
            return self.name
        return '%s.%s' % (self.parent.fullname(), self.name)
    
    def get_subobject(self, name):
        """method used to get sub-objects lazily : sub package or module are
        only build once they are requested
        """
        if self.__subobjects is None:
            self.__subobjects = dict.fromkeys(self.keys())
        obj = self.__subobjects[name]
        if obj is None:
            objpath = join(self.path, name)
            if isdir(objpath):
                obj = Package(objpath, name, self.manager)
                obj.parent = self
            else:
                modname = '%s.%s' % (self.fullname(), name)
                obj = self.manager.astng_from_file(objpath + '.py', modname)
            self.__subobjects[name] = obj
        return obj
    
    def get_module(self, modname):
        """return the Module or Package object with the given name if any
        """
        path = modname.split('.')
        if path[0] != self.name:
            raise KeyError(modname)
        obj = self
        for part in path[1:]:
            obj = obj.get_subobject(part)
        return obj
    
    def keys(self):
        if self.__keys is None:
            self.__keys = []
            for fname in os.listdir(self.path):
                if fname.endswith('.py'):
                    self.__keys.append(fname[:-3])
                    continue
                abspath = join(self.path, fname)
                if isdir(abspath) and exists(join(abspath, '__init__.py')):
                    self.__keys.append(fname)
            self.__keys.sort()
        return self.__keys[:]
    
    def values(self):
        return [self.get_subobject(name) for name in self.keys()]
        
    def items(self):
        return zip(self.keys(), self.values())
    
    def has_key(self, name):
        return bool(self.get(name))
    
    def get(self, name, default=None):
        try:
            return self.get_subobject(name)
        except KeyError:
            return default
        
    def __getitem__(self, name):
        return self.get_subobject(name)        
    def __contains__(self, name):
        return self.has_key(name)
    def __iter__(self):
        return iter(self.keys())
    

class Project:
    """a project handle a set of modules / packages"""
    def __init__(self, name=''):
        self.name = name
        self.path = None
        self.modules = []
        self._modules = {}
        
    def add_module(self, node):
        self._modules[node.name] = node
        self.modules.append(node)
        
    def get_module(self, name):
        return self._modules[name]
    
    def getChildNodes(self):
        return self.modules

    def __repr__(self):
        return '<Project %r at %s (%s modules)>' % (self.name, id(self),
                                                    len(self.modules))
        
if __name__ == '__main__':
    obj_code = ASTNGManager().astng_from_module_name(sys.argv[1])
    print repr(obj_code)
    print obj_code.locals.keys()
    #print [str(c.__class__) for c in obj_code.children]
    print obj_code.doc


