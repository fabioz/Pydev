# Copyright (c) 2004 LOGILAB S.A. (Paris, FRANCE).
# http://www.logilab.fr/ -- mailto:contact@logilab.fr
#
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
"""some wrapper around tools introduced into python 2.3, making them available
in python 2.2
"""

from __future__ import generators

__revision__ = '$Id: compat.py,v 1.3 2005-01-21 17:42:05 fabioz Exp $'

try:
    from sets import Set
except ImportError:
    class Set(dict):
        def __init__(self, values):
            for v in values:
                self.add(v)
        def add(self, value):
            self[value] = 1

try:
    from itertools import izip, chain
except ImportError:
    
    # from itertools documentation ###
    
    def izip(*iterables): 
        iterables = map(iter, iterables)
        while iterables:
            result = [i.next() for i in iterables]
            yield tuple(result)

    def chain(*iterables):
        for it in iterables:
            for element in it:
                yield element
                
try:
    sum = sum
    enumerate = enumerate
except NameError:
    # define the sum and enumerate functions (builtins introduced in py 2.3)
    import operator
    def sum(seq, start=0):
        """Returns the sum of all elements in the sequence"""
        return reduce(operator.add, seq, start)

    def enumerate(iterable):
        """emulates the python2.3 enumerate() function"""
        i = 0
        for val in iterable:
            yield i, val
            i += 1
        #return zip(range(len(iterable)), iterable)
