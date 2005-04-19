# pylint: disable-msg=W0611
#
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
""" Python Abstract Syntax Tree New Generation

The aim of this module is to provide a common base representation for projects
such as pychecker, pyreverse, pylint...

it's actually the same classes as defined in compiler.ast (from the standard
library) with some more methods and attributes. This module modifies
compiler.ast classes to add some usefull methods. Instance attributes are added
by the ASTNGBuilder module.

New attributes and methods are :

on all nodes :
 .parent, referencing the parent node
 .is_statement(), returning true if the node should be considered as a
  statement node
 .root(), returning the root node of the tree (i.e. a Module)
 .previous_sibling(), returning previous sibling statement node
 .next_sibling(), returning next sibling statement node
 .get_statement(), returning the first parent node marked as statement node
 .get_frame(), returning the first node defining a new local scope (i.e.
  Module, Function or Class)
 .resolve(name), try to resolve the value (astng node) associated to an
  identifier <name>. Raises ResolveError if it has been unable to resolve the
  name.
 .set_local(name, node), define an identifier <name> on the first parent frame,
  with the node defining it. This is used by the astng builder and should not
  be used from out there.
 .as_string(), returning a string representation of the code (should be
  executable).

on Module, Class, Function and Lambda:
 .locals, dictionary of locals with name as key and node defining the local as
  value
 
on Module, Class and Function:
 .object, referencing the living object

on Module :
 .file, the file from which as been extracted the astng representation. It may
  be None if the representation has been built from a built-in module.
 .name, the module name
 .pure_python, boolean for astng built from source (i.e. ast)
 .package, boolean for package module
 .globals, dictionary of globals with name as key and node defining the global
  as value

on Function :
 .type, special attribute for methods with value equals to 'classmethod' or
  'staticmethod' if the method should be  considered as a class method or as
  a static method
 .argnames, list of argument names. MAY BE NONE on some builtin functions where
  arguments are unknown
 .is_method(), return true if the function is actually a method
 .is_class_method(), return true if the function is actually a class method
 .is_static_method(), return true if the function is actually a static method

on Class:
 .instance_attrs, a dictionary of class instances attributes
 .ancestors(), return the list of ancestors nodes in prefixed depth first order
 .get_ancestor_for_method,
 .get_ancestor_for_class_attribute,
 .get_ancestor_for_attribute,
 
on From and Import :
 .get_real_name,
 .get_module_object,
 
"""

from __future__ import generators

__author__ = "Sylvain Thenault"
__revision__ = "$Id: astng.py,v 1.7 2005-04-19 14:39:11 fabioz Exp $"

import __builtin__
from compiler.ast import *
from inspect import isclass

import re
ID_RGX = re.compile('^[a-zA-Z_][a-zA-Z_0-9]*$')
del re

from logilab.common.modutils import get_module_part, load_module_from_name
from logilab.common.astng import ResolveError, NotFoundError, ASTNGManager, \
     ASTNGBuildingException
from logilab.common.compat import chain        


# Node  ######################################################################

def is_statement(node):
    """return true if the node should be considered as statement node
    """
    if isinstance(node.parent, Stmt):
        return node
    return None

Node.is_statement = is_statement

def get_frame(node):
    """return the first node defining a new local scope (i.e. Module,
    Function or Class)
    """
    for klass in (Function, Class, Module):
        if isinstance(node, klass):
            return node
    return node.parent.get_frame()

Node.get_frame = get_frame

def root(node):
    """return the root node of the tree, (i.e. a Module)
    """
    if node.parent:
        return node.parent.root()
    return node

Node.root = root

def get_statement(node):
    """return the first parent node marked as statement node
    """
    if node.is_statement():
        return node
    return node.parent.get_statement()

Node.get_statement = get_statement

def next_sibling(node):
    """return the previous sibling statement 
    """
    while not node.is_statement(): 
        node = node.parent
    index = node.parent.nodes.index(node)
    try:
        return node.parent.nodes[index+1]
    except IndexError:
        return

Node.next_sibling = next_sibling

