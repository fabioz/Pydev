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
"""Some file / file path manipulation utilities.

:version:   $Revision: 1.4 $  
:author:    Logilab
:copyright: 2003-2005 LOGILAB S.A. (Paris, FRANCE)
:contact:   http://www.logilab.fr/ -- mailto:python-projects@logilab.org

:group path manipulation: first_level_directory, relative_path, is_binary,\
files_by_ext, include_files_by_ext, exclude_files_by_ext, get_by_ext
:group file manipulation: norm_read, norm_open, lines, stream_lines, lines,\
write_open_mode, ensure_fs_mode, export
:sort: path manipulation, file manipulation



:type BASE_BLACKLIST: tuple
:var BASE_BLACKLIST:
  list files or directories ignored by default by the `export` function

:type IGNORED_EXTENSIONS: tuple
:var IGNORED_EXTENSIONS:
  list file extensions ignored by default by the `export` function

"""

from __future__ import nested_scopes

__revision__ = "$Id: fileutils.py,v 1.4 2005-02-16 16:45:43 fabioz Exp $"
__docformat__ = "restructuredtext en"

import sys
import shutil
import mimetypes
from os.path import isabs, isdir, split, exists, walk, normpath, join
from os import sep, linesep, mkdir, remove, listdir, stat, chmod
from stat import ST_MODE
from cStringIO import StringIO
from warnings import warn


def first_level_directory(path):
    """return the first level directory of a path
    
    >>> first_level_directory('home/syt/work')
    'home'
    >>> first_level_directory('/home/syt/work')
    '/'
    >>> first_level_directory('work')
    'work'
    >>>

    :type path: str
    :param path: the path for which we want the first level directory

    :rtype: str
    :return: the first level directory appearing in `path`
    """
    head, tail = split(path)
    while head and tail:
        head, tail = split(head)
    if tail:
        return tail
    # path was absolute, head is the fs root
    return head


def is_binary(filename):
    """return true if filename may be a binary file, according to it's
    extension

    :type filename: str
    :param filename: the name of the file

    :rtype: bool
    :return:
      true if the file is a binary file (actually if it's mime type
      isn't begining by text/)
    """
    try:
        return not mimetypes.guess_type(filename)[0].startswith('text')
    except AttributeError:
        return 1


def write_open_mode(filename):
    """return the write mode that should used to open file

    :type filename: str
    :param filename: the name of the file

    :rtype: str
    :return: the mode that should be use to open the file ('w' or 'wb') 
    """
    if is_binary(filename):
        return 'wb'
    return 'w'

# backward compat
def get_mode(*args, **kwargs):
    """deprecated, use `files_by_ext` instead"""
    warn('logilab.common.fileutils.get_mode() is deprecated, use '
         'write_open_mode() instead', DeprecationWarning)
    return write_open_mode(*args, **kwargs)

def ensure_fs_mode(filepath, desired_mode):
    """check that the given file has the given mode(s) set, else try to
    set it

    :type filepath: str
    :param filepath: path of the file

    :type desired_mode: int
    :param desired_mode:
      ORed flags describing the desired mode. Use constants from the
      `stat` module for file permission's modes
    """
    mode = stat(filepath)[ST_MODE]
    if not mode & desired_mode:
        chmod(filepath, mode | desired_mode)
        
ensure_mode = ensure_fs_mode # backward compat


class UnresolvableError(Exception):
    """exception raised by relative path when it's unable to compute relative
    path between two paths
    """

