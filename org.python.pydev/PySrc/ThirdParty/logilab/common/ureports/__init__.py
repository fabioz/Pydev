# Copyright (c) 2004 LOGILAB S.A. (Paris, FRANCE).
# http://www.logilab.fr/ -- mailto:contact@logilab.fr
#
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
""" Universal report objects and some formatting drivers

a way to create simple reports using python objects, primarly designed to be
formatted as text and html
"""

from __future__ import generators

__revision__ = "$Id: __init__.py,v 1.3 2005-01-21 17:42:06 fabioz Exp $"

import sys
from os import linesep
from cStringIO import StringIO

class BaseWriter(object):
    """base class for ureport writers"""
    
    def format(self, layout, stream=None):
        """format and write the given layout into the stream object
        """
        if stream is None:
            stream = sys.stdout
        self.__compute_funcs = []
        self.out = stream
        self.begin_format(layout)
        layout.accept(self)
        self.end_format(layout)
        
    def format_children(self, layout):
        """recurse on the layout children and call their accept method
        (see the Visitor pattern)
        """
        for child in getattr(layout, 'children', ()):
            child.accept(self)

    def writeln(self, string=''):
        """write a line in the output buffer"""
        self.out.write(string + linesep)

    def write(self, string):
        """write a string in the output buffer"""
        self.out.write(string)

    def begin_format(self, layout):
        """begin to format a layout"""
        self.section = 0
        
    def end_format(self, layout):
        """finished to format a layout"""

    def get_table_content(self, table):
        """trick to get table content without actually writing it

        return an aligned list of lists containing table cells values as string
        """
        result = [[]]
        cols = table.cols
        for cell in self.compute_content(table):
            if cols == 0:
                result.append([])
                cols = table.cols
            cols -= 1
            result[-1].append(cell)
        # fill missing cells
        while len(result[-1]) < cols:
            result[-1].append('')
        return result

    def compute_content(self, layout):
        """trick to compute the formatting of children layout before actually
        writing it

        return an iterator on strings (one for each child element)
        """
        # use cells !
        def write(data):
            stream.write(data)
        def writeln(data=''):
            stream.write(data+linesep)
        self.write = write
        self.writeln = writeln
        self.__compute_funcs.append((write, writeln))
        for child in layout.children:
            stream = StringIO()
            child.accept(self)
            yield stream.getvalue()
        self.__compute_funcs.pop()
        try:
            self.write, self.writeln = self.__compute_funcs[-1]
        except IndexError:
            del self.write
            del self.writeln

from logilab.common.ureports.nodes import *
from logilab.common.ureports.text_writer import TextWriter
from logilab.common.ureports.html_writer import HTMLWriter
