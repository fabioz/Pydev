# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation; either version 2 of the License, or (at your option) any later
# version.
#
# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details
#
# You should have received a copy of the GNU General Public License along with
# this program; if not, write to the Free Software Foundation, Inc.,
# 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
""" Copyright (c) 2002-2003 LOGILAB S.A. (Paris, FRANCE).
 http://www.logilab.fr/ -- mailto:contact@logilab.fr

module manipulation utilities
"""

__revision__ = "$Id: modutils.py,v 1.2 2004-10-26 14:18:34 fabioz Exp $"

from __future__ import nested_scopes
import os
import sys
from os.path import walk, splitext, join, abspath, isdir, dirname, exists
from imp import find_module, load_module

# get standard library directory for the running python version
STD_LIB_DIR = join(sys.prefix, 'lib', 'python%s' % sys.version[:3])


def load_module_from_name(dotted_name, path=None, use_sys=1):
    """ load a python module from it's name """
    return load_module_from_parts(dotted_name.split('.'), path, use_sys)

def load_module_from_parts(parts, path=None, use_sys=1, prefix=None):
    """ load a python module from it's splitted name """
    if prefix is None and use_sys:
        # make tricks like "import os.path" working
        try:
            return sys.modules['.'.join(parts)]
        except KeyError:
            pass
    if prefix:
        name = '%s.%s' % (prefix, parts[0])
    else:
        name = parts[0]
    mp_file, mp_filename, mp_desc = find_module(parts[0], path)
    module = load_module(name, mp_file, mp_filename, mp_desc)
    if len(parts) == 1:
        return module
    return load_module_from_parts(parts[1:], [dirname(module.__file__)],
                                  use_sys, name)


def modpath_from_file(filename):
    """given an absolute file path return the python module's path as a list
    """
    base, ext = splitext(abspath(filename))
    base = base.lower()
    for path in sys.path:
        path = abspath(path).lower()
        if path and base[:len(path)] == path:
            if filename.find('site-packages') != -1 and \
                   path.find('site-packages') == -1:
                continue
            mod_path = base[len(path):].split(os.sep)
            mod_path = [module for module in mod_path if module]
            # if the given filename has no extension, we  can suppose it's a 
            # directory,i.e. a python package. So check if there is a
            # __init__.py file
            if not (ext or exists(join(base[:len(path)],
                                       mod_path[0], '__init__.py'))):
                continue
            break
    else:
        raise Exception('Unable to find module for %s in %s' % (
            base, ', \n'.join(sys.path)))
    return mod_path


def has_module(directory, modname):
    """return the path corresponding to modname in directory if it exists,
    or None
    """
    # FIXME: zip !
    try:
        for filename in os.listdir(directory):
            # FIXME: check extension
            if splitext(filename)[0] == modname:
                filepath = join(directory, filename)
                # is it a package
                if isdir(filepath):
                    if exists(join(filepath, '__init__.py')):
                        return filepath
                    continue
                return filepath
    except OSError:
        return
    
def file_from_modpath(modpath):
    """given a mod path (i.e. splited module / package name), return the
    corresponding file

    FIXME: doesn't handle zip archive...
    """
    assert len(modpath) > 0
    for path in sys.path:
        # ignore bin directory
        if abspath(path).endswith('/bin'):
            continue
        found = has_module(abspath(path), modpath[0])
        if found is None:
            continue
        for part in modpath[1:]:
            found = has_module(found, part)
            if found is None:
                raise ImportError('.'.join(modpath))
        if isdir(found):
            found = join(found, '__init__.py')
        return found
    raise ImportError('.'.join(modpath))
        
    
