# Copyright (c) 2003-2005 Sylvain Thenault (thenault@gmail.com)
# Copyright (c) 2003-2005 Logilab (contact@logilab.fr)
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
"""utilities to create astng trees from scratch
"""

__author__ = "Sylvain Thenault"
__revision__ = "$Id: raw_building.py,v 1.2 2005-02-16 16:45:44 fabioz Exp $"

import sys
from logilab.common.astng import astng

def build_module(name, doc=None):
    """create and initialize a astng Module node"""
    node = astng.Module(doc, astng.Stmt([]))
    node.node.parent = node
    node.object = None
    node.name = name
    node.pure_python = 0
    node.parent = None
    node.globals = node.locals = {}
    return node


if sys.version_info >= (2, 4):
    
    # introduction of decorators has changed the Function initializer arguments
    
    def build_function(name, args=None, defaults=None, flag=0, doc=None):
        """create and initialize a astng Function node"""
        args, defaults = args or [], defaults or []
        # first argument is now a list of decorators
        func = astng.Function([], name, args, defaults, flag, doc, astng.Stmt([]))
        func.code.parent = func
        func.locals = {}
        func.object = None
        if args:
            register_arguments(func, args)
        return func
else:
    
    def build_function(name, args=None, defaults=None, flag=0, doc=None):
        """create and initialize a astng Function node"""
        args, defaults = args or [], defaults or []
        func = astng.Function(name, args, defaults, flag, doc, astng.Stmt([]))
        func.code.parent = func
        func.locals = {}
        func.object = None
        if args:
            register_arguments(func, args)
        return func


def build_class(name, basenames=None, doc=None):
    """create and initialize a astng Class node"""
    klass = astng.Class(name, [], doc, astng.Stmt([]))
    klass.set_parents(basenames or [])
    klass.code.parent = klass
    klass.object = None
    klass.locals = {}
    klass.instance_attrs = {}
    return klass

def build_name_assign(name, value):
    """create and initialize a astng Assign for a name assignment"""
    return astng.Assign([astng.AssName(name, 'OP_ASSIGN')], astng.Const(value))

def build_attr_assign(name, value, attr='self'):
    """create and initialize a astng Assign for an attribute assignment"""
    return astng.Assign([astng.AssAttr(astng.Name(attr), name, 'OP_ASSIGN')],
                        astng.Const(value))

def build_from_import(fromname, names):
    return astng.From(fromname, [(name, None) for name in names])

def register_arguments(node, args):
    """add given arguments to local
    
    args is a list that may contains nested lists
    (i.e. def func(a, (b, c, d)): ...)
    """
    for arg in args:
        if type(arg) is type(''):
            node.set_local(arg, node)
        else:
            register_arguments(node, arg)