def previous_sibling(node):
    """return the next sibling statement 
    """
    while not node.is_statement(): 
        node = node.parent
    index = node.parent.nodes.index(node)
    if index > 0:
        return node.parent.nodes[index-1]
    return

Node.previous_sibling = previous_sibling

def source_line(node):
    """return the line number where the given node appears

    we need this method since not all node as the lineno attribute
    correctly set...
    """
    line = node.lineno
    if line is None:
        # FIXME: log node with the missing line no
        _node = node
        try:
            while line is None:
                _node = _node.getChildNodes()[0]
                line = _node.lineno
        except IndexError:
            _node = node.parent
            while _node and line is None:
                line = _node.lineno
                _node = _node.parent
        node.lineno = line
    return line

Node.source_line = source_line

def set_local(node, name, stmt):
    """define <name> in locals (<stmt> is the node defining the name)
    if the node is a Module node (i.e. has globals), add the name to globals

    if the name is already defined, ignore it
    """
    if not hasattr(node, 'locals'):
        node.parent.set_local(name, stmt)
    elif not node.locals.has_key(name):
        node.locals[name] = stmt
            
Node.set_local = set_local

def resolve_dotted(node, name):
    """resolve a dotted names"""
    parts = name.split('.')
    baseobj = node.resolve(parts[0])
    for part in parts[1:]:
        baseobj = baseobj.resolve(part)
    return baseobj

Node.resolve_dotted = resolve_dotted

def resolve_all(node, names):
    """return an iterator the resolved names"""
    for name in names:
        try:
            yield node.resolve_dotted(name)
        except (ResolveError, NotFoundError, AssertionError):
            continue
        
Node.resolve_all = resolve_all

def resolve(node, name):
    """try to resolve the value (astng node) associated to an
    identifier <name>.

    Raises ResolveError if it has been unable to resolve the name.

    FIXME: refactor !!
    """
    #print 'resolve', node.__class__, getattr(node, 'name', '?'), name
    assert ID_RGX.match(name), '%r is not a valid identifier' % name
    frame = node.get_frame()
    try:
        stmt = frame.locals[name]
    except KeyError:
        try:
            stmt = frame.root().globals[name]
        except KeyError:
            if isinstance(frame, Module) and frame.package:
                modname = '%s.%s' % (frame.name, name)
                try:
                    return ASTNGManager().astng_from_module_name(modname)
                except:
                    pass
            try:
                object = getattr(__builtin__, name)
            except AttributeError:
                raise ResolveError(name)
            if name in ('None', 'True', 'False'):
                return Const(eval(name))
            module = getattr(object, '__module__', '__builtin__')
            # the first test avoid infinite loops
            if frame.name.split('.')[-1] == module or not isclass(object):
                raise ResolveError(name)
            # FIXME: may not be a class
            return ASTNGManager().astng_from_class(object, module)
    return stmt.self_resolve(name)
    
Node.resolve = resolve

def self_resolve(node, name):
    """self resolve return the node itself by default"""
    return node
Node.self_resolve = self_resolve

def assname_self_resolve(node, name):
    """self resolve on AssName try to return the assigned value"""
    # FIXME
    assigned = node.get_assigned_value() or node
    # FIXME: check name != assigned.name to avoid infinite loop
    # if this is the case, we probably want to look for that name
    # in globals/builtins, but this is not easlily feasible right now...
    if isinstance(assigned, Name) and name != assigned.name:
        return resolve(node.get_frame(), assigned.name)
    return assigned
AssName.self_resolve = assname_self_resolve

