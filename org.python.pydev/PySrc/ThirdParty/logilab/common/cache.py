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

 Cache module, with a least recently used algorithm for the management of the
 deletion of entries.
"""

__revision__ = '$Id: cache.py,v 1.2 2004-10-26 14:18:34 fabioz Exp $'


class Cache:
    """ a dictionnary like cache

    inv:
        len(self._usage) <= self.size
        len(self.data) <= self.size
    """
    
    def __init__(self, size=100):
        self.data = {}
        self.size = size
        self._usage = []

    def __repr__(self):
        return repr(self.data)

    def __len__(self):
        return len(self.data)

    def _update_usage(self, key):
        # Special case : cache's size = 0 !
        if self.size <= 0:
            return
        
        if not self._usage:
            self._usage.append(key)
        
        if self._usage[-1] != key:
            try:
                self._usage.remove(key)
            except ValueError:
                # we are inserting a new key
                # check the size of the dictionnary
                # and remove the oldest item in the cache
                if self.size and len(self._usage) >= self.size:
                    del self.data[self._usage[0]]
                    del self._usage[0]
            self._usage.append(key)

            
    def __getitem__(self, key):
        value = self.data[key]
        self._update_usage(key)
        return value
    
    def __setitem__(self, key, item):
        # Just make sure that size > 0 before inserting a new item in the cache
        if self.size > 0:
            self.data[key] = item
        self._update_usage(key)
        
    def __delitem__(self, key):
        # If size <= 0, then we don't have anything to do
        # XXX FIXME : Should we let the 'del' raise a KeyError ?
        if self.size > 0:
            del self.data[key]
            self._usage.remove(key)
        
    def clear(self):
        self.data.clear()
        self._usage = []

    def keys(self):
        return self.data.keys()

    def items(self):
        return self.data.items()

    def values(self):
        return self.data.values()

    def has_key(self, key):
        return self.data.has_key(key)

    
