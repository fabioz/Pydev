# Copyright (c) 2003-2005 Sylvain Thenault (thenault@nerim.net)
# Copyright (c) 2003-2005 Logilab
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

The builder is not thread safe and can't be used to parse different sources
at the same time.

TODO:
 - more complet representation on inspect build
   (imported modules ? use dis.dis ?)
"""

__author__ = "Sylvain Thenault"
__revision__ = "$Id: builder.py,v 1.5 2005-01-26 18:09:57 fabioz Exp $"

import sys
from compiler import parse
from inspect import isfunction, ismethod, ismethoddescriptor, isclass, \
     isbuiltin, getargspec
from os.path import splitext, basename, dirname, exists

from logilab.common.fileutils import norm_read
from logilab.common.modutils import modpath_from_file
from logilab.common import astng
from logilab.common.astng.raw_building import register_arguments, \
     build_module, build_function, build_class

def _init_module(node):
    """init a module node"""
    node.parent = None
    node.globals = node.locals = {}

# ast NG builder ##############################################################

class ASTNGBuilder:
    """provide astng building methods
    """
    
    def __init__(self, manager):
        self._manager = manager
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
            path_, ext = splitext(module.__file__)
            if ext in ('.py', '.pyc', '.pyo') and exists(path_ + '.py'):
                path = path_ + '.py'
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
        try:
            sys.path.insert(0, dirname(path))
            node = self.string_build(data)
        finally:
            sys.path.pop(0)
        node.file = node.path = path
        if modname is None:
            try:
                modname = '.'.join(modpath_from_file(path))
            except:
                modname = splitext(basename(path))[0]
        node.name = modname 
        node.package = path.find('__init__') > -1
        return node
    
    def string_build(self, data):
        """build astng from a source code stream (i.e. from an ast)"""
        return self.ast_build(parse(data + '\n'))


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
        _init_module(node)

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
        self.visit_default(node)
        node.instance_attrs = {}
        node.basenames = [b_node.as_string() for b_node in node.bases]
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
        self.visit_default(node)
        node.argnames = list(node.argnames)
        if node.name == '__new__':
            node.type = 'classmethod'
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
        self.visit_default(node)
        node.argnames = list(node.argnames)
        node.locals = {}
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
                try:
                    imported = self._manager.astng_from_module_name(node.modname)
                    for name in imported.wildcard_import_names():
                        node.parent.set_local(name, node)
                except:
                    import traceback
                    traceback.print_exc()
                    print >> sys.stderr, \
                          'Unable to get imported names for %r line %s"' % (
                        node.modname, node.lineno)
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
                iattrs = klass.instance_attrs
                # assign if not yet existant in others
                if not iattrs.has_key(node.attrname):
                    iattrs[node.attrname] = node
                # but always assign in __init__, except if previous assigment
                # already come from __init__
                elif frame.name == '__init__' and not \
                         iattrs[node.attrname].get_frame().name == '__init__':
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
        node = build_module(module.__name__, module.__doc__)
        node.object = module
        self._done = {}
        if module.__name__ == 'qt':
            #print 'hanlding qt !'
            #print module.QWidget.__name__, module.QWidget.__module__
            self._qt_member_build(node, module)
        else:
            self._member_build(node, module)
        return node

    def _member_build(self, node, obj):
        """recursive method which create a partial ast from real objects
         (only function, class, and method are handled)
        """
        if self._done.has_key(obj):
            return node
        self._done[obj] = 1
        modname = self._module.__name__
        modfile = getattr(self._module, '__file__', None)
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
                if member.func_code.co_filename != modfile:
                    continue
                self._function_member_build(node, member)
            elif ismethoddescriptor(member):
                self._methoddescriptor_member_build(node, member)
            elif isbuiltin(member):
                # verify this is not an imported member
                if getattr(member, '__module__', None) != modname:
                    continue
                self._builtin_member_build(node, member)                
            elif isclass(member):
                # verify this is not an imported class
                #
                # /!\ some classes like ExtensionClass doesn't have a 
                # __module__ attribute !
                if getattr(member, '__module__', None) != modname:
                    continue
                klass = self._class_member_build(node, member)
                # recursion
                self._member_build(klass, member)

    def _class_member_build(self, node, member):
        """create astng for a living class object"""
        basenames = [base.__name__ for base in member.__bases__]
        klass = build_class(member.__name__, basenames, member.__doc__)
        klass.object = member
        node.add_local_node(klass)
        return klass
    
    def _function_member_build(self, node, member):
        """create astng for a living function object"""
        args, varargs, varkw, defaults = getargspec(member)
        if varargs is not None:
            args.append(varargs)
        if varkw is not None:
            args.append(varkw)
        func = build_function(member.__name__, args, defaults,
                              member.func_code.co_flags, member.__doc__)
        func.object = member
        node.add_local_node(func)
    
    def _methoddescriptor_member_build(self, node, member):
        """create astng for a living method descriptor object"""
        # FIXME get arguments ?
        func = build_function(member.__name__, doc=member.__doc__)
        func.object = member
        node.add_local_node(func)    
    _builtin_member_build = _methoddescriptor_member_build
##     def _builtin_member_build(self, node, member):
##         """create astng for a living method descriptor object"""
##         # FIXME get arguments ?
##         func = build_function(member.__name__, doc=member.__doc__)
##         func.object = member
##         node.add_local_node(func)    
    

    def _qt_member_build(self, node, obj):
        """recursive method which create a partial ast from real objects
         (only function, class, and method are handled)
        """
        if self._done.has_key(obj):
            return node
        self._done[obj] = 1
        # added __main__ to the list to fix problem with qt
        # (qt.QWidget.__module__ == '__main__') !
        # and finally also added modutils since __module__ attribute on qt
        # classes seems to take the name of the importing module
        # FIXME: bug report on pyqt to remove this special handling...
        modname = self._module.__name__
        modules = (modname, '__main__', 'logilab.common.modutils')
        modfile = getattr(self._module, '__file__', None)
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
                if member.func_code.co_filename != modfile:
                    continue
                self._function_member_build(node, member)
            elif ismethoddescriptor(member):
                self._methoddescriptor_member_build(node, member)
            elif isbuiltin(member):
                # verify this is not an imported member
                if not (member.__module__ is None or member.__module__ in modules):
                    print 'skipping', member, member.__module__, modules
                    continue
                self._builtin_member_build(node, member)
            elif isclass(member):
                # verify this is not an imported class
                if not member.__module__ in modules:
                    continue
                # grrr: qt.QWidget.__name__ == 'qt.QWidget' !
                member.__name__ = member.__name__.split('.')[-1]
                klass = self._class_member_build(node, member)
                # recursion
                self._qt_member_build(klass, member)
