# pylint: disable-msg=W0611
#
# Copyright (c) 2002-2004 LOGILAB S.A. (Paris, FRANCE).
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
"""some functions that may be usefull for checkers
"""

__revision__ = '$Id: utils.py,v 1.2 2004-10-26 14:18:35 fabioz Exp $'

from logilab.common import astng
from logilab.common.astng.utils import is_exception, is_interface, \
     is_metaclass, is_abstract, get_nodes_from_class, get_names


def is_error(node):
    """return true if the function does nothing but raising an exception"""
    for child_node in node.code.getChildNodes():
        if isinstance(child_node, astng.Raise):
            return 1
        return 0

def is_empty(node):
    """return true if the given node does nothing but 'pass'"""
    for child_node in node.getChildNodes():
        if isinstance(child_node, astng.Pass):
            return 1
        else:
            return 0

builtins = __builtins__.copy()

def is_native_builtin(name):
    """return true if <name> could be considered as a builtin defined by python
    """
    if builtins.has_key(name):
        return 1
    elif name in ('__path__', '__file__'):
        return 1
    return 0

def is_builtin(name):
    """return true if <name> could be considered as a builtin"""
    if __builtins__.has_key(name):
        return 1
    elif name in ('__builtins__', '__path__', '__file__'):
        return 1
    return 0

def are_exclusive(stmt1, stmt2):
    """return true if the two given statement are mutually exclusive

    algorithm :
     1) index stmt1's parents
     2) climb among stmt2's parents until we find a common parent
     3) if the common parent is a If or TryExcept statement, look if nodes are
        in exclusive branchs
    """
    # index stmt1's parents
    stmt1_parents = {}
    children = {}
    node = stmt1.parent
    previous = stmt1
    while node:
        stmt1_parents[node] = 1
        children[node] = previous
        previous = node
        node = node.parent
    # climb among stmt2's parents until we find a common parent
    node = stmt2.parent
    previous = stmt2
    result = 0
    while node:
        if stmt1_parents.has_key(node):
            # if the common parent is a If or TryExcept statement, look if
            # nodes are in exclusive branchs
            if isinstance(node, astng.If):
                if previous != children[node]:
                    result = 1
            elif isinstance(node, astng.TryExcept):
                stmt1_previous = children[node]
                if previous != stmt1_previous and not (
                    (previous is node.body and stmt1_previous is node.else_) or
                    (stmt1_previous is node.body and previous is node.else_)):
                    result = 1
                
            break
        previous = node
        node = node.parent
    return result
    
