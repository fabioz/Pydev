# Copyright (c) 2003 Sylvain Thenault (thenault@nerim.net)
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
"""astng loader, avoid multible astng build of a same module
"""

__author__ = "Sylvain Thenault"
__revision__ = "$Id: manager.py,v 1.3 2004-10-28 16:01:19 fabioz Exp $"

import sys
import os
from os.path import normpath, dirname, basename, abspath, join, isdir, exists

from logilab.common.cache import Cache
from logilab.common.astng import ASTNGBuildingException
from logilab.common.modutils import modpath_from_file, file_from_modpath, \
     get_modules, load_module_from_name


def normalize_module_name(modname):
    """normalize a module name (i.e remove trailing __init__ if any)
    """
    parts = modname.split('.')
    if parts[-1] == '__init__':
        return '.'.join(parts[:-1])
    return modname

def normalize_file_name(filename):
    """normalize a file name (i.e .pyc, .pyo, .pyd -> .py and normalize the
    path)
    """
    return normpath(filename.replace('.pyc', '.py').replace('.pyo', '.py').replace('.pyd', '.py'))

from logilab.common.configuration import OptionsProviderMixIn


class AbstractASTNGManager(OptionsProviderMixIn):
    """abstract class for astng manager, responsible to build astng from files
    and / or modules.

    Use the Borg pattern.
    """
    name = 'astng loader'
    options = (("ignore",
                {'action' :"append", 'type' : "string", 'metavar' : "<file>",
                 'dest' : "black_list", "default" : ('CVS',),
                 'help' : "Add <file> (may be a directory) to the black list\
. It should be a base name, not a path. You may set this option multiple times\
."}),
               ("project",
                {'default': "No Name", 'type' : 'string',
                 'metavar' : '<project name>',
                 'help' : 'set the project name.'}),
               )
    brain = {}    
    def __init__(self):
        self.__dict__ = ASTNGManager.brain
        if not self.__dict__:
            OptionsProviderMixIn.__init__(self)
            from logilab.common.astng.builder import ASTNGBuilder
            self._builder = ASTNGBuilder()
            self._cache = Cache(200)

    def set_cache_size(self, cache_size):
        """set the cache size
        """
        self._cache = Cache(cache_size)

    # astng manager iface
    
    def astng_from_module_name(self, modname):
        """given a module name, return the astng object"""
        raise NotImplementedError

    def astng_from_module(self, module, modname=None):
        """given an imported module, return the astng object"""
        raise NotImplementedError

    def astng_from_class(self, klass, module=None):
        """get astng for the given class"""
        raise NotImplementedError

    def astng_from_file(self, filepath, modname=None):
        """given a module name, return the astng object"""
        raise NotImplementedError

