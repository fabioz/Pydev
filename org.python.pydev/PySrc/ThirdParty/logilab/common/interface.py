# Copyright (c) 2000-2004 LOGILAB S.A. (Paris, FRANCE).
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
"""
 bases class for interfaces

 TODO:
  _ implements a check method which check that an object implements the
    interface
  _ Attribute objects

  This module requires at least python 2.2
"""

__revision__ = "$Id: interface.py,v 1.1 2004-10-26 12:52:29 fabioz Exp $"

from types import ListType, TupleType

class Interface:
    """base class for interfaces"""
    def is_implemented_by(cls, instance):
        return implements(instance, cls)
    is_implemented_by = classmethod(is_implemented_by)
    
def implements(instance, interface):
    """return true if the instance implements the interface
    """
    if hasattr(instance, "__implements__") and \
           (interface is instance.__implements__ or 
            (type(instance.__implements__) in (ListType, TupleType) and \
             interface in instance.__implements__)):
        return 1
    return 0


