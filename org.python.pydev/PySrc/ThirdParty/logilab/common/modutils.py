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

__revision__ = "$Id: modutils.py,v 1.4 2005-01-21 17:42:03 fabioz Exp $"

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
    for path in sys.path:
        path = abspath(path)
        if path and base[:len(path)] == path:
            if filename.find('site-packages') != -1 and \
                   path.find('site-packages') == -1:
                continue
            mod_path = [module for module in base[len(path):].split(os.sep) if module]
            if len(mod_path) > 1:
                if not has_init(join(path, mod_path[0])):
                    continue
                break
            break
    else:
        raise Exception('Unable to find module for %s in %s' % (
            base, ', \n'.join(sys.path)))
    return mod_path


_PYEXTS = {'so':1, 'dll':1, 'py':1, 'pyo':1, 'pyc':1}

_HAS_MOD_CACHE = {}

def has_module(directory, modname, _exts=_PYEXTS):
    """return the path corresponding to modname in directory if it exists,
    else raise ImportError
    """
    try:
        if _HAS_MOD_CACHE[(directory, modname)] is None:
            raise ImportError('No module %r in %s' % (modname, directory))
        return _HAS_MOD_CACHE[(directory, modname)]
    except KeyError:
        pass
    # FIXME: zip import !
    filepath = join(directory, modname)
    if isdir(filepath) and has_init(filepath):
        _HAS_MOD_CACHE[(directory, modname)] = filepath
        return filepath
    try:
        for filename in os.listdir(directory):
            try:
                basename, ext = filename.split('.', 1)#splitext(filename)
            except ValueError:
                continue
            #basename, ext = splitext(filename)
            if basename == modname and ext in _exts:
                filepath = '%s.%s' % (filepath, ext)
                _HAS_MOD_CACHE[(directory, modname)] = filepath
                return filepath
    except OSError:
        pass
    _HAS_MOD_CACHE[(directory, modname)] = None
    raise ImportError('No module %r in %s' % (modname, directory))


def file_from_modpath(modpath, search_path=None):
    if modpath[0] == 'xml':
        # handle _xmlplus
        try:
            return _file_from_modpath(['_xmlplus'] + modpath[1:], search_path)
        except ImportError:
            pass
    elif modpath == ['os', 'path']:
        # FIXME: ignore search_path...
        return os.path.__file__
    return _file_from_modpath(modpath, search_path)

def _file_from_modpath(modpath, search_path=None):
    """given a mod path (i.e. splited module / package name), return the
    corresponding file

    FIXME: doesn't handle zip archive...
    """
    assert len(modpath) > 0
    search_path = search_path or sys.path
    for path in search_path:
        path = abspath(path)
        # ignore bin directory
        if path.endswith('/bin'):
            continue
        try:
            found = has_module(path, modpath[0])
        except ImportError:
            continue
        for part in modpath[1:]:
            found = has_module(found, part)
        if isdir(found):
            found = has_init(found)
        return found
    raise ImportError('No module %r' % '.'.join(modpath))
        
    
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
        # don't use +=, we want a new list to be created !
        path = [dirname(context_file)] + path
    for i in range(len(parts)):
        try:
            file_from_modpath(parts[:i+1], path)
        except ImportError, ex:
            if not i >= max(1, len(parts) - 2):
                raise
            return '.'.join(parts[:i])
    return dotted_name

    
def is_standard_module(modname, std_path=(STD_LIB_DIR,)):
    """
    return true if the module may be considered as a module from the standard
    library or is a built-in module
    """
    modpath = modname.split('.')
    modname = modpath[0]
    try:
        filename = file_from_modpath(modpath)
    except ImportError:
        try:
            filename = find_module(modname)[1]
            # modules which are not living in a file are considered standard
            # (sys and __builtin__ for instance)
            # see imp.find_module documentation for more explanations
            if not filename or filename == modname:
                return True
        except ImportError:
            # import failed, i'm probably not so wrong by supposing it's
            # not standard...
            return False
    for path in std_path:
        path = abspath(path)
        if filename.startswith(path):
            pfx_len = len(path)
            if filename[pfx_len+1:pfx_len+14] != 'site-packages':
                return True
            return False
    return False


def has_init(directory):
    """if the given directory has a valid __init__ file, return its path,
    else return None
    """
    mod_or_pack = join(directory, '__init__')
    for ext in ('.py', '.pyc', '.pyo', '.pyd'):
        if exists(mod_or_pack + ext):
            return mod_or_pack + ext
    return None
    

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
            return True
    # or it a relative package ?
    return has_init(mod_or_pack) and True or False


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