class FileBasedASTNGManager(AbstractASTNGManager):
    """build astng from files or modules, but mainly modules"""

    def from_directory(self, directory, modname=None):
        """given a module name, return the astng object"""
        modname = modname or basename(directory)
        directory = normpath(directory)
        return Package(directory, modname, self)

    def astng_from_file(self, filepath, modname=None):
        """given a module name, return the astng object"""
        norm_file = normalize_file_name(filepath)
        try:
            return self._cache[norm_file]
        except KeyError:
            try:
                astng = self._builder.file_build(norm_file, modname)
            except SyntaxError:
                raise
            except Exception, ex:
                import sys;exc_info = sys.exc_info()
                import traceback;traceback.print_exception(exc_info[0], exc_info[1], exc_info[2])
                
                msg = 'Unable to load module %s (%s)' % (modname, ex)
                raise ASTNGBuildingException(msg)
        self._cache[norm_file] = astng
        return astng
    from_file = astng_from_file
    
    def astng_from_module_name(self, modname):
        """given a module name, return the astng object"""
        print '\n\n*********-------------- modname %s\n\n'% modname
        try:
            filepath = file_from_modpath(modname.split('.'))
        except ImportError:
            import sys;exc_info = sys.exc_info()
            import traceback;traceback.print_exception(exc_info[0], exc_info[1], exc_info[2])
            
            return self.astng_from_module(load_module_from_name(modname), modname)
        
        if filepath.endswith("py") or filepath.endswith("pyc"):
            return self.astng_from_file(filepath, modname)
        else:
            mod = load_module_from_name(modname)
            print '###############------------ mod = %s modname = %s\n\n' % (mod,modname)
            return self.astng_from_module(mod, modname)
                         
                                 
    def astng_from_module(self, module, modname=None):
        """given an imported module, return the astng object"""
        try:
            return self.astng_from_file(module.__file__, modname or module.__name__)
        except (ASTNGBuildingException , AttributeError ):
            # builtin modules don't have __file__ attribute
            try:
                return self._cache[modname]
            except KeyError:
                astng = self._builder.build_from_module(module, modname)
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

    
class ModuleBasedASTNGManager(AbstractASTNGManager):
    """build astng from files or modules, but mainly files"""
    

    def astng_from_class(self, klass, modname=None):
        """get astng for the given class"""
        if modname is None:
            try:
                modname = klass.__module__
            except AttributeError:
                raise ASTNGBuildingException(
                    'Unable to get module for class %s' % klass)
        return self.astng_from_module_name(modname).resolve(klass.__name__)

    def astng_from_module(self, module, modname=None):
        """given an imported module, return the astng object"""
        try:
            return self._cache[modname or module.__name__]
        except:
            if hasattr(module, '__file__'):
                # this is required to make works relative imports in
                # analyzed code
                sys.path.insert(0, dirname(module.__file__))
            try:
                astng = self._builder.build_from_module(module, modname)
                # update caches
                self._cache[astng.name] = astng
                return astng
            finally:
                # clean path
                if hasattr(module, '__file__'):
                    sys.path.pop(0)

    def astng_from_module_name(self, modname):
        """given a module name, return the astng object"""
        norm_modname = normalize_module_name(modname)
        try:
            return self._cache[norm_modname]
        except KeyError:
            try:
                module = load_module_from_name(norm_modname)
            except Exception, ex:
                msg = 'Unable to load module %s (%s)' % (modname, ex)
                raise ASTNGBuildingException(msg)
        # return the astng representation
        return self.astng_from_module(module, norm_modname)

    def astng_from_file(self, filepath, modname=None):
        """given a module name, return the astng object"""
        norm_modname = normalize_module_name(modname)
        try:
            return self._cache[norm_modname]
        except KeyError:
            try:
                astng = self._builder.file_build(filepath, modname)
            except Exception, ex:
                msg = 'Unable to load module %s (%s)' % (modname, ex)
                raise ASTNGBuildingException(msg)
        self._cache[norm_modname] = astng
        return astng

    def project_from_files(self, files, func_wrapper,
                           project_name=None, black_list=None):
        """return a Project from a list of files or modules
        """
        # insert current working directory to the python path to have a correct
        # behaviour
        sys.path.insert(0, os.getcwd())
        try:
            # build the project representation
            project_name = project_name or self.config.project
            black_list = black_list or self.config.black_list
            project = Project(project_name)
            modnames = []
            for modname in files:
                if modname[-3:] == '.py' or modname.find(os.sep) > -1:
                    modname = '.'.join(modpath_from_file(modname))
                astng = func_wrapper(self.astng_from_module_name, modname)
                if astng is None:
                    continue
                project.base_file = project.base_file or astng.file
                project.add_module(astng)
                # recurse in package except if __init__ was explicitly given
                if not modname.endswith('.__init__') and astng.package:
                    # recurse on others packages / modules if this is a package
                    for submod in get_modules(modname, dirname(astng.file),
                                              black_list):
                        astng = func_wrapper(self.astng_from_module_name, submod)
                        if astng is None:
                            continue
                        project.add_module(astng)
            return project
        finally:
            sys.path.pop(0)

    
ASTNGManager = FileBasedASTNGManager


class Package:
    """a package using a dictionary interface

    load submodules as needed
    """
    
    def __init__(self, path, name, manager):
        self.name = name
        self.path = abspath(path)
        self.manager = manager
        self.parent = None
        self.lineno = 0
        self.__keys = None
        self.__subobjects = None
    
    def get_subobject(self, name):
        if self.__subobjects is None:
            self.__subobjects = dict.fromkeys(self.keys())
        obj = self.__subobjects[name]
        if obj is None:
            abspath = join(self.path, name)
            if isdir(abspath):
                obj = Package(abspath, name, self.manager)
                obj.parent = self
            else:
                abspath += '.py'
                obj = self.manager.astng_from_file(abspath, name)
            self.__subobjects[name] = obj
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
            print 'computed keys', self.__keys
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
    """a project handle a set of modules"""
    def __init__(self, name=''):
        self.name = name
        self.base_file = None
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


