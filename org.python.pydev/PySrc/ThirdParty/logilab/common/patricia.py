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


a Python implementation of PATRICIA trie

PATRICIA - Practical Algorithm to Retrieve Information Coded in Alphanumeric
           D.R.Morrison (1968).
See http://www.csse.monash.edu.au/~lloyd/tildeAlgDS/Tree/PATRICIA.html if you
want to know what's a PATRICIA tree...

      _ profile code
      _ use mxTextTools ?
"""

__revision__ = "$Id: patricia.py,v 1.3 2004-12-01 16:59:13 fabioz Exp $"

def prefix(prfx, string):
    """return the index of the first character from string which differs from
    prefix
    """
    i = 0
    while i < len(prfx):
        if i == len(string) or prfx[i] != string[i]:
            break
        i += 1
    return i

def split(index, string):
    """split a string on index, returning a 3-uple :
        (string before index, character at index, string after index)
    """
    return string[:index], string[index], string[index+1:]


class PatriciaNode:
    """a PATRICIA trie node
    """
    
    def __init__(self, value='', leaf=0, data=None):
        self.value = value
        self.edges = {}
        if leaf:
            self.datas = [data]
        else:
            self.datas = []
        
    def insert(self, string, data):
        """ insert the string in the trie and associate data to it
        if the string exists is the trie, data is added to the existing datas
        """
        # are we arrived ?
        if self.value == string:
            self.datas.append(data)
        # not yet !
        else:
            # check we don't break compression (value don't match)
            ind = prefix(self.value, string)
            if ind < len(self.value):
                # split this node
                pfx, e, self.value = split(ind, self.value)
                if ind < len(string):
                    n = PatriciaNode(pfx)
                    n.edges[string[ind]] = PatriciaNode(string[ind+1:], 1, data)
                else:
                    n = PatriciaNode(pfx, 1, data)
                n.edges[e] = self
                return n
            n_pfx, n_e, n_sfx = split(len(self.value), string)
            if self.edges.has_key(n_e):
                self.edges[n_e] = self.edges[n_e].insert(n_sfx, data)
            else:
                self.edges[n_e] = PatriciaNode(n_sfx, 1, data)
        return self

    def remove(self, string):
        """ return datas associated with string and remove string from the trie
        raise KeyError if the key isn't found
        FIXME: we should change the trie structure
        """
        if string == self.value and self.datas:
            datas = self.datas
            self.datas = []
            return datas
        else: 
            pfx, e, sfx = split(len(self.value), string)
            if self.value == pfx:
                return self.edges[e].remove(sfx)
        raise KeyError(string)
    
    def lookup(self, string):
        """ return datas associated with string
        raise KeyError if the key isn't found
        """
        if string == self.value:
            if self.datas:
                return self.datas
            raise KeyError(string)
        else: # len(self.value) < len(string): 
            pfx, e, sfx = split(len(self.value), string)
            if self.value == pfx:
                return self.edges[e].lookup(sfx)
        raise KeyError(string)
    
    def pfx_search(self, pfx, depth=-1):
        """ return all string with prefix pfx """
        sfxs = []
        if pfx and self.value[:len(pfx)] != pfx:
            pfx, e, sfx = split(len(self.value), pfx)
            if self.value == pfx and self.edges.has_key(e):
                sfxs = ['%s%s%s' % (self.value, e, sfx)
                        for sfx in self.edges[e].pfx_search(sfx, depth)]
        else:
            if depth != 0:
                for e, child in self.edges.items():
                    search = child.pfx_search('', depth-1-len(self.value))
                    sfxs += ['%s%s%s' % (self.value, e, sfx)
                             for sfx in search]
            if (depth < 0 or len(self.value) <= depth):
                if self.datas:
                    sfxs.append(self.value)
        return sfxs
        
    def __str__(self, indent=''):
        node_str = ''.join([' %s%s:\n%s' % (indent, key,
                                            a.__str__('  %s' % indent))
                            for key, a in self.edges.items()])
        return '%s%s, %s\n%s' % (indent, self.value, self.datas, node_str)

    def __repr__(self):
        return '<PatriciaNode id=%s value=%s childs=%s datas=%s>' % (
            id(self), self.value, self.edges.keys(), self.datas)


class PatriciaTrie:
    """ wrapper class for a patricia tree
    delegates to the root of the tree (PatriciaNode)
    """
    
    def __init__(self):
        self._trie = None
        self.words = 0

    def insert(self, string, data=None):
        """ insert a string into the tree """
        self.words += 1
        if self._trie is None:
            self._trie = PatriciaNode(string, 1, data)
        else:
            self._trie = self._trie.insert(string, data)
            
    def remove(self, string):
        """ remove a string from the tree """
        if self._trie is not None:
            return self._trie.remove(string)
        raise KeyError(string)

    def lookup(self, string):
        """ look for a string into the tree """
        if self._trie is not None:
            return self._trie.lookup(string)
        raise KeyError(string)

    def pfx_search(self, string, depth=-1):
        """ search all words begining by <string> """
        if self._trie is not None:
            return self._trie.pfx_search(string, depth)
        raise KeyError(string)

    def __str__(self):
        return self._trie.__str__()
    
    def __repr__(self):
        return '<PatriciaTrie id=%s words=%s>' % (id(self), self.words)
