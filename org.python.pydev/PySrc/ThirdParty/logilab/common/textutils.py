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
"""Some text manipulation utility functions.

:version:   $Revision: 1.6 $  
:author:    Logilab
:copyright: 2003-2005 LOGILAB S.A. (Paris, FRANCE)
:contact:   http://www.logilab.fr/ -- mailto:python-projects@logilab.org

:group text formatting: normalize_text, normalize_paragraph, pretty_match,\
unquote, colorize_ansi
:group text manipulation: searchall, get_csv
:sort: text formatting, text manipulation



:type ANSI_STYLES: dict(str)
:var ANSI_STYLES: dictionary mapping style identifier to ANSI terminal code

:type ANSI_COLORS: dict(str)
:var ANSI_COLORS: dictionary mapping color identifier to ANSI terminal code

:type ANSI_PREFIX: str
:var ANSI_PREFIX:
  ANSI terminal code notifing the start of an ANSI escape sequence
  
:type ANSI_END: str
:var ANSI_END:
  ANSI terminal code notifing the end of an ANSI escape sequence
  
:type ANSI_RESET: str
:var ANSI_RESET:
  ANSI terminal code reseting format defined by a previous ANSI escape sequence
"""

__revision__ = "$Id: textutils.py,v 1.6 2005-04-19 14:39:09 fabioz Exp $"
__docformat__ = "restructuredtext en"

import re
from os import linesep
from warnings import warn


def searchall(rgx, data):
    """apply a regexp using "search" until no more match is found

    This function is deprecated, use re.finditer() instead.
    """
    warn('logilab.common.textutils.searchall() is deprecated, use '
         're.finditer() instead', DeprecationWarning)
    result = []
    match = rgx.search(data)
    while match is not None:
        result.append(match)
        match = rgx.search(data, match.end())        
    return result


def unquote(string):
    """remove optional quotes (simple or double) from the string

    :type string: str or unicode
    :param string: an optionaly quoted string

    :rtype: str or unicode
    :return: the unquoted string (or the input string if it wasn't quoted)
    """
    if not string:
        return string
    if string[0] in '"\'':
        string = string[1:]
    if string[-1] in '"\'':
        string = string[:-1]
    return string


_BLANKLINES_RGX = re.compile('\r?\n\r?\n')
_NORM_SPACES_RGX = re.compile('\s+')

def normalize_text(text, line_len=80, indent=''):
    """normalize a text to display it with a maximum line size and
    optionaly arbitrary indentation. Line jumps are normalized but blank
    lines are kept. The indentation string may be used top insert a
    comment mark for instance.

    :type text: str or unicode
    :param text: the input text to normalize

    :type line_len: int
    :param line_len: expected maximum line's length, default to 80

    :type indent: str or unicode
    :param indent: optional string to use as indentation

    :rtype: str or unicode
    :return:
      the input text normalized to fit on lines with a maximized size
      inferior to `line_len`, and optionally prefixed by an
      indentation string
    """
    result = []
    for text in _BLANKLINES_RGX.split(text):
        result.append(normalize_paragraph(text, line_len, indent))
    return ('%s%s%s' % (linesep, indent, linesep)).join(result)

def normalize_paragraph(text, line_len=80, indent=''):
    """normalize a text to display it with a maximum line size and
    optionaly arbitrary indentation. Line jumps are normalized. The
    indentation string may be used top insert a comment mark for
    instance.


    :type text: str or unicode
    :param text: the input text to normalize

    :type line_len: int
    :param line_len: expected maximum line's length, default to 80

    :type indent: str or unicode
    :param indent: optional string to use as indentation

    :rtype: str or unicode
    :return:
      the input text normalized to fit on lines with a maximized size
      inferior to `line_len`, and optionally prefixed by an
      indentation string
    """
    #text = text.replace(linesep, ' ')
    text = _NORM_SPACES_RGX.sub(' ', text)
    lines = []
    while text:
        text = text.strip()
        pos = min(len(indent) + len(text), line_len)
        if pos == line_len and len(text) > line_len:
            pos = pos - len(indent)
            while pos > 0 and text[pos] != ' ':
                pos -= 1
            if pos == 0:
                pos = min(len(indent) + len(text), line_len)
                pos = pos - len(indent)
                while text[pos] != ' ':
                    pos += 1
        lines.append(indent + text[:pos])
        text = text[pos+1:]
    return linesep.join(lines)


