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
Some shell utilities, usefull to write some python scripts instead of shell
scripts
"""

__revision__ = '$Id: shellutils.py,v 1.4 2005-02-16 16:45:43 fabioz Exp $'

import os        
import glob
import shutil
from os.path import exists, isdir, basename, join

def mv(source, destination, _action=os.rename):
    """a shell like mv, supporting wildcards
    """
    sources = glob.glob(source)
    if len(sources) > 1:
        assert isdir(destination)
        for filename in sources:
            _action(filename, join(destination, basename(filename)))
    else:
        try:
            source = sources[0]
        except IndexError:
            raise OSError('No file matching %s' % source)
        if exists(destination):
            destination = join(destination, basename(source))
        try:
            _action(source, destination)
        except OSError, ex:
            raise OSError('Unable to move %r to %r (%s)' % (
                source, destination, ex))
        
def rm(*files):
    """a shell like rm, supporting wildcards
    """
    for wfile in files:
        for filename in glob.glob(wfile):
            if isdir(filename):
                shutil.rmtree(filename)
            else:
                os.remove(filename)
    
def cp(source, destination):
    """a shell like cp, supporting wildcards
    """
    mv(source, destination, _action=shutil.copy)
    
