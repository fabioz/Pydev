# Copyright (c) 2003-2004 Sylvain Thenault (thenault@nerim.net)
# Copyright (c) 2003-2004 Logilab
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
"""astng utilities
"""

from __future__ import generators

__author__ = "Sylvain Thenault"
__revision__ = "$Id: utils.py,v 1.1 2004-10-26 12:52:31 fabioz Exp $"

from logilab.common.astng import NotFoundError


def get_nodes_from_class(node, klass):
    """return an iterator on nodes which are instance of the given class"""
    if isinstance(node, klass):
        yield node
    for child_node in node.getChildNodes():
        for matching in get_nodes_from_class(child_node, klass):
            yield matching

def get_names(node):
    """return the list of accessed names from node"""
    from logilab.common.astng import Name
    return [name.name for name in get_nodes_from_class(node, Name)]


class IgnoreChild(Exception):
    """exception that maybe raised by visit methods to avoid children traversal
    """
    
class ASTWalker:
    """a walker visiting a tree in preorder, calling on the handler:
    
    * visit_<class name> on entering a node, where class name is the class of
    the node in lower case
    
    * leave_<class name> on leaving a node, where class name is the class of
    the node in lower case
    """
    def __init__(self, handler):
        self.handler = handler
        self._cache = {}
        
    def walk(self, node):
        """walk on the tree from <node>, getting callbacks from handler
        """
        try:            
            self.visit(node)
        except IgnoreChild:
            pass
        else:
            for child_node in node.getChildNodes():
                self.walk(child_node)
        self.leave(node)

    def get_callbacks(self, node):
        """get callbacks from handler for the visited node
        """
        klass = node.__class__
        methods = self._cache.get(klass)
        if methods is None:
            handler = self.handler
            kid = klass.__name__.lower()
            e_method = getattr(handler, 'visit_%s' % kid,
                               getattr(handler, 'visit_default', None))
            l_method = getattr(handler, 'leave_%s' % kid, 
                               getattr(handler, 'leave_default', None))
            self._cache[klass] = (e_method, l_method)
        else:
            e_method, l_method = methods
        return e_method, l_method
    
    def visit(self, node):
        """walk on the tree from <node>, getting callbacks from handler"""
        method = self.get_callbacks(node)[0]
        if method is not None:
            method(node)
            
    def leave(self, node):
        """walk on the tree from <node>, getting callbacks from handler"""
        method = self.get_callbacks(node)[1]
        if method is not None:
            method(node)


def is_metaclass(klass):
    """return true if the given class may be considered as a meta-class"""
    if klass.name == 'type':
        return True
    for base in klass.ancestors():
        if base.name == 'type':
            return True
    return False

def is_interface(klass):
    """return true if the given class may be considered as an interface"""
    if klass.name.endswith('Interface'):
        return True
    for base in klass.ancestors():
        if base.name.endswith('Interface'):
            return True
    return False

def is_exception(klass):
    """return true if the given class may be considered as an exception"""
    if klass.name.endswith('Exception') or 'Exception' in klass.basenames:
        return True
    for base in klass.ancestors():
        if base.name.endswith('Exception'):
            return True
    return False

def is_abstract(node, pass_is_abstract=True):
    """return true if the method is abstract, ie raises a NotImplementError
    or contains a single pass (if pass_is_abstract)
    """
    from logilab.common.astng import Raise, Pass, Function
    assert isinstance(node, Function)
    for child_node in node.code.getChildNodes():
        if (isinstance(child_node, Raise) and child_node.expr1 and
              get_names(child_node.expr1)[0] == 'NotImplementedError'):
            return True
        if pass_is_abstract and isinstance(child_node, Pass):
            return True
        return False


def iface_hdlr(klass, iface_node):
    """a handler function used by get_interfaces to handle suspicious
    interface nodes
    """
    try:
        yield klass.resolve(iface_node.as_string())
    except Exception, ex:
        return
    
def get_interfaces(klass, herited=True, manager=None, handler_func=iface_hdlr):
    """return an iterator on interfaces implemented by the given klass node"""
    try:
        implements = klass.locals['__implements__']
    except KeyError:
        if herited:
            try:
                parent = klass.get_ancestor_for_class_attribute('__implements__')
                implements = parent.locals['__implements__']
            except NotFoundError:
                return
    from logilab.common.astng import Class
    if manager is None:
        from logilab.common.astng.manager import ASTNGManager
        manager = ASTNGManager()
    implements = implements.get_assigned_value()
    if hasattr(implements, 'nodes'):
        implements = implements.nodes
    else:
        implements = (implements,)
    #if not (isinstance(implements, tuple) or isinstance(implements, list)):
    #    implements = (implements,)
    for iface in implements:
        if isinstance(iface, Class):
            yield iface
            continue
        # let the handler function take care of this....
        for iface in handler_func(klass, iface):
            yield iface

def get_raises(node):
    """return an iterator on exceptions raised in node"""
    from logilab.common.astng import Raise
    for child in node.getChildNodes():
        if isinstance(child, Raise):
            if child.expr1:
                yield child.expr1
        else:
            for matching in get_raises(child):
                yield matching

def get_returns(klass, herited=True, manager=None):
    """return an iterator on nodes used in return statements"""
    from logilab.common.astng import Return
    for child in klass.getChildNodes():
        if isinstance(child, Return):
            yield child.value
        else:
            for matching in get_returns(child):
                yield matching
