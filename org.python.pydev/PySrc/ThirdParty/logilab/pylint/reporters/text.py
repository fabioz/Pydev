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
""" Copyright (c) 2000-2003 LOGILAB S.A. (Paris, FRANCE).
 http://www.logilab.fr/ -- mailto:contact@logilab.fr

Plain text reporter
"""

__revision__ = "$Id: text.py,v 1.1 2004-10-26 12:52:31 fabioz Exp $"

import sys
import os

from logilab.common.modutils import load_module_from_name
from logilab.common.ureports import TextWriter

from logilab.pylint.interfaces import IReporter
from logilab.pylint.reporters import BaseReporter

TITLE_UNDERLINES = ['', '=', '-', '.']


def modname_to_path(modname, prefix=os.getcwd() + os.sep):
    """transform a module name into a path"""
    module = load_module_from_name(modname).__file__.replace(prefix, '')
    return module.replace('.pyc', '.py').replace('.pyo', '.py')


class TextReporter(BaseReporter):
    """reports messages and layouts in plain text
    """
    
    __implements____ = IReporter
    extension = 'txt'
    
    def __init__(self, output=sys.stdout):
        BaseReporter.__init__(self, output)
        self._modules = {}

    def add_message(self, msg_id, location, msg):
        """manage message of different type and in the context of path"""
        module, obj, line = location
        if not self._modules.has_key(module):
            self.writeln('************* Module %s' % module)
            self._modules[module] = 1
        if obj:
            obj = ':%s' % obj
        if self.include_ids:
            sigle = msg_id
        else:
            sigle = msg_id[0]
        self.writeln('%s:%3s%s: %s' % (sigle, line, obj, msg))

    def _display(self, layout):
        """launch layouts display"""
        print >> self.out 
        TextWriter().format(layout, self.out)


class TextReporter2(TextReporter):
    """a reporter very similar to TextReporter, but display messages in a form
    recognized by most text editors :
    
    <filename>:<linenum>:<msg>
    """

    def add_message(self, msg_id, location, msg):
        """manage message of different type and in the context of path"""
        module, obj, line = location
        if obj:
            obj = ', %s' % obj
        if self.include_ids:
            sigle = msg_id
        else:
            sigle = msg_id[0]
        try:
            modpath = self._modules[module]
        except KeyError:
            modpath = self._modules[module] = modname_to_path(module)
        self.writeln('%s:%s: [%s%s] %s' % (modpath, line, sigle, obj, msg))

    