def relative_path(from_file, to_file):
    """try to get a relative path from from `from_file` to `to_file`
    (path will be absolute if to_file is an absolute file). This function
    is useful to create link in `from_file` to `to_file`. This typical use
    case is used in this function description.
    
    If both files are relative, they're expected to be relative to the same
    directory.
    
    >>> relative_path( from_file='toto/index.html', to_file='index.html')
    '../index.html'
    >>> relative_path( from_file='index.html', to_file='toto/index.html')
    'toto/index.html'
    >>> relative_path( from_file='tutu/index.html', to_file='toto/index.html')
    '../toto/index.html'
    >>> relative_path( from_file='toto/index.html', to_file='/index.html')
    '/index.html'
    >>> relative_path( from_file='/toto/index.html', to_file='/index.html')
    '../index.html'
    >>> relative_path( from_file='/toto/index.html', to_file='/toto/summary.html')
    'summary.html'
    >>> relative_path( from_file='index.html', to_file='index.html')
    ''
    >>> relative_path( from_file='/index.html', to_file='toto/index.html')
    Traceback (most recent call last):
      File "<string>", line 1, in ?
      File "<stdin>", line 37, in relative_path
    UnresolvableError
    >>> relative_path( from_file='/index.html', to_file='/index.html')
    ''
    >>>

    :type from_file: str
    :param from_file: source file (where links will be inserted)
    
    :type to_file: str
    :param to_file: target file (on which links point)

    :raise UnresolvableError: if it has been unable to guess a correct path
    
    :rtype: str
    :return: the relative path of `to_file` from `from_file`
    """
    from_file = normpath(from_file)
    to_file = normpath(to_file)
    if from_file == to_file:
        return ''
    if isabs(to_file):
        if not isabs(from_file):
            return to_file
    elif isabs(from_file):
        raise UnresolvableError()
    from_parts = from_file.split(sep)
    to_parts = to_file.split(sep)
    idem = 1
    result = []
    while len(from_parts) > 1:
        dirname = from_parts.pop(0)
        if idem and len(to_parts) > 1 and dirname == to_parts[0]:
            to_parts.pop(0)
        else:
            idem = 0
            result.append('..')
    result += to_parts
    return sep.join(result)


from logilab.common.textutils import _LINE_RGX
from sys import version_info
_HAS_UNIV_OPEN = version_info[:2] >= (2, 3)
del version_info

def norm_read(path):
    """return the content of the file with normalized line feeds

    :type path: str
    :param path: path to the file to read

    :rtype: str
    :return: the content of the file with normalized line feeds
    """
    if _HAS_UNIV_OPEN:
        return open(path, 'U').read()
    return _LINE_RGX.sub('\n', open(path).read())

def norm_open(path):
    """return a stream for a file with content with normalized line feeds

    :type path: str
    :param path: path to the file to open

    :rtype: file or StringIO
    :return: the opened file with normalized line feeds
    """
    if _HAS_UNIV_OPEN:
        return open(path, 'U')
    return StringIO(_LINE_RGX.sub('\n', open(path).read()))

      
def lines(path, comments=None):
    """return a list of non empty lines in the file located at `path`

    :type path: str
    :param path: path to the file

    :type comments: str or None
    :param comments:
      optional string which can be used to comment a line in the file
      (ie lines starting with this string won't be returned)

    :rtype: list
    :return:
      a list of stripped line in the file, without empty and commented
      lines

    :warning: at some point this function will probably return an iterator
    """
    stream = norm_open(path)
    result = stream_lines(stream, comments)
    stream.close()
    return result

def stream_lines(stream, comments=None):
    """return a list of non empty lines in the given `stream`

    :type stream: object implementing 'xreadlines' or 'readlines'
    :param stream: file like object

    :type comments: str or None
    :param comments:
      optional string which can be used to comment a line in the file
      (ie lines starting with this string won't be returned)

    :rtype: list
    :return:
      a list of stripped line in the file, without empty and commented
      lines

    :warning: at some point this function will probably return an iterator
    """
    try:
        readlines = stream.xreadlines
    except AttributeError:
        readlines = stream.readlines
    result = []
    for line in readlines():
        line = line.strip()
        if line and (comments is None or not line.startswith(comments)):
            result.append(line)
    return result



BASE_BLACKLIST = ('CVS', '.svn', 'debian', 'dist', 'build', '__buildlog')
IGNORED_EXTENSIONS = ('.pyc', '.pyo', '.elc', '~')