def import_self_resolve(node, name):
    """self resolve on From / Import nodes return the imported module/object"""
    context_file = node.root().file
    try:
        modname, obj = node.get_module_object(name, context_file)
    # FIXME: log error...
    except ImportError:
        raise ResolveError(name)
    except Exception, ex:
        #import traceback
        #traceback.print_exc()
        raise ResolveError(name)
    # check that get_module_object seems to have worked correctly...
    if obj is not None and obj.find('.') > -1:
        raise ResolveError(name)
    # handle special case where we are on a package node importing a module
    # using the same name as the package, which may end in an infinite loop
    # on relative imports
    if modname == getattr(node.root(), 'name', None):
        # FIXME: I don't know what to do here...
        raise ResolveError(name)
    try:
        module = ASTNGManager().astng_from_module_name(modname, context_file)
    except (ASTNGBuildingException, SyntaxError):
        # FIXME
        raise ResolveError(name)

    assert not (module is node and obj == name), \
           str((node.file, node.name, name))
    if obj:
        return module.resolve(obj)
    return module
Import.self_resolve = import_self_resolve
From.self_resolve = import_self_resolve

def global_self_resolve(node, name):
    """self resolve on Global is equivalent to resolve at the module level"""
    return node.root().resolve(name)
Global.self_resolve = global_self_resolve


# module class dict/iterator interface ########################################

def node_getitem(node, item):
    # FIXME: for class sort keys according to line number
    return node.locals[item]
def node_iter(node):
    return iter(node.keys())
def node_keys(node):
    try:
        return node.__keys
    except AttributeError:
        keys = [n.name for n in node.locals.values()
                if (isinstance(n, Function) or isinstance(n, Class))
                    and n.parent.get_frame() is node]
        node.__keys = keys
        return keys 
def node_values(node):
    return [node[key] for key in node.keys()]
def node_items(node):
    return zip(node.keys(), node.values())

def module_values(node):
    return node.locals.values()
def module_items(node):
    return node.locals.items()

Module.__getitem__ = node_getitem
Module.__iter__ = node_iter
Module.keys = node_keys
Module.values = node_values
Module.items = node_items

Class.__getitem__ = node_getitem
Class.__iter__ = node_iter
Class.keys = node_keys
Class.values = node_values
Class.items = node_items

Function.__getitem__ = node_getitem
Function.__iter__ = node_iter
Function.keys = node_keys
Function.values = node_values
Function.items = node_items

def module_append_node(node, child_node):
    """append a child to the given node"""
    node.node.nodes.append(child_node)
    child_node.parent = node
    
Module.append_node = module_append_node

def base_append_node(node, child_node):
    """append a child which should alter locals to the given node"""
    node.code.nodes.append(child_node)
    child_node.parent = node
    
Class.append_node = base_append_node
Function.append_node = base_append_node

def add_local_node(node, child_node):
    """append a child which should alter locals to the given node"""
    node.append_node(child_node)
    node.set_local(child_node.name, child_node)
    
Module.add_local_node = add_local_node
Function.add_local_node = add_local_node
Class.add_local_node = add_local_node

def class_set_parents(node, basenames):
    """set the given name as class parents"""
    bases = [Name(base) for base in basenames]
    for base in bases:
        base.parent = node
    node.basenames = basenames
    node.bases = bases
    
Class.set_parents = class_set_parents

# Module  #####################################################################

def module_get_statement(node):
    """return the first parent node marked as statement node"""
    return node

Module.get_statement = module_get_statement

def module_wildcard_import_names(node):
    """return the list of imported names when this module is 'wildard imported'

    FIXME: should i include '__builtins__' which is added by the actual
           cpython implementation ?
    """
    try:
        explicit = node.locals['__all__'].get_assigned_value()
    except KeyError:
        return [n for n in node.locals.keys() if not n.startswith('_')]
    else:
        # should be a tuple of constant string
        return [const.value for const in explicit.nodes]

Module.wildcard_import_names = module_wildcard_import_names

# Function  ###################################################################

Function.type = None # 'staticmethod' / 'classmethod'

def is_method(node):
    """return true if the function should be considered as a method"""
    return isinstance(node.parent.get_frame(), Class)

Function.is_method = is_method

def is_class_method(node):
    """return true if the function should be considered as a method"""
    return node.type == 'classmethod'

Function.is_class_method = is_class_method

def is_static_method(node):
    """return true if the function should be considered as a method
    """
    return node.type == 'staticmethod'

Function.is_static_method = is_static_method

