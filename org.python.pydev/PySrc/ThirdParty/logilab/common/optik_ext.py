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
""" Copyright (c) 2002-2003 LOGILAB S.A. (Paris, FRANCE).
 http://www.logilab.fr/ -- mailto:contact@logilab.fr

add an abstraction level to transparently import optik classes from optparse
(python >= 2.3) or the optik package.
It also defines three new types for optik/optparse command line parser :

  * regexp
    argument of this type will be converted using re.compile
  * csv
    argument of this type will be converted using split(',')
  * yn
    argument of this type will be true if 'y' or 'yes', false if 'n' or 'no'
  * named
    argument of this type are in the form <NAME>=<VALUE> or <NAME>:<VALUE>

"""

__revision__ = '$Id: optik_ext.py,v 1.3 2005-01-21 17:42:03 fabioz Exp $'

try:
    # python >= 2.3
    from optparse import OptionParser as BaseParser, Option as BaseOption, \
         OptionGroup, OptionValueError, Values
except Exception, ex:
    # python < 2.3
    from optik import OptionParser as BaseParser, Option as BaseOption, \
         OptionGroup, OptionValueError, Values
    
import re
from copy import copy
import os.path

from logilab.common.textutils import get_csv

def check_regexp(option, opt, value):
    """check a regexp value by trying to compile it
    return the compiled regexp
    """
    try:
        return re.compile(value)
    except ValueError:
        raise OptionValueError(
            "option %s: invalid regexp value: %r" % (opt, value))
    
def check_csv(option, opt, value):
    """check a csv value by trying to split it
    return the list of separated values
    """
    try:
        return get_csv(value)
    except ValueError:
        raise OptionValueError(
            "option %s: invalid regexp value: %r" % (opt, value))

def check_yn(option, opt, value):
    """check a yn value
    return true for yes and false for no
    """
    if value in ('y', 'yes'):
        return 1
    if value in ('n', 'no'):
        return 0
    msg = "option %s: invalid yn value %r, should be in (y, yes, n, no)"
    raise OptionValueError(msg % (opt, value))

def check_named(option, opt, value):
    """check a named value
    return a 2-uple (name, value)
    """
    if value.find('=') != -1:
        return value.split('=', 1)
    if value.find(':') != -1:
        return value.split(':', 1)
    msg = "option %s: invalid named value %r, should be <NAME>=<VALUE> or \
<NAME>:<VALUE>"
    raise OptionValueError(msg % (opt, value))

def check_file(option, opt, value):
    """check a file value
    return the filepath
    """
    if os.path.exists(value):
        return value
    msg = "option %s: file %r does not exist"
    raise OptionValueError(msg % (opt, value))

def check_color(option, opt, value):
    """check a color value and returns it
    /!\ does *not* check color labels (like 'red', 'green'), only
    checks hexadecimal forms
    """
    # Case (1) : color label, we trust the end-user
    if re.match('[a-z0-9 ]+$', value, re.I):
        return value
    # Case (2) : only accepts hexadecimal forms
    if re.match('#[a-f0-9]{6}', value, re.I):
        return value
    # Else : not a color label neither a valid hexadecimal form => error
    msg = "option %s: invalid color : %r, should be either hexadecimal \
    value or predefinied color"
    raise OptionValueError(msg % (opt, value))

import types

class Option(BaseOption):
    """override optik.Option to add some new option types
    """
    TYPES = BaseOption.TYPES + ("regexp", "csv", 'yn', 'named', "multiple_choice", "file", "font", "color")
    TYPE_CHECKER = copy(BaseOption.TYPE_CHECKER)
    TYPE_CHECKER["regexp"] = check_regexp
    TYPE_CHECKER["csv"] = check_csv
    TYPE_CHECKER["yn"] = check_yn
    TYPE_CHECKER["named"] = check_named
    TYPE_CHECKER["multiple_choice"] = check_csv
    TYPE_CHECKER["file"] = check_file
    TYPE_CHECKER["color"] = check_color

    def _check_choice(self):
        """FIXME: need to override this due to optik misdesign"""
        if self.type in ("choice", "multiple_choice"):
            if self.choices is None:
                raise OptionError(
                    "must supply a list of choices for type 'choice'", self)
            elif type(self.choices) not in (types.TupleType, types.ListType):
                raise OptionError(
                    "choices must be a list of strings ('%s' supplied)"
                    % str(type(self.choices)).split("'")[1], self)
        elif self.choices is not None:
            raise OptionError(
                "must not supply choices for type %r" % self.type, self)
    BaseOption.CHECK_METHODS[2] = _check_choice

    
class OptionParser(BaseParser):
    """override optik.OptionParser to use our Option class
    """
    def __init__(self, option_class=Option, *args, **kwargs):
        BaseParser.__init__(self, option_class=Option, *args, **kwargs)
    
__all__ = ('OptionParser', 'Option', 'OptionGroup', 'OptionValueError',
           'Values')
