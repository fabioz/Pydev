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
 
a generic visitor abstract implementation
"""

__revision__ = "$Id: visitor.py,v 1.1 2004-10-26 12:52:29 fabioz Exp $"

def no_filter(object):
    return 1


# Iterators ###################################################################
class FilteredIterator:

    def __init__(self, node, list_func, filter_func=None):
        self._next = [(node, 0)]
        if filter_func is None:
            filter_func = no_filter
        self._list = list_func(node, filter_func)
        
    def next(self):
        try:
            return self._list.pop(0)
        except :
            return None


# Base Visitor ################################################################
class Visitor:

    def __init__(self, iterator_class, filter_func=None):
        self._iter_class = iterator_class
        self.filter = filter_func
        
    def visit(self, node, *args, **kargs):
        """
        launch the visit on a given node

        call 'open_visit' before the begining of the visit, with extra args
        given
        when all nodes have been visited, call the 'close_visit' method
        """
        self.open_visit(node, *args, **kargs)
        return self.close_visit(self._visit(node))

    def _visit(self, node):
        iter = self._get_iterator(node)
        n = iter.next()
        while n:
            result = n.accept(self)
            n = iter.next()  
        return result

    def _get_iterator(self, node):
        return self._iter_class(node, self.filter)
        
    def open_visit(self, *args, **kargs):
        """
        method called at the beginning of the visit
        """
        pass
    
    def close_visit(self, result):
        """
        method called at the end of the visit
        """
        return result



# standard visited mixin ######################################################
class VisitedMixIn:
    """
    Visited interface allow node visitors to use the node
    """
    def get_visit_name(self):
        """
        return the visit name for the mixed class. When calling 'accept', the
        method <'visit_' + name returned by this method> will be called on the
        visitor
        """
        try:
            return self.TYPE.replace('-', '_')
        except:
            return self.__class__.__name__.lower()
    
    def accept(self, visitor, *args, **kwargs):
        func = getattr(visitor, 'visit_%s' % self.get_visit_name())
        return func(self, *args, **kwargs)
    
    def leave(self, visitor, *args, **kwargs):
        func = getattr(visitor, 'leave_%s' % self.get_visit_name())
        return func(self, *args, **kwargs)