def format_args(node):
    """return arguments formatted as string"""
    result = []
    args = node.flags & 4
    kwargs = node.flags & 4
    last = len(node.argnames) - 1
    for i in range(len(node.argnames)):
        name = node.argnames[i]
        if type(name) is type(()):
            name = '(%s)' % ','.join(name)
        if len(node.defaults) > i:
            name = '%s=%s' % (name, node.defaults[i].as_string())
        elif i == last and kwargs:
            name = '**%s' % name
        elif args and i == last or (kwargs and i == last - 1):
            name = '*%s' % name
        result.append(name)
    return ', '.join(result)

Function.format_args = format_args
Lambda.format_args = format_args


# Class ######################################################################

def ancestors(node, recurs=True):
    """return an iterator on the node base classes in a prefixed depth first
    order
    """
    # FIXME: mro
    for baseobj in resolve_all(node, node.basenames):
        if not isinstance(baseobj, Class):
            # FIXME
            continue
        yield baseobj
        if recurs:
            for grandpa in ancestors(baseobj, True):
                yield grandpa
        
Class.ancestors = ancestors

def get_ancestor_for_class_attribute(node, name):
    """get the first class which has the <name> atribute and return the astng
    representation of that class.

    Raises NotFoundError if it has been unable to resolve the name.
    """
    for astng in ancestors(node):
        if astng.locals.has_key(name):
            return astng
    raise NotFoundError(name)

Class.get_ancestor_for_method = get_ancestor_for_class_attribute
Class.get_ancestor_for_class_attribute = get_ancestor_for_class_attribute

def get_ancestor_for_attribute(node, attr):
    """get the first class which has a class or instance attribute <attr>
    and return the astng representation of that class.

    Raises NotFoundError if it has been unable to resolve the attribute.
    """
    for astng in ancestors(node):
        if astng.instance_attrs.has_key(attr):
            return astng
#    anode = get_ancestor_for_class_attribute(node, attr)
#    if not isinstance(anode, Function):
#        return anode
    raise NotFoundError(attr)

Class.get_ancestor_for_attribute = get_ancestor_for_attribute

def get_method(node, name):
    """look for a method astng in the given class node or its ancestors
    """
    try:
        return node.resolve_method(name)
    except NotFoundError:
        class_node = node.get_ancestor_for_method(name)
        if class_node is not None:
            # get astng for the searched method
            return class_node.resolve_method(name)
    raise NotFoundError(name)

Class.get_method = get_method

def methods(node, inherited=True):
    """return an iterator on all defined methods in the class (including
    inherited methods by default)
    """
    if inherited:
        done = {}
        for astng in chain(iter((node,)), node.ancestors()):
            for method in astng.locals.values():
                try:
                    methname = method.name
                except AttributeError:
                    # no name attribute, it can't be a function
                    continue
                if not isinstance(method, Function) or done.has_key(methname):
                    continue
                done[methname] = 1
                yield method
    else:
        for member in node.locals.values():
            if not isinstance(member, Function):
                continue
            yield member

Class.methods = methods

def resolve_method(node, name):
    """try to get the method corresponding to name, resolving assigment
    statements
    """
    try:
        stmt = node.locals[name]
    except KeyError:
        raise NotFoundError(name)
    if isinstance(stmt, Function):
        return stmt
    #FIXME
    #if isinstance(stmt, AssName):
    #    return node.resolve_method(stmt.rhs_ast.as_string())
    raise NotFoundError(name)

Class.resolve_method = resolve_method


# From #######################################################################

def get_real_name(node, asname):
    """get name from 'as' name
    """
    for index in range(len(node.names)):
        name, _asname = node.names[index]
        if name == '*':
            return asname
        if not _asname:
            name = name.split('.', 1)[0]
            _asname = name
        if asname == _asname:
            return name
    raise NotFoundError(asname)
    
From.get_real_name = get_real_name

