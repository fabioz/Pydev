# pylint: disable-msg=W0511
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

Check source code is ascii only or has an encoding declaration (PEP 263)
"""

__revision__ = '$Id: misc.py,v 1.1 2004-10-26 12:52:30 fabioz Exp $'

import re

from logilab.pylint.interfaces import IRawChecker
from logilab.pylint.checkers import BaseChecker, CheckerHandler

def is_ascii(string):
    """return true if non ascii characters are detected in the given string
    """
    if string:
        return max([ord(char) for char in string]) < 128
    return False
    

EMACS_ENCODING_RGX = re.compile('[^#]*[#\s]*-\*-\s*coding: ([^\s]*)\s*-\*-\s*')
VIM_ENCODING_RGX = re.compile('[^#]*[#\s]*vim:fileencoding=\s*([^\s]*)\s*')

def guess_encoding(string):
    """try to guess encoding from a python file as string
    return None if not found
    """
    assert type(string) is type(''), type(string)
    # default to ascii on empty string
    if not string:
        return 'ascii'
    
    # check for UTF-8 byte-order mark
    if string.startswith('\xef\xbb\xbf'):
        return 'UTF-8'
    
    first_lines = string.split('\n')[:2]
    for line in first_lines:
        # check for emacs / vim encoding declaration
        match = EMACS_ENCODING_RGX.match(line) or VIM_ENCODING_RGX.match(line)
        if match is not None:
            return match.group(1)

        
MSGS = {
    'E0501': ('Non ascii characters found but no encoding specified (PEP 263)',
              'Used when some non ascii characters are detected but now \
              encoding is specified, as explicited in the PEP 263.'),
    'E0502': ('Wrong encoding specified (%s)',
              'Used when a known encoding is specified but the file doesn\'t \
              seem to be actually in this encoding.'),
    'E0503': ('Unknown encoding specified (%s)',
              'Used when an encoding is specified, but it\'s unknown to Python.'
              ),
    
    'W0511': ('%s',
              'Used when a warning note as FIXME or XXX is detected.'),
    }

class EncodingChecker(BaseChecker, CheckerHandler):
    """checks for:                                                             
    * source code with non ascii characters but no encoding declaration (PEP
      263)                                                                     
    * warning notes in the code like FIXME, XXX
    """
    __implements__ = IRawChecker

    # configuration section name
    name = 'miscellaneous'
    msgs = MSGS

    options = (('notes',
                {'type' : 'csv', 'metavar' : '<comma separated values>',
                 'default' : ('FIXME', 'XXX', 'TODO'),
                 'help' : 'List of note tags to take in consideration, \
separated by a comma. Default to FIXME, XXX, TODO'
                 }),               
               )

    def __init__(self, linter=None):
        BaseChecker.__init__(self, linter)
        CheckerHandler.__init__(self)
    
    def process_module(self, stream):
        """inspect the source file to found encoding problem or fixmes like
        notes
        """
        # source encoding
        data = stream.read()
        if not is_ascii(data):
            encoding = guess_encoding(data)
            if encoding is None:
                self.add_message('E0501', line=1)
            else:
                try:
                    unicode(data, encoding)
                except UnicodeError:
                    self.add_message('E0502', args=encoding, line=1)
                except LookupError:
                    self.add_message('E0503', args=encoding, line=1)
        del data
        # warning notes in the code
        stream.seek(0)
        notes = []
        for note in self.config.notes:
            notes.append(re.compile(note))
        linenum = 1
        for line in stream.readlines():
            for note in notes:
                match = note.search(line)
                if match:
                    self.add_message('W0511', args=line[match.start():-1],
                                     line=linenum)
                    break
            linenum += 1
                    
                
            
def register(linter):
    """required method to auto register this checker"""
    linter.register_checker(EncodingChecker(linter))
