# Copyright (c) 2000-2003 LOGILAB S.A. (Paris, FRANCE).
# http://www.logilab.fr/ -- mailto:contact@logilab.fr

# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation; either version 2 of the License, or (at your option) any later
# version.

# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

# You should have received a copy of the GNU General Public License along with
# this program; if not, write to the Free Software Foundation, Inc.,
# 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
"""
Some file / file path manipulation utilities
"""

__revision__ = "$Id: fileutils.py,v 1.1 2004-10-26 12:52:29 fabioz Exp $"

from __future__ import nested_scopes
import sys
import re
import shutil
import mimetypes
from os.path import isabs, isdir, split, exists, walk, normpath, join
from os import sep, linesep, mkdir, remove, listdir
from cStringIO import StringIO
from sys import version_info

HAS_UNIV_OPEN = version_info[:2] >= (2, 3)

LINE_RGX = re.compile('\r\n|\r|\n')

def first_level_directory(path):
    """return the first level directory of a path"""
    head, tail = split(path)
    while head and tail:
        head, tail = split(head)
    if tail:
        return tail
    # path was absolute, head is the fs root
    return head

def is_binary(filename):
    """return true if filename may be a binary file, according to it's extension
    """
    return not mimetypes.guess_type(filename)[0].startswith('text')

def get_mode(filename):
    """return the write mode that should used to open file"""
    if is_binary(filename):
        return 'wb'
    return 'w'


class UnresolvableError(Exception):
    """exception raise by relative path when it's unable to compute relative
    path between two paths
    """

def relative_path(from_file, to_file):
    """try to get a relative path from from <from_file> to <to_file>
    (path will be absolute if to_file is an absolute file).
    
    If both files are relative, they're expected to be relative to the same
    directory.
    
    EXAMPLES:
    
    >>> relative_path( from_file='toto/index.html', to_file='index.html')
    '../index.html'

    >>> relative_path( from_file='index.html', to_file='toto/index.html')
    'toto/index.html'

    >>> relative_path( from_file='tutu/index.html', to_file='toto/index.html')
    '../toto/index.html'

    >>> relative_path( from_file='toto/index.html', to_file='/index.html')
    '/index.html'

    >>> relative_path( from_file='/toto/index.html', to_file='/index.html')
    '/index.html'

    >>> relative_path( from_file='index.html', to_file='index.html')
    ''

    >>> relative_path( from_file='/index.html', to_file='toto/index.html')
    Traceback (most recent call last):
      File "<string>", line 1, in ?
      File "<stdin>", line 37, in relative_path
    UnresolvableError
    
    >>> relative_path( from_file='/index.html', to_file='/index.html')
    ''

    """
    from_file = normpath(from_file)
    to_file = normpath(to_file)
    if from_file == to_file:
        return ''
    if isabs(to_file):
        return to_file
    if isabs(from_file):
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
        

def norm_read(path, linesep=linesep):
    """open a file, an normalize line feed"""
    if HAS_UNIV_OPEN:
        return open(path, 'U').read()
    stream = open(path)
    return LINE_RGX.sub(linesep, stream.read())

def norm_open(path, linesep=linesep):
    """open a file in universal mode"""
    if HAS_UNIV_OPEN:
        return open(path, 'U')
    stream = open(path)
    return StringIO(LINE_RGX.sub(linesep, stream.read()))


        
def lines(path, comments=None):
    """return a list of non empty lines in <filename>"""
    stream = norm_open(path)
    result = stream_lines(stream, comments)
    stream.close()
    return result

def stream_lines(stream, comments=None):
    """return a list of non empty lines in <stream>"""
    result = []
    for line in stream.readlines():
        line = line.strip()
        if line and (comments is None or not line.startswith(comments)):
            result.append(line)
    return result



BASE_BLACKLIST = ('CVS', 'debian', 'dist', 'build', '__buildlog')
IGNORED_EXTENSIONS = ('.pyc', '.pyo', '.elc')

def export(from_dir, to_dir,
           blacklist=BASE_BLACKLIST,
           ignore_ext=IGNORED_EXTENSIONS):
    """make a mirror of from_dir in to_dir, omitting directories and files
    listed in the black list
    """
    def make_mirror(arg, directory, fnames):
        """walk handler"""
        for norecurs in blacklist:
            try:
                fnames.remove(norecurs)
            except ValueError:
                continue
        for filename in fnames:
            # don't include binary files
            if filename[-4:] in ignore_ext:
                continue
            if filename[-1] == '~':
                continue
            src = '%s/%s' % (directory, filename)
            dest = to_dir + src[len(from_dir):]
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

def get_by_ext(directory, include_exts=(), exclude_exts=()):
    """return a list of files in a directory matching some extensions 
    """
    assert not (include_exts and exclude_exts)
    result = []
    if exclude_exts:
        for fname in listdir(directory):
            absfile = join(directory, fname)
            for ext in exclude_exts:
                if fname.endswith(ext) or fname == 'makefile':
                    break
            else:
                if isdir(absfile):
                    if fname == 'CVS':
                        continue
                    result += get_by_ext(absfile,
                                         include_exts, exclude_exts)
                else:
                    result.append(join(directory, fname))
    else:
        for fname in listdir(directory):
            absfile = join(directory, fname)
            for ext in include_exts:
                if fname.endswith(ext):
                    result.append(join(directory, fname))
                    break
            else:
                if isdir(absfile) and fname != 'CVS':
                    result += get_by_ext(join(directory, fname),
                                         include_exts, exclude_exts)
    return result