def from_get_module_object(node, asname, context_file=None):
    """given a name which has been introduced by this statement,
    return the name of the module and the name of the object in the module
    
    if the name is a module, object will be None        
    """
    real_name = node.get_real_name(asname)
    name = '%s.%s' % (node.modname, real_name)
    module = get_module_part(name, context_file)
    if module != name:
        obj = name.replace(module + '.', '')
    else:
        obj = None
    return module, obj

From.get_module_object = from_get_module_object


# Import #####################################################################

Import.get_real_name = get_real_name

def import_get_module_object(node, asname, context_file=None):
    """given a name which has been introduced by this statement,
    return the name of the module and the name of the object in the module
    
    if the name is a module, object will be None        
    """
    return node.get_real_name(asname), None

Import.get_module_object = import_get_module_object


# AssName #####################################################################

def get_assigned_value(node):
    """return the node corresponding to the assigned value
    """
    # FIXME AssTuple...
    assnode = node.parent
    while assnode and not isinstance(assnode, Assign):
#        assert assnode is not node.parent, assnode
        assnode = assnode.parent
    return assnode and assnode.expr or None
AssName.get_assigned_value = get_assigned_value
AssAttr.get_assigned_value = get_assigned_value
        
# as_string ###################################################################

def add_as_string(node):
    """return an ast.Add node as string"""
    return '(%s) + (%s)' % (node.left.as_string(), node.right.as_string())
Add.as_string = add_as_string

def and_as_string(node):
    """return an ast.And node as string"""
    return ' and '.join(['(%s)' % n.as_string() for n in node.nodes])
And.as_string = and_as_string
    
def assattr_as_string(node):
    """return an ast.AssAttr node as string"""
    if node.flags == 'OP_DELETE':
        return 'del %s.%s' % (node.expr.as_string(), node.attrname)
    return '%s.%s' % (node.expr.as_string(), node.attrname)
AssAttr.as_string = assattr_as_string

def asslist_as_string(node):
    """return an ast.AssList node as string"""
    string = ', '.join([n.as_string() for n in node.nodes])
    return '[%s]' % string
AssList.as_string = asslist_as_string

def assname_as_string(node):
    """return an ast.AssName node as string"""
    if node.flags == 'OP_DELETE':
        return 'del %s' % node.name
    return node.name
AssName.as_string = assname_as_string
    
def asstuple_as_string(node):
    """return an ast.AssTuple node as string"""
    string = ', '.join([n.as_string() for n in node.nodes])
    # fix for del statement
    return string.replace(', del ', ', ')
AssTuple.as_string = asstuple_as_string

def assert_as_string(node):
    """return an ast.Assert node as string"""
    if node.fail:
        return 'assert %s, %s' % (node.test.as_string(), node.fail.as_string())
    return 'assert %s' % node.test.as_string()
Assert.as_string = assert_as_string

def assign_as_string(node):
    """return an ast.Assign node as string"""
    lhs = ' = '.join([n.as_string() for n in node.nodes])
    return '%s = %s' % (lhs, node.expr.as_string())
Assign.as_string = assign_as_string

def augassign_as_string(node):
    """return an ast.AugAssign node as string"""
    return '%s %s %s' % (node.node.as_string(), node.op, node.expr.as_string())
AugAssign.as_string = augassign_as_string

def backquote_as_string(node):
    """return an ast.Backquote node as string"""
    return '`%s`' % node.expr.as_string()
Backquote.as_string = backquote_as_string

def bitand_as_string(node):
    """return an ast.Bitand node as string"""
    return ' & '.join(['(%s)' % n.as_string() for n in node.nodes])
Bitand.as_string = bitand_as_string

def bitor_as_string(node):
    """return an ast.Bitor node as string"""
    return ' | '.join(['(%s)' % n.as_string() for n in node.nodes])
Bitor.as_string = bitor_as_string

def bitxor_as_string(node):
    """return an ast.Bitxor node as string"""
    return ' ^ '.join(['(%s)' % n.as_string() for n in node.nodes])
Bitxor.as_string = bitxor_as_string

def break_as_string(node):
    """return an ast.Break node as string"""
    return 'break'
