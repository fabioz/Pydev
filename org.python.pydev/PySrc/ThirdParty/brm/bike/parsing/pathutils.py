
# A some of this code is take from Pythius -
# Copyright (GPL) 2001 Jurgen Hermann <jh@web.de>

from bike.globals import *
import os

def containsAny(str, set):
    """ Check whether 'str' contains ANY of the chars in 'set'
    """
    return 1 in [c in str for c in set]



def getPathOfModuleOrPackage(dotted_name, pathlist = None):
    """ Get the filesystem path for a module or a package.

        Return the file system path to a file for a module,
        and to a directory for a package. Return None if
        the name is not found, or is a builtin or extension module.
    """
    from bike.parsing.newstuff import getPythonPath
    if pathlist is None:
        pathlist = getPythonPath()

    import imp

    # split off top-most name
    parts = dotted_name.split('.', 1)

    if len(parts) > 1:
        # we have a dotted path, import top-level package
        try:
            file, pathname, description = imp.find_module(parts[0], pathlist)
            if file: file.close()
        except ImportError:
            return None

        # check if it's indeed a package
        if description[2] == imp.PKG_DIRECTORY:
            # recursively handle the remaining name parts
            pathname = getPathOfModuleOrPackage(parts[1], [pathname])
        else:
            pathname = None
    else:
        # plain name
        try:
            file, pathname, description = imp.find_module(dotted_name, pathlist)
            if file: file.close()
            if description[2]not in[imp.PY_SOURCE, imp.PKG_DIRECTORY]:
                pathname = None
        except ImportError:
            pathname = None

    return pathname


def getFilesForName(name):
    """ Get a list of module files for a filename, a module or package name,
        or a directory.
    """
    import imp

    if not os.path.exists(name):
        # check for glob chars
        if containsAny(name, "*?[]"):
            import glob
            files = glob.glob(name)
            list = []
            for file in files:
                list.extend(getFilesForName(file))
            return list

        # try to find module or package
        name = getPathOfModuleOrPackage(name)
        if not name:
            return[]

    if os.path.isdir(name):
        # find all python files in directory
        list = []
        os.path.walk(name, _visit_pyfiles, list)
        return list
    elif os.path.exists(name) and not name.startswith("."):
        # a single file
        return [name]

    return []

def _visit_pyfiles(list, dirname, names):
    """ Helper for getFilesForName().
    """
    # get extension for python source files
    if not globals().has_key('_py_ext'):
        import imp
        global _py_ext
        _py_ext = [triple[0]for triple in imp.get_suffixes()if triple[2] == imp.PY_SOURCE][0]

    # don't recurse into CVS or Subversion directories
    if 'CVS'in names:
        names.remove('CVS')
    if '.svn'in names:
        names.remove('.svn')

    names_copy = [] + names
    for n in names_copy:
        if os.path.isdir(os.path.join(dirname, n))and \
           not os.path.exists(os.path.join(dirname, n, "__init__.py")):
            names.remove(n)

    # add all *.py files to list
    list.extend(
    [os.path.join(dirname, file)
    for file in names
    if os.path.splitext(file)[1] == _py_ext and not file.startswith(".")])


# returns the directory which holds the first package of the package
# hierarchy under which 'filename' belongs
def getRootDirectory(filename):
    if os.path.isdir(filename):
        dir = filename
    else:
        dir = os.path.dirname(filename)
    while dir != "" and \
          os.path.exists(os.path.join(dir, "__init__.py")):
        dir = os.path.dirname(dir)
    return dir



# Returns the root package directoryname of the package hierarchy
# under which 'filename' belongs
def getPackageBaseDirectory(filename):
    if os.path.isdir(filename):
        dir = filename
    else:
        dir = os.path.dirname(filename)

    if not os.path.exists(os.path.join(dir, "__init__.py")):
        # parent dir is not a package
        return dir

    while dir != "" and \
          os.path.exists(os.path.join(os.path.dirname(dir), "__init__.py")):
        dir = os.path.dirname(dir)
    return dir



def filenameToModulePath(fname):
    directoriesPreceedingRoot = getRootDirectory(fname)
    import os
    # strip off directories preceeding root package directory
    if directoriesPreceedingRoot != "":
        mpath = fname.replace(directoriesPreceedingRoot, "")
    else:
        mpath = fname

    if(mpath[0] == os.path.normpath("/")):
        mpath = mpath[1:]
    mpath, ext = os.path.splitext(mpath)
    mpath = mpath.replace(os.path.normpath("/"), ".")
    return mpath