def get_csv(string, sep=','):
    """return a list of string in from a csv formatted line

    >>> get_csv('a, b, c   ,  4')
    ['a', 'b', 'c', '4']
    >>> get_csv('a')
    ['a']
    >>>

    :type string: str or unicode
    :param string: a csv line

    :type sep: str or unicode
    :param sep: field separator, default to the comma (',')

    :rtype: str or unicode
    :return: the unquoted string (or the input string if it wasn't quoted)    
    """
    return [word.strip() for word in string.split(sep) if word.strip()]


_LINE_RGX = re.compile('\r\n|\r+|\n')

def pretty_match(match, string, underline_char='^'):
    """return a string with the match location underlined:

    >>> import re
    >>> print pretty_match(re.search('mange', 'il mange du bacon'), 'il mange du bacon')
    il mange du bacon
       ^^^^^
    >>>
    
    :type match: _sre.SRE_match
    :param match: object returned by re.match, re.search or re.finditer

    :type string: str or unicode
    :param string:
      the string on which the regular expression has been applied to
      obtain the `match` object

    :type underline_char: str or unicode
    :param underline_char:
      character to use to underline the matched section, default to the
      carret '^'

    :rtype: str or unicode
    :return:
      the original string with an inserted line to underline the match
      location
    """
    start = match.start()
    end = match.end()
    string = _LINE_RGX.sub(linesep, string)
    start_line_pos = string.rfind(linesep, 0, start)
    if start_line_pos == -1:
        start_line_pos = 0
        result = []
    else:
        result = [string[:start_line_pos]]
        start_line_pos += len(linesep)
    offset = start - start_line_pos
    underline = ' ' * offset + underline_char * (end - start)
    end_line_pos = string.find(linesep, end)
    if end_line_pos == -1:
        string = string[start_line_pos:]
        result.append(string)
        result.append(underline)
    else:
        end = string[end_line_pos + len(linesep):]
        string = string[start_line_pos:end_line_pos]
        result.append(string)
        result.append(underline)
        result.append(end)
    return linesep.join(result).rstrip()


# Ansi colorization ###########################################################

ANSI_PREFIX = '\033['
ANSI_END = 'm'
ANSI_RESET = '\033[0m'
ANSI_STYLES = {
    'reset'     : "0",
    'bold'      : "1",
    'italic'    : "3",
    'underline' : "4",
    'blink'     : "5",
    'inverse'   : "7",
    'strike'    : "9",
}
ANSI_COLORS = {
    'reset'   : "0",
    'black'   : "30",
    'red'     : "31",
    'green'   : "32",
    'yellow'  : "33",
    'blue'    : "34",
    'magenta' : "35",
    'cyan'    : "36",
    'white'   : "37",
}


def _get_ansi_code(color=None, style=None):
    """return ansi escape code corresponding to color and style
    
    :type color: str or None
    :param color:
      the color identifier (see `ANSI_COLORS` for available values)

    :type style: str or None
    :param style:
      style string (see `ANSI_COLORS` for available values). To get
      several style effects at the same time, use a coma as separator.

    :raise KeyError: if an unexistant color or style identifier is given
    
    :rtype: str
    :return: the built escape code
    """
    ansi_code = []
    if style:
        style_attrs = get_csv(style)
        for effect in style_attrs:
            ansi_code.append(ANSI_STYLES[effect])
    if color:
        ansi_code.append(ANSI_COLORS[color])
    if ansi_code:
        return ANSI_PREFIX + ';'.join(ansi_code) + ANSI_END
    return ''

def colorize_ansi(msg, color=None, style=None):
    """colorize message by wrapping it with ansi escape codes

    :type msg: str or unicode
    :param msg: the message string to colorize

    :type color: str or None
    :param color:
      the color identifier (see `ANSI_COLORS` for available values)

    :type style: str or None
    :param style:
      style string (see `ANSI_COLORS` for available values). To get
      several style effects at the same time, use a coma as separator.

    :raise KeyError: if an unexistant color or style identifier is given

    :rtype: str or unicode
    :return: the ansi escaped string
    """
    # If both color and style are not defined, then leave the text as is
    if color is None and style is None:
        return msg
    escape_code = _get_ansi_code(color, style)
    # If invalid (or unknown) color, don't wrap msg with ansi codes
    if escape_code:
        return '%s%s%s' % (escape_code, msg, ANSI_RESET)
    return msg