Break.as_string = break_as_string

def callfunc_as_string(node):
    """return an ast.CallFunc node as string"""
    expr_str = node.node.as_string()
    args = ', '.join([arg.as_string() for arg in node.args])
    if node.star_args:
        args += ', *%s' % node.star_args.as_string()
    if node.dstar_args:
        args += ', **%s' % node.dstar_args.as_string()
    return '%s(%s)' % (expr_str, args)
CallFunc.as_string = callfunc_as_string

def class_as_string(node):
    """return an ast.Class node as string"""
    bases =  ', '.join([n.as_string() for n in node.bases])
    bases = bases and '(%s)' % bases or ''
    docs = node.doc and '\n    """%s"""' % node.doc or ''
    return 'class %s%s:%s\n    %s\n' % (node.name, bases, docs,
                                      node.code.as_string())
Class.as_string = class_as_string

def compare_as_string(node):
    """return an ast.Compare node as string"""
    return '%s %s' % (node.expr.as_string(), ' '.join(['%s %s' % (op, expr)
                                                       for op, expr in node.ops]))
Compare.as_string = compare_as_string

def const_as_string(node):
    """return an ast.Const node as string"""
    return repr(node.value)
Const.as_string = const_as_string

def continue_as_string(node):
    """return an ast.Continue node as string"""
    return 'continue'
Continue.as_string = continue_as_string

def dict_as_string(node):
    """return an ast.Dict node as string"""
    return '{%s}' % ', '.join(['%s: %s' % (key.as_string(), value.as_string())
                               for key, value in node.items])
Dict.as_string = dict_as_string

def discard_as_string(node):
    """return an ast.Discard node as string"""
    return node.expr.as_string()
Discard.as_string = discard_as_string

def div_as_string(node):
    """return an ast.Div node as string"""
    return '(%s) / (%s)' % (node.left.as_string(), node.right.as_string())
Div.as_string = div_as_string

def ellipsis_as_string(node):
    """return an ast.Ellipsis node as string"""
    return '...'
Ellipsis.as_string = ellipsis_as_string

def exec_as_string(node):
    """return an ast.Exec node as string"""
    if node.globals:
        return 'exec %s in %s, %s' % (node.expr.as_string(),
                                      node.locals.as_string(),
                                      node.globals.as_string())
    if node.locals:
        return 'exec %s in %s' % (node.expr.as_string(), node.locals.as_string())
    return 'exec %s' % node.expr.as_string()
Exec.as_string = exec_as_string

def for_as_string(node):
    """return an ast.For node as string"""
    fors = 'for %s in %s:\n    %s' % (node.assign.as_string(),
                                      node.list.as_string(),
                                      node.body.as_string())
    if node.else_:
        fors = '%s\nelse:\n    %s' % (fors, node.else_.as_string())
    return fors
For.as_string = for_as_string

def from_as_string(node):
    """return an ast.From node as string"""
    return 'from %s import %s' % (node.modname, _import_string(node.names))
From.as_string = from_as_string

def function_as_string(node):
    """return an ast.Function node as string"""
    fargs = node.format_args()
    docs = node.doc and '\n    """%s"""' % node.doc or ''
    return 'def %s(%s):%s\n    %s' % (node.name, fargs, docs,
                                      node.code.as_string())
Function.as_string = function_as_string

def getattr_as_string(node):
    """return an ast.Getattr node as string"""
    return '%s.%s' % (node.expr.as_string(), node.attrname)
Getattr.as_string = getattr_as_string

def global_as_string(node):
    """return an ast.Global node as string"""
    return 'global %s' % ', '.join(node.names)
Global.as_string = global_as_string

def if_as_string(node):
    """return an ast.If node as string"""
    cond, body = node.tests[0]
    ifs = ['if %s:\n    %s' % (cond.as_string(), body.as_string())]
    for cond, body in node.tests[1:]:
        ifs.append('elif %s:\n    %s' % (cond.as_string(), body.as_string()))
    if node.else_:
        ifs.append('else:\n    %s' % node.else_.as_string())
    return '\n'.join(ifs)