def get_module_part(dotted_name, context_file=None):
    """given a dotted name like 'logilab.common.modutils.get_module',
    return the module part of the name (in the  previous example,
    'logilab.common.modutils' would be returned)

    return None if we are not able at all to import the given name
    """
    # os.path trick
    if dotted_name.startswith('os.path'):
        return 'os.path'
    parts = dotted_name.split('.')
    path = sys.path
    if context_file is not None:
        path = path + [dirname(context_file)]
    # import the first level package to get the correct context path
    result = []
    module = load_module_from_parts([parts[0]], path=path)
    # try to import first to avoid success on unexistent module
    if len(parts) == 1:
        # it must be a module
        return dotted_name
    result.append(parts.pop(0))
    to_pop = 0
    while parts and hasattr(module, '__path__'):
        name = parts[0]
        sys.path.insert(0, module.__path__[0])
        to_pop += 1
        try:
            mp_file, mp_filename, mp_desc = find_module(name, module.__path__)
            module = load_module(name, mp_file, mp_filename, mp_desc)
        except ImportError, ex:
            break
        result.append(parts.pop(0))
    while to_pop:
        sys.path.pop(0)
        to_pop -= 1
    return '.'.join(result)

    
def is_standard_module(modname, std_path=(STD_LIB_DIR,)):
    """
    return true if the module may be considered as a module from the standard
    library or is a built-in module
    """
    try:
        filename = file_from_modpath(modname.split('.'))
    except ImportError:
        try:
            filename = load_module_from_name(modname).__file__
        except AttributeError:
            return 1
        except:
            return 0
    for path in std_path:
        path = abspath(path)
        if filename.startswith(path):
            pfx_len = len(path)
            if filename[pfx_len+1:pfx_len+14] != 'site-packages':
                return 1
            return 0
    return 0


def is_relative(modname, from_file):
    """
    return true if the module is imported relativly to from file
    """
    package = modname.split('.')[0]
    if isdir(from_file):
        mod_or_pack = join(from_file, package)
    else:
        mod_or_pack = join(dirname(from_file), package)
    # is it a relative module ?
    for ext in ('.py', '.pyc', '.pyo', '.pyd', '.so'):
        if exists(mod_or_pack + ext):
            return 1
    # is it a relative package ?
    mod_or_pack = join(mod_or_pack, '__init__')
    for ext in ('.py', '.pyc', '.pyo', '.pyd'):
        if exists(mod_or_pack + ext):
            return 1
    # no, so it must be absolute
    return 0


def get_modules(package, src_directory, blacklist = ('CVS','debian')):
    """ given a directory return a list of all available python modules, even
    in subdirectories
    """
    def func(modules, directory, fnames):
        """walk handler"""
        # remove files/directories in the black list
        for norecurs in blacklist:
            try:
                fnames.remove(norecurs)
            except ValueError:
                continue
        # check for __init__.py
        if not '__init__.py' in fnames:
            while fnames:
                fnames.pop()
        elif directory != src_directory:
            #src = join(directory, file)
            dir_package = directory[len(src_directory):].replace(os.sep, '.')
            modules.append(package + dir_package)
        for filename in fnames:
            src = join(directory, filename)
            if isdir(src):
                continue
            if filename[-3:] in ('.py', '.so') and filename != '__init__.py':
                module = package + src[len(src_directory):-3]
                modules.append(module.replace(os.sep, '.'))
    modules = []
    walk(src_directory, func, modules)
    return modules

def get_module_files(src_directory, blacklist = ('CVS','debian')):
    """ given a directory return a list of all files available as python
    modules, even in subdirectories (ie subpackages
    """
    def func(files, directory, fnames):
        """walk handler"""
        # remove files/directories in the black list
        for norecurs in blacklist:
            try:
                fnames.remove(norecurs)
            except ValueError:
                continue
        # check for __init__.py
        if not '__init__.py' in fnames:
            while fnames:
                fnames.pop()            
        for filename in fnames:
            src = join(directory, filename)
            if isdir(src):
                continue
            if filename[-3:] in ('.py', '.so'):
                files.append(src)
    files = []
    walk(src_directory, func, files)
    return files