def export(from_dir, to_dir,
           blacklist=BASE_BLACKLIST,
           ignore_ext=IGNORED_EXTENSIONS,
           verbose=0):
    """make a mirror of `from_dir` in `to_dir`, omitting directories and
    files listed in the black list or ending with one of the given
    extensions

    :type from_dir: str
    :param from_dir: directory to export
    
    :type to_dir: str
    :param to_dir: destination directory

    :type blacklist: list or tuple
    :param blacklist:
      list of files or directories to ignore, default to the content of
      `BASE_BLACKLIST`

    :type ignore_ext: list or tuple
    :param ignore_ext:
      list of extensions to ignore, default to  the content of
      `IGNORED_EXTENSIONS`

    :type verbose: bool
    :param verbose:
      flag indicating wether information about exported files should be
      printed to stderr, default to True
    """
    def make_mirror(_, directory, fnames):
        """walk handler"""
        for norecurs in blacklist:
            try:
                fnames.remove(norecurs)
            except ValueError:
                continue
        for filename in fnames:
            # don't include binary files
            for ext in ignore_ext:
                if filename.endswith(ext):
                    break
            else:
                src = join(directory, filename)
                dest = to_dir + src[len(from_dir):]
                if verbose:
                    print >> sys.stderr, src, '->', dest
                if isdir(src):
                    if not exists(dest):
                        mkdir(dest)
                else:
                    if exists(dest):
                        remove(dest)
                    shutil.copy2(src, dest)
    try:
        mkdir(to_dir)
    except OSError:
        pass
    walk(from_dir, make_mirror, None)


def files_by_ext(directory, include_exts=None, exclude_exts=None,
                 exclude_dirs=('CVS', '.svn')):
    """return a list of files in a directory matching (or not) some
    extensions: you should either give the `include_exts` argument (and
    only files ending with one of the listed extensions will be
    considered) or the `exclude_exts` argument (and only files not
    ending by one of the listed extensions will be considered).
    Subdirectories are processed recursivly.

    :type directory: str
    :param directory: directory where files should be searched

    :type include_exts: list or tuple or None
    :param include_exts: list of file extensions to consider
    
    :type exclude_exts: list or tuple or None
    :param exclude_exts: list of file extensions to ignore

    :type exclude_dirs: list or tuple or None
    :param exclude_dirs: list of directory where we should not recurse

    :rtype: list
    :return: the list of files matching input criteria
    """
    assert not (include_exts and exclude_exts)
    if directory in exclude_dirs:
        return []
    if exclude_exts:
        return exclude_files_by_ext(directory, exclude_exts, exclude_dirs)
    return include_files_by_ext(directory, include_exts, exclude_dirs)

# backward compat
def get_by_ext(*args, **kwargs):
    """deprecated, use `files_by_ext` instead"""
    warn('logilab.common.fileutils.get_by_ext() is deprecated, use '
         'files_by_ext() instead', DeprecationWarning)
    return files_by_ext(*args, **kwargs)


def include_files_by_ext(directory, include_exts,
                         exclude_dirs=('CVS', '.svn')):
    """return a list of files in a directory matching some extensions

    :type directory: str
    :param directory: directory where files should be searched

    :type include_exts: list or tuple or None
    :param include_exts: list of file extensions to consider

    :type exclude_dirs: list or tuple or None
    :param exclude_dirs: list of directory where we should not recurse

    :rtype: list
    :return: the list of files matching input criteria
    """
    result = []
    for fname in listdir(directory):
        absfile = join(directory, fname)
        for ext in include_exts:
            if fname.endswith(ext):
                result.append(join(directory, fname))
                break
        else:
            if isdir(absfile):
                if fname in exclude_dirs:
                    continue
                result += include_files_by_ext(join(directory, fname),
                                               include_exts, exclude_dirs)
    return result

def exclude_files_by_ext(directory, exclude_exts,
                         exclude_dirs=('CVS', '.svn')):
    """return a list of files in a directory not matching some extensions

    :type directory: str
    :param directory: directory where files should be searched

    :type exclude_exts: list or tuple or None
    :param exclude_exts: list of file extensions to ignore

    :type exclude_dirs: list or tuple or None
    :param exclude_dirs: list of directory where we should not recurse

    :rtype: list
    :return: the list of files matching input criteria
    """
    result = []
    for fname in listdir(directory):
        absfile = join(directory, fname)
        for ext in exclude_exts:
            if fname.endswith(ext) or fname == 'makefile':
                break
        else:
            if isdir(absfile):
                if fname in exclude_dirs:
                    continue
                result += exclude_files_by_ext(absfile, exclude_exts,
                                               exclude_dirs)
            else:
                result.append(join(directory, fname))
    return result