If.as_string = if_as_string

def import_as_string(node):
    """return an ast.Import node as string"""
    return 'import %s' % _import_string(node.names)
Import.as_string = import_as_string

def invert_as_string(node):
    """return an ast.Invert node as string"""
    return '~%s' % node.expr.as_string()
Invert.as_string = invert_as_string

def keyword_as_string(node):
    """return an ast.Keyword node as string"""
    return '%s=%s' % (node.name, node.expr.as_string())
Keyword.as_string = keyword_as_string

def lambda_as_string(node):
    """return an ast.Lambda node as string"""
    return 'lambda %s: %s' % (node.format_args(), node.code.as_string())
Lambda.as_string = lambda_as_string

def leftshift_as_string(node):
    """return an ast.LeftShift node as string"""
    return '(%s) << (%s)' % (node.left.as_string(), node.right.as_string())
LeftShift.as_string = leftshift_as_string

def list_as_string(node):
    """return an ast.List node as string"""
    return '[%s]' % ', '.join([child.as_string() for child in node.nodes])
List.as_string = list_as_string

def listcomp_as_string(node):
    """return an ast.ListComp node as string"""
    return '[%s %s]' % (node.expr.as_string(), ' '.join([n.as_string()
                                                         for n in node.quals]))
ListComp.as_string = listcomp_as_string

def listcompfor_as_string(node):
    """return an ast.ListCompFor node as string"""
    return 'for %s in %s %s' % (node.assign.as_string(),
                                node.list.as_string(),
                                ' '.join([n.as_string() for n in node.ifs]))
ListCompFor.as_string = listcompfor_as_string

def listcompif_as_string(node):
    """return an ast.ListCompIf node as string"""
    return 'if %s' % node.test.as_string()
ListCompIf.as_string = listcompif_as_string

def mod_as_string(node):
    """return an ast.Mod node as string"""
    return '(%s) %% (%s)' % (node.left.as_string(), node.right.as_string())
Mod.as_string = mod_as_string

def module_as_string(node):
    """return an ast.Module node as string"""
    docs = node.doc and '"""%s"""\n' % node.doc or ''
    return '%s%s' % (docs, node.node.as_string())
Module.as_string = module_as_string

def mul_as_string(node):
    """return an ast.Mul node as string"""
    return '(%s) * (%s)' % (node.left.as_string(), node.right.as_string())
Mul.as_string = mul_as_string

def name_as_string(node):
    """return an ast.Name node as string"""
    return node.name
Name.as_string = name_as_string

def not_as_string(node):
    """return an ast.Not node as string"""
    return 'not %s' % node.expr.as_string()
Not.as_string = not_as_string

def or_as_string(node):
    """return an ast.Or node as string"""
    return ' or '.join(['(%s)' % n.as_string() for n in node.nodes])
Or.as_string = or_as_string

def pass_as_string(node):
    """return an ast.Pass node as string"""
    return 'pass'
Pass.as_string = pass_as_string

def power_as_string(node):
    """return an ast.Power node as string"""
    return '(%s) ** (%s)' % (node.left.as_string(), node.right.as_string())
Power.as_string = power_as_string

def print_as_string(node):
    """return an ast.Print node as string"""
    nodes = ', '.join([n.as_string() for n in node.nodes])
    if node.dest:
        return 'print >> %s, %s,' % (node.dest.as_string(), nodes)
    return 'print %s,' % nodes
Print.as_string = print_as_string

def printnl_as_string(node):
    """return an ast.Printnl node as string"""
    nodes = ', '.join([n.as_string() for n in node.nodes])
    if node.dest:
        return 'print >> %s, %s' % (node.dest.as_string(), nodes)
    return 'print %s' % nodes
Printnl.as_string = printnl_as_string

def raise_as_string(node):
    """return an ast.Raise node as string"""
    if node.expr1:
        if node.expr2:
            if node.expr3:
                return 'raise %s, %s, %s' % (node.expr1.as_string(),
                                             node.expr2.as_string(),
                                             node.expr3.as_string())
            return 'raise %s, %s' % (node.expr1.as_string(),
                                     node.expr2.as_string())
        return 'raise %s' % node.expr1.as_string()
    return 'raise'
