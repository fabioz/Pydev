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

__revision__ = '$Id: utils.py,v 1.1 2004-10-26 12:52:30 fabioz Exp $'

from logilab.common import astng

def get_nodes_from_class(node, klass):
    """return the list of node instance of the given class"""
    if isinstance(node, klass):
        return [node]
    result = []
    for child_node in node.getChildNodes():
        result += get_nodes_from_class(child_node, klass)
    return result

def get_names(node):
    """return the list of accessed names from node"""
    return [name.name for name in get_nodes_from_class(node, astng.Name)]


def is_interface(klass):
    """return true if the given class may be considered as an interface"""
    if klass.__name__.endswith('Interface'):
        return 1
    for base in klass.__bases__:
        if is_interface(base):
            return 1
    return 0

def is_exception(klass):
    """return true if the given class may be considered as an exception"""
    if klass.__name__.endswith('Exception'):
        return 1
    for base in klass.__bases__:
        if is_exception(base):
            return 1
    return 0

def is_metaclass(klass):
    """return true if the given class may be considered as a meta-class"""
    if klass.__name__ == 'type':
        return 1
    for base in klass.__bases__:
        if is_metaclass(base):
            return 1
    return 0


def is_abstract(node, pass_is_abstract=1):
    """return true if the method is abstract, ie raises a NotImplementError
    or contains a single pass (if pass_is_abstract)
    """
    for child_node in node.code.getChildNodes():
        if (isinstance(child_node, astng.Raise) and child_node.expr1 and
              get_names(child_node.expr1)[0] == 'NotImplementedError'):
            return 1
        if pass_is_abstract and isinstance(child_node, astng.Pass):
            return 1
        return 0


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
    
