# Copyright (c) 2003 Sylvain Thenault (thenault@nerim.net)
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
"""The ASTNGBuilder makes astng from living object and / or from compiler.ast 

TODO:
 - more complet representation on inspect build
   (imported modules ? use dis.dis ?)
"""

__author__ = "Sylvain Thenault"
__revision__ = "$Id: builder.py,v 1.1 2004-10-26 12:52:31 fabioz Exp $"

import sys
from compiler import parse
from inspect import isfunction, ismethod, ismethoddescriptor, isclass, \
     getargspec
from os.path import splitext, exists

from logilab.common.fileutils import norm_read
from logilab.common.modutils import modpath_from_file
from logilab.common import astng

# ast NG builder ##############################################################

class ASTNGBuilder:
    """provide astng building methods
    """
    
    def __init__(self):
        self._module = None
        self._done = None
        self._stack = None
        self._walker = astng.ASTWalker(self)
        
    def build_from_module(self, module, mod_name=None):
        """build an astng from a living module instance
        """
        node = None
        path = getattr(module, '__file__', None)
        self._module = module
        if path is not None:
            path, ext = splitext(module.__file__)
            if ext in ('.py', '.pyc', '.pyo') and exists(path + '.py'):
                path = path + '.py'
                node = self.file_build(path)
        if node is None:
            # this is a built-in module
            # get a partial representation by introspection
            node = self.inspect_build(module)
            node.file = path
        node.name = mod_name or module.__name__
        node.package = hasattr(module, '__path__')
        return node

    
    def file_build(self, path, modname=None):
        """build astng from a source code file (i.e. from an ast)

        path is expected to be a python source file
        """
        try:
            data = norm_read(path, '\n')
        except IOError, ex:
            msg = 'Unable to load file %r (%s)' % (path, ex)
            raise astng.ASTNGBuildingException(msg)
        # build astng representation
        node = self.string_build(data)
        node.file = path
        node.name = modname or '.'.join(modpath_from_file(path))
        node.package = path.find('__init__') > -1
        return node
    
    def string_build(self, data):
        """build astng from a source code stream (i.e. from an ast)"""
        return self.ast_build(parse(data))


    # astng from ast ##########################################################
    
    def ast_build(self, node):
        """recurse on the ast (soon ng) to add some arguments et method
        """
        node.pure_python = 1
        self._walker.walk(node)
        return node

    def visit_module(self, node):
        """visit a stmt.Module node -> init node and push the corresponding
        object or None on the top of the stack
        """
        self._stack = [self._module]
        self._par_stack = [node]
        node.object = self._module
        init_module(node)

    def leave_module(self, node):
        """leave a stmt.Module node -> pop the last item on the stack and check
        the stack is empty
        """
        self._stack.pop()
        assert not self._stack, 'Stack is not empty : %s' % self._stack
        self._par_stack.pop()
        assert not self._par_stack, \
               'Parent stack is not empty : %s' % self._par_stack
        
    def visit_class(self, node):
        """visit a stmt.Class node -> init node and push the corresponding
        object or None on the top of the stack
        """
        node.instance_attrs = {}
        node.basenames = [b_node.as_string() for b_node in node.bases]
        self.visit_default(node)
        self._push(node)
        
    def leave_class(self, node):
        """leave a stmt.Class node -> pop the last item on the stack
        """
        self.leave_default(node)
        self._stack.pop()
        
    def visit_function(self, node):
        """visit a stmt.Function node -> init node and push the corresponding
        object or None on the top of the stack
        """
        node.argnames = list(node.argnames)
        if node.name == '__new__':
            node.type = 'classmethod'
        else:
            node.type = None
        self.visit_default(node)
        self._push(node)
        obj = node.object
        if hasattr(obj, 'im_func'):
            node.object = obj.im_func
        register_arguments(node, node.argnames)
        #assert not obj or hasattr(node.object, 'func_defaults'), \
        #       '%s %s %s' % (node, node.object.__class__, dir(node.object))
        
    def leave_function(self, node):
        """leave a stmt.Function node -> pop the last item on the stack
        """
        self.leave_default(node)
        self._stack.pop()
        
    def visit_lambda(self, node):
        """visit a stmt.Lambda node -> init node locals
        """
        node.argnames = list(node.argnames)
        node.locals = {}
        self.visit_default(node)
        register_arguments(node, node.argnames)
        
    def visit_global(self, node):
        """visit a stmt.Global node -> add declared names to locals
        """
        self.visit_default(node)
        for name in node.names:
            node.parent.set_local(name, node)
            
    def visit_import(self, node):
        """visit a stmt.Import node -> add imported names to locals
        """
        self.visit_default(node)
        for (name, asname) in node.names:
            name = asname or name
            node.parent.set_local(name.split('.')[0], node)
            
    def visit_from(self, node):
        """visit a stmt.From node -> add imported names to locals
        """
        self.visit_default(node)
        # add names imported by wildcard import
        for (name, asname) in node.names:
            if name == '*':
                imported = {}
                try:
                    exec 'from %s import *' % node.modname in imported
                    for name in imported.keys():
                        node.parent.set_local(name, node)
                except:
                    print >> sys.stderr, \
                          'Unable to exec "from %s import *"' % node.modname
            else:
                node.parent.set_local(asname or name, node)
        
    def visit_assign(self, node):
        """visit a stmt.Assign node -> check for classmethod and staticmethod
        """
        self.visit_default(node)
        klass = node.parent.get_frame()
        if isinstance(klass, astng.Class) and \
            isinstance(node.expr, astng.CallFunc) and \
            isinstance(node.expr.node, astng.Name):
            name = node.expr.node.name
            if name in ('classmethod', 'staticmethod'):
                for ass_node in node.nodes:
                    if isinstance(ass_node, astng.AssName):
                        try:
                            meth = klass.locals[ass_node.name]
                            if isinstance(meth, astng.Function):
                                meth.type = name
                            else:
                                print >> sys.stderr, 'FIXME 1', meth
                        except Exception:
                            print >> sys.stderr, 'FIXME 2', ass_node.name
                            continue
                
            
    def visit_assname(self, node):
        """visit a stmt.AssName node -> add name to locals
        """
        self.visit_default(node)
        if node.flags == 'OP_ASSIGN':
            node.parent.set_local(node.name, node)
            
                
    def visit_assattr(self, node):
        """visit a stmt.AssAttr node -> add name to locals, handle members
        definition
        """
        self.visit_default(node)
        #node.parent.set_local(node.name, node)
        frame = node.get_frame()
        if isinstance(frame, astng.Function) and frame.is_method():
            klass = frame.parent.get_frame()
            # are we assigning to a (new ?) instance attribute ?
            _self = frame.argnames[0]
            if isinstance(node.expr, astng.Name) and node.expr.name == _self:
                # always assign in __init__
                if frame.name == '__init__':
                    klass.instance_attrs[node.attrname] = node
                # but only if not yet existant in others
                elif not klass.instance_attrs.has_key(node.attrname):
                    klass.instance_attrs[node.attrname] = node
                    
    def visit_default(self, node):
        """default visit method, handle the parent attribute
        """
        node.parent = self._par_stack[-1]
        assert node.parent is not node
        self._par_stack.append(node)

    def leave_default(self, node):       
        """default leave method, handle the parent attribute
        """
        self._par_stack.pop()             

    def _push(self, node):
        """update the stack and init some parts of the Function or Class node
        """
        obj = getattr(self._stack[-1], node.name, None)
        self._stack.append(obj)
        node.object = obj
        node.locals = {}
        node.parent.get_frame().set_local(node.name, node)

    # astng from living objects ###############################################
    #
    # this is actually a really minimal representation, including only Module,
    # Function and Class nodes
    
    def inspect_build(self, module):
        """build astng from a living module (i.e. using inspect)
        this is used when there is no python source code available (either
        because it's a built-in module or because the .py is not available)
        """
        node = astng.Module(module.__doc__, astng.Stmt([]))
        node.node.parent = node
        node.object = module
        node.name = module.__name__
        node.pure_python = 0
        init_module(node)
        self._done = {}
        self._member_build(node.node, module)
        return node

    def _member_build(self, node, obj):
        """recursive method which create a partial ast from real objects
         (only function, class, and method are handled)
        """
        if self._done.has_key(obj):
            return node
        self._done[obj] = 1
        for name in dir(obj):
            try:
                member = getattr(obj, name)
            except AttributeError:
                # damned ExtensionClass.Base, I know you're there !
                continue
            if ismethod(member):
                member = member.im_func
            if isfunction(member):
                # verify this is not an imported function
                modfile = getattr(self._module, '__file__', None)
                if member.func_code.co_filename != modfile:
                    continue
                args, varargs, varkw, defaults = getargspec(member)
                if varargs is not None:
                    args.append(varargs)
                if varkw is not None:
                    args.append(varkw)
                func = astng.Function(name, args, defaults or [],
                                      member.func_code.co_flags, member.__doc__,
                                      astng.Stmt([]))
                func.code.parent = func
                func.locals = {}
                register_arguments(func, args)
                func.object = member
                _append(node, func)
            elif ismethoddescriptor(member):
                # FIXME get arguments ?
                func = astng.Function(member.__name__, None, None,
                                      0, member.__doc__,
                                      astng.Stmt([]))
                func.code.parent = func
                func.locals = {}
                func.object = member
                _append(node, func)
            elif isclass(member):
                # verify this is not an imported class
                #
                # /!\ some classes like ExtensionClass doesn't have a 
                # __module__ attribute !
                if getattr(member, '__module__', None) != self._module.__name__:
                    continue
                bases = []
                basenames = []
                for base in member.__bases__:
                    bases.append(astng.Name(base.__name__))
                    basenames.append(base.__name__)
                klass = astng.Class(member.__name__, bases, member.__doc__,
                                    astng.Stmt([]))
                for base in bases:
                    base.parent = klass
                klass.code.parent = klass
                klass.basenames = basenames
                klass.object = member
                klass.locals = {}
                klass.instance_attrs = {}
                _append(node, klass)
                # recursion
                self._member_build(klass, member)


# misc utilities ##############################################################

def init_module(node):
    """init a module node
    """
    node.parent = None
    node.globals = node.locals = {}

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
    
def _append(node, child):
    """append child to the given node, which is expected to be a Stmt (ie a
    Module) or either a Function or a Class
    """
    if hasattr(node, 'nodes'):
        # node is a module (hey, actually a statement)
        node.nodes.append(child)
    else:
        # node is a class
        node.code.nodes.append(child)
    child.parent = node
    node.set_local(child.name, child)