Raise.as_string = raise_as_string

def return_as_string(node):
    """return an ast.Return node as string"""
    return 'return %s' % node.value.as_string()
Return.as_string = return_as_string

def rightshift_as_string(node):
    """return an ast.RightShift node as string"""
    return '(%s) >> (%s)' % (node.left.as_string(), node.right.as_string())
RightShift.as_string = rightshift_as_string

def slice_as_string(node):
    """return an ast.Slice node as string"""
    # FIXME: use flags
    lower = node.lower and node.lower.as_string() or ''
    upper = node.upper and node.upper.as_string() or ''
    return '%s[%s:%s]' % (node.expr.as_string(), lower, upper)
Slice.as_string = slice_as_string

def sliceobj_as_string(node):
    """return an ast.Sliceobj node as string"""
    return ':'.join([n.as_string() for n in node.nodes])
Sliceobj.as_string = sliceobj_as_string

def stmt_as_string(node):
    """return an ast.Stmt node as string"""
    stmts = '\n'.join([n.as_string() for n in node.nodes])
    if isinstance(node.parent, Module):
        return stmts
    return stmts.replace('\n', '\n    ')
Stmt.as_string = stmt_as_string

def sub_as_string(node):
    """return an ast.Sub node as string"""
    return '(%s) - (%s)' % (node.left.as_string(), node.right.as_string())
Sub.as_string = sub_as_string

def subscript_as_string(node):
    """return an ast.Subscript node as string"""
    # FIXME: flags ?
    return '%s[%s]' % (node.expr.as_string(), ','.join([n.as_string()
                                                        for n in node.subs]))
Subscript.as_string = subscript_as_string

def tryexcept_as_string(node):
    """return an ast.TryExcept node as string"""
    trys = ['try:\n    %s' % node.body.as_string()]
    for exc_type, exc_obj, body in node.handlers:
        if exc_type:
            if exc_obj:
                excs = 'except %s, %s' % (exc_type.as_string(),
                                          exc_obj.as_string())
            else:
                excs = 'except %s' % exc_type.as_string()
        else:
            excs = 'except'
        trys.append('%s:\n    %s' % (excs, body.as_string()))
    return '\n'.join(trys)
TryExcept.as_string = tryexcept_as_string

def tryfinally_as_string(node):
    """return an ast.TryFinally node as string"""
    return 'try:\n    %s\nfinally:\n    %s' % (node.body.as_string(),
                                               node.final.as_string())
TryFinally.as_string = tryfinally_as_string

def tuple_as_string(node):
    """return an ast.Tuple node as string"""
    return '(%s)' % ', '.join([child.as_string() for child in node.nodes])
Tuple.as_string = tuple_as_string

def unaryadd_as_string(node):
    """return an ast.UnaryAdd node as string"""
    return '+%s' % node.expr.as_string()
UnaryAdd.as_string = unaryadd_as_string

def unarysub_as_string(node):
    """return an ast.UnarySub node as string"""
    return '-%s' % node.expr.as_string()
UnarySub.as_string = unarysub_as_string

def while_as_string(node):
    """return an ast.While node as string"""
    whiles = 'while %s:\n    %s' % (node.test.as_string(),
                                    node.body.as_string())
    if node.else_:
        whiles = '%s\nelse:\n    %s' % (whiles, node.else_.as_string())
    return whiles
While.as_string = while_as_string

def yield_as_string(node):
    """yield an ast.Yield node as string"""
    return 'yield %s' % node.value.as_string()
Yield.as_string = yield_as_string

    
# private utilities ###########################################################

def _import_string(names):
    """return a list of (name, asname) formatted as a string
    """
    _names = []
    for name, asname in names:
        if asname is not None:
            _names.append('%s as %s' % (name, asname))
        else:
            _names.append(name)
    return  ', '.join(_names)
