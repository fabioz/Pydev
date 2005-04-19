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
""" Copyright (c) 2002-2003 LOGILAB S.A. (Paris, FRANCE).
 http://www.logilab.fr/ -- mailto:contact@logilab.fr
"""

__revision__ = "$Id: html.py,v 1.5 2005-04-19 14:39:09 fabioz Exp $"

# mk html traceback error #####################################################
def html_traceback(info, exception,
                   title='', encoding='ISO-8859-1', body = '') :
    """ return an html formatted traceback from python exception infos.
    """
    import traceback
    from xml.sax.saxutils import escape  
    #typ, value, tbck = info
    stacktb = traceback.extract_tb(info[2]) #tbck)
    strings = []
    if body:
        strings.append('<div class="error_body">')
        strings.append(body)
        strings.append('</div>')
    
    if title:
        strings.append('<h1 class="error">%s</h1>'% escape(title))
    strings.append('<p class="error">%s</p>' % escape(str(exception)))
    strings.append('<div class="error_traceback">')
    for stackentry in stacktb :
        strings.append('<b>File</b> <b class="file">%s</b>, <b>line</b> '
                      '<b class="line">%s</b>, <b>function</b> '
                      '<b class="function">%s</b>:<br/>'%(
            escape(stackentry[0]), stackentry[1], stackentry[2]))
        if stackentry[3]:
            string = escape(stackentry[3]).encode(encoding)
            strings.append('&nbsp;&nbsp;%s<br/>\n' % string)
    strings.append('</div>')
    return '\n'.join(strings)
