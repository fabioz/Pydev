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
Some text manipulation utilities
"""

__revision__ = "$Id: textutils.py,v 1.1 2004-10-26 12:52:29 fabioz Exp $"

import re
from os import linesep

NORM_SPACES = re.compile('\s+')

def unquote(string):
    """return the unquoted string"""
    if string[0] in '"\'':
        string = string[1:]
    if string[-1] in '"\'':
        string = string[:-1]
    return string


def searchall(rgx, data):
    """apply a regexp using "search" until no more match is found
    """
    result = []
    match = rgx.search(data)
    while match is not None:
        result.append(match)
        match = rgx.search(data, match.end())        
    return result

def normalize_text(text, line_len=80, indent=''):
    """normalize a text to display it with a maximum line size and optionaly
    indentation
    """
    text = text.replace(linesep, ' ')
    text = NORM_SPACES.sub(' ', text)
    lines = []
    while text:
        text = text.strip()
        pos = min(len(indent) + len(text) - 1, line_len)
        if pos == line_len:
            pos = pos - len(indent)
            while text[pos] not in (' ', '\t'):
                pos -= 1
            if pos == 0:
                pos = min(len(indent) + len(text) - 1, line_len)
                pos = pos - len(indent)
                while text[pos] not in (' ', '\t'):
                    pos += 1
        lines.append(indent + text[:pos])
        text = text[pos+1:]
    return linesep.join(lines)

def get_csv(string):
    """return a list of string in a csv format"""
    return [word.strip() for word in string.split(',') if word.strip()]

LINE_RGX = re.compile('\r\n|\r|\n')

def pretty_match(match, string, underline_char='^'):
    """return a string whith the match location underlined
    """
    start = match.start()
    end = match.end()
    string = LINE_RGX.sub(linesep, string)
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


## Ansi colorization #################################################
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


ANSI_PREFIX = '\033['
ANSI_END = 'm'
ANSI_RESET = '\033[0m'


def _get_ansi_code(color, style = None):
    """Returns ansi escape code corresponding to color and style

    :type color: str
    :param color: a string representing the color

    :type style: str
    :param style: a string representing style. Can define several
                  style effect at the same time, (juste use coma
                  as separator)

    :rtype: str
    :return: The built escape code or '' in case of unmatched color / style
    """
    ansi_code = []
    if style is not None:
        style_attrs = [attr.strip() for attr in style.split(',')]
        for effect in style_attrs:
            try:
                ansi_code.append(ANSI_STYLES[effect])
            except KeyError:
                pass
    try:
        ansi_code.append(ANSI_COLORS[color])
    except KeyError:
        pass
    if ansi_code:
        return ANSI_PREFIX + ';'.join(ansi_code) + ANSI_END
    return ''


def colorize_ansi(msg, color, style = None):
    """Colorize message by wrapping it with ansi escape codes"""
    # If both color and style are not defined, then leave the text as is
    if color is None and style is None:
        return msg
    escape_code = _get_ansi_code(color, style)
    # If invalid (or unknown) color, don't wrap msg with ansi codes
    if escape_code:
        return escape_code + msg + ANSI_RESET
    return msg

