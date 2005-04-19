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
""" Copyright (c) 2000-2002 LOGILAB S.A. (Paris, FRANCE).
 http://www.logilab.fr/ -- mailto:contact@logilab.fr
 
Logilab common libraries
"""

__revision__ = "$Id: __init__.py,v 1.5 2005-04-19 14:39:09 fabioz Exp $"

# FIXME: move all those functions in a separated module

def intersection(list1, list2):
    """return the intersection of list1 and list2"""
    intersect_dict, result = {}, []
    for item in list1:
        intersect_dict[item] = 1
    for item in list2:
        if intersect_dict.has_key(item):
            result.append(item)
    return result

def difference(list1, list2):
    """return elements of list1 not in list2"""
    tmp, result = {}, []
    for i in list2:
        tmp[i] = 1
    for i in list1:
        if not tmp.has_key(i):
            result.append(i)
    return result

def union(list1, list2):
    """return list1 union list2"""
    tmp = {}
    for i in list1:
        tmp[i] = 1
    for i in list2:
        tmp[i] = 1
    return tmp.keys()

def make_domains(lists):
    """
    given a list of lists, return a list of domain for each list to produce all
    combinaisons of possibles values

    ex: (['a', 'b'], ['c','d', 'e'])
       -> (['a', 'b', 'a', 'b', 'a', 'b'],
           ['c', 'c', 'd', 'd', 'e', 'e'])
    """
    domains = []
    for iterable in lists:
        new_domain = iterable[:]
        for i in range(len(domains)):
            domains[i] = domains[i]*len(iterable)
        if domains:
            missing = (len(domains[0]) - len(iterable)) / len(iterable)
            i = 0
            for j in range(len(iterable)):
                value = iterable[j]
                for dummy in range(missing):
                    new_domain.insert(i, value)
                    i += 1
                i += 1
        domains.append(new_domain)
    return domains


def flatten(iterable, tr_func=None, results=None):
    """flatten a list of list with any level

    if tr_func is not None, it should be a one argument function that'll be called
    on each final element
    """
    if results is None:
        results = []
    for val in iterable:
        if type(val) in (type(()), type([])):
            flatten(val, tr_func, results)
        elif tr_func is None:
            results.append(val)
        else:
            results.append(tr_func(val))
    return results


def get_cycles(graph_dict, vertices=None):
    '''given a dictionnary representing an ordered graph (i.e. key are vertices
    and values is a list of destination vertices representing edges), return a
    list of detected cycles
    '''
    if not graph_dict:
        return ()
    result = []
    if vertices is None:
        vertices = graph_dict.keys()
    for vertice in vertices:
        _get_cycles(graph_dict, vertice, [], result)
    return result

def _get_cycles(graph_dict, vertice=None, path=None, result=None):
    """recursive function doing the real work for get_cycles"""
    if vertice in path:
        cycle = [vertice]
        for i in range(len(path)-1, 0, -1):
            node = path[i]
            if node == vertice:
                break
            cycle.insert(0, node)
        # make a canonical representation
        start_from = min(cycle)
        index = cycle.index(start_from)
        cycle = cycle[index:] + cycle[0:index]
        # append it to result if not already in
        if not cycle in result:
            result.append(cycle)
        return
    path.append(vertice)
    try:
        for node in graph_dict[vertice]:
            _get_cycles(graph_dict, node, path, result)
    except KeyError:
        pass
    path.pop()


import tempfile
import os
import time
from os.path import exists

class Execute:
    """This is a deadlock save version of popen2 (no stdin), that returns
    an object with errorlevel, out and err
    """
    
    def __init__(self, command):
        outfile = tempfile.mktemp()
        errfile = tempfile.mktemp()
        self.status = os.system("( %s ) >%s 2>%s" %
                                (command, outfile, errfile)) >> 8
        self.out = open(outfile,"r").read()
        self.err = open(errfile,"r").read()
        os.remove(outfile)
        os.remove(errfile)

def acquire_lock(lock_file, max_try=10, delay=10):
    """acquire a lock represented by a file on the file system"""
    count = 0
    while max_try <= 0 or count < max_try:
        if not exists(lock_file):
            break
        count += 1
        time.sleep(delay)
    else:
        raise Exception('Unable to acquire %s' % lock_file)
    stream = open(lock_file, 'w')
    stream.write(str(os.getpid()))
    stream.close()
    
def release_lock(lock_file):
    """release a lock represented by a file on the file system"""
    os.remove(lock_file)
