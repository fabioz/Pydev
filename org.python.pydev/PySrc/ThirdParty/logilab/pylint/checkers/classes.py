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
""" Copyright (c) 2002-2005 LOGILAB S.A. (Paris, FRANCE).
 http://www.logilab.fr/ -- mailto:contact@logilab.fr

 basic checker for Python code
"""
from __future__ import generators

__revision__ = "$Id: classes.py,v 1.4 2005-01-31 17:23:22 fabioz Exp $"

from logilab.common import astng

from logilab.pylint.interfaces import IASTNGChecker
from logilab.pylint.checkers import BaseChecker
from logilab.pylint.checkers.utils import is_abstract, is_interface, \
     is_exception, is_metaclass, get_nodes_from_class


MSGS = {
    'F0201': ('Unable to check method %r of interface %s',
              'Used when PyLint has been unable to fetch a method declared in \
              an interface (either in the class or in the interface) and so to\
              check its implementation.'),
    'F0202': ('Unable to check methods signature (%s / %s)',
              'Used when PyLint has been unable to check methods signature \
              compatibility for an unexpected raison. Please report this kind \
              if you don\'t make sense of it.'),
    'F0203': ('Unable to resolve %s',
              'Used when PyLint has been unable to resolve a name.'),

    'E0201': ('Access to undefined member %r',
              'Used when an instance member not defined in the instance, its\
              class or its ancestors is accessed.'),
    'E0202': ('Method hide an inherited attribute from %s',
              'Used when a class defines a method which hide an attribute from \
              an ancestor class.'),
    'E0203': ('Access to member %r before its definition line %s',
              'Used when an instance member is accessed before it\'s actually\
              assigned.'),
    'W0201': ('Attribute %r defined outside __init__',
              'Used when an instance attribute is defined outside the __init__\
              method.'),
    
    'E0211': ('Method has no argument',
              'Used when a method which should have the bound instance as \
              first argument has no argument defined.'),
    'E0212': ('Class method should have "cls" as first argument',
              'Used when a class method has an attribute different than "cls"\
              as first argument, to easily differentiate them from regular \
              instance methods.'),
    'E0213': ('Method doesn\'t have "self" as first argument',
              'Used when a method has an attribute different the "self" as\
              first argument.'),
    'E0214': ('Metaclass method doesn\'t have "mcs" as first argument',
              'Used when a metaclass method has an attribute different the \
              "mcs" as first argument.'),
    'W0211': ('Static method with %r as first argument',
              'Used when a static method has "self" or "cls" as first argument.'
              ),
    
    'E0221': ('Interface %s is not a class (%s)',
              'Used when a class claims to implement an interface which is not \
              a class.'),
    'E0222': ('Missing method %r from %s interface' ,
              'Used when a method declared in an interface is missing from a \
              class implementing this interface'),
    'W0221': ('Arguments number differs from %s method',
              'Used when a method has a different number of arguments than in \
              the implemented interface or in an overriden method.'),
    'W0222': ('Signature differs from %s method',
              'Used when a method signature is different than in the \
              implemented interface or in an overriden method.'),
    'W0223': ('Method %r is abstract in class %r but is not overriden',
              'Used when an abstract method (ie raise NotImplementedError) is \
              not overriden in concrete class.'
              ),
    
    'W0231': ('__init__ method from base class %r is not called',
              'Used when an ancestor class method has an __init__ method \
              which is not called by a derived class.'),
    'W0232': ('Class has no __init__ method',
              'Used when a class has no __init__ method, neither its parent \
              classes.'),
    'W0233': ('__init__ method from a non direct base class %r is called',
              'Used when an __init__ method is called on a class which is not \
              in the direct ancestors for the analysed class.'),
    
    }


class ClassChecker(BaseChecker):
    """checks for :                                                            
    * methods without self as first argument                                   
    * overriden methods signature                                              
    * access only to existant members via self                                 
    * attributes not defined in the __init__ method                            
    * supported interfaces implementation                                      
    * unreachable code                                                         
    """
    
    __implements__ = (IASTNGChecker,)

    # configuration section name
    name = 'classes'
    # messages
    msgs = MSGS
    priority = -2
    # configuration options
    options = (('ignore-iface-methods',
                {'default' : (#zope interface
        'isImplementedBy', 'deferred', 'extends', 'names',
        'namesAndDescriptions', 'queryDescriptionFor',  'getBases',
        'getDescriptionFor', 'getDoc', 'getName', 'getTaggedValue',
        'getTaggedValueTags', 'isEqualOrExtendedBy', 'setTaggedValue',
        'isImplementedByInstancesOf',
        # twisted
        'adaptWith',
        # logilab.common interface
        'is_implemented_by'),
                 'type' : 'csv',
                 'metavar' : '<method names>',
                 'help' : 'List of interface methods to ignore, \
separated by a comma. This is used for instance to not check methods defines \
in Zope\'s Interface base class.'}
                ),
               ('ignore-mixin-members',
                {'default' : 1, 'type' : 'yn', 'metavar': '<y_or_n>',
                 'help' : 'Tells wether missing members accessed in mixin \
class should be ignored. A mixin class is detected if its name ends with \
"mixin" (case insensitive).'}
                ),
               )

    def __init__(self, linter=None):
        BaseChecker.__init__(self, linter)
        self._accessed = []
        self._first_attrs = []
        
    def visit_class(self, node):
        """init visit variable _accessed and check interfaces
        """        
        self._accessed.append({})
        node.metaclass = is_metaclass(node)
        self._check_bases_classes(node)
        self._check_interfaces(node)
        if not (is_interface(node) or is_exception(node) or node.metaclass):
            try:
                node.get_method('__init__')
            except astng.NotFoundError:
                self.add_message('W0232', args=node, node=node)
            
    def leave_class(self, class_node):
        """close a class node :
        check that instance attributes are defined in __init__ and check
        access to existant members
        """
        # checks attributes are defined in __init__
        for attr, node in class_node.instance_attrs.items():
            frame = node.get_frame()
            if frame.name not in ('__init__', '__new__', 'setUp'):
                try:
                    par_node = class_node.get_ancestor_for_attribute(attr)
                    frame = par_node.instance_attrs[attr].get_frame()
                    if frame.name not in ('__init__', '__new__',
                                          'setUp'):
                        self.add_message('W0201', args=attr, node=node)
                except astng.NotFoundError:
                    self.add_message('W0201', args=attr, node=node)
        # check access to existant members
        accessed = self._accessed.pop()
        if not self.config.ignore_mixin_members or \
               class_node.name[-5:].lower() != 'mixin':
            self._check_accessed_members(class_node, accessed)
            
    def visit_function(self, node):
        """check method arguments, overriding"""
        # check first argument is self if this is actually a method
        if node.is_method():
            klass = node.parent.get_frame()
            self._check_first_arg_for_type(node, klass.metaclass)
            if node.name == '__init__':
                self._check_init(node)
                return
            # check signature if the method overrload an herited method
            try:
                overriden = klass.get_ancestor_for_method(node.name)
            except (astng.NotFoundError, astng.ASTNGBuildingException):
                pass
            else:
                # get astng for the searched method
                try:
                    meth_node = overriden.locals[node.name]
                    self._check_signature(node, meth_node, 'overriden')
                except KeyError:
                    # we have found the method but it's not in the local
                    # dictionnary.
                    # This may happen with astng build from living objects only
                    pass
            # check if the method overload an attribute
            try:
                overriden = klass.get_ancestor_for_attribute(node.name)
                self.add_message('E0202', args=overriden.name, node=node)
            except astng.NotFoundError:
                pass
                
    def leave_function(self, node):
        """check method arguments, overriding"""
        # check first argument is self if this is actually a method
        if node.is_method() and node.argnames is not None:
            self._first_attrs.pop()

    def visit_getattr(self, node):
        """check if the name handle an access to a class member
        if so, register it
        """
        if self._first_attrs and isinstance(node.expr, astng.Name):
            if node.expr.name == self._first_attrs[-1]:
                self._accessed[-1].setdefault(node.attrname, []).append(node)

                
    def _check_accessed_members(self, node, accessed):
        """check that accessed members are defined"""
        for attr, nodes in accessed.items():
            # is it a builtin attribute
            if attr in ('__dict__', '__class__', '__doc__'):
                continue
            # is it an instance attribute ?
            if node.instance_attrs.has_key(attr):
                frame = node.instance_attrs[attr].get_frame()
                lineno = node.instance_attrs[attr].source_line()
                # check that if the node is accessed in the same method as
                # it's defined, it's accessed after the initial assigment
                for _node in nodes:
                    if _node.get_frame() is frame and _node.lineno < lineno:
                        self.add_message('E0203', node=_node,
                                         args=(attr, lineno))
                continue
            # or a class attribute ?
            if node.locals.has_key(attr):
                continue
            # or an inherited method / attribute ?
            try:
                node.get_ancestor_for_method(attr)
            except astng.NotFoundError:
                pass
            else:
                continue
            try:
                node.get_ancestor_for_attribute(attr)
            except astng.NotFoundError:
                pass
            else:
                continue
            for _node in nodes:
                self.add_message('E0201', node=_node, args=attr)
        
    def _check_first_arg_for_type(self, node, metaclass=0):
        """check the name of first argument, expect:
        
        * 'self' for a regular method
        * 'cls' for a class method
        * 'mcs' for a metaclass
        * not one of the above for a static method
        """
        # don't care about functions with unknown argument (builtins)
        if node.argnames is None:
            return
        self._first_attrs.append(node.argnames and node.argnames[0])
        # metaclass method
        if metaclass:
            if self._first_attrs[-1] != 'mcs':
                self.add_message('E0214', node=node)
        # static method
        elif node.is_static_method():
            if node.argnames and node.argnames[0] in ('self', 'cls', 'mcs'):
                self.add_message('W0211', args=node.argnames[0], node=node)
            self._first_attrs[-1] = None
        # class / regular method with no args
        elif not node.argnames:
            self.add_message('E0211', node=node)
        # class method
        elif node.is_class_method():
            if self._first_attrs[-1] != 'cls':
                self.add_message('E0212', node=node)
        # regular method without self as argument
        elif self._first_attrs[-1] != 'self':
            self.add_message('E0213', node=node)

    def _check_bases_classes(self, node):
        """check that the given class node implements abstract methods from
        base classes
        """
        for method in node.methods():
            owner = method.parent.get_frame()
            if owner is node:
                continue
            # check that the ancestor's method is not abstract
            if is_abstract(method, pass_is_abstract=0):
                self.add_message('W0223', node=node,
                                 args=(method.name, owner.name))
                    
    def _check_interfaces(self, node):
        """check that the given class node really implements declared
        interfaces
        """
        def iface_handler(klass, iface_node):
            """filter interface objects, it should be classes"""
            try:
                obj = klass.resolve_dotted(iface_node.as_string())
                if not isinstance(obj, astng.Class):
                    self.add_message('E0221', node=node,
                                     args=(iface_node.as_string(),
                                           obj.__class__.__name__))
                else:
                    yield obj
            except:
                self.add_message('F0203', node=node,
                                 args=(iface_node.as_string()))
                
        implements = astng.utils.get_interfaces(node, handler_func=iface_handler)
        for iface in implements:
            for imethod in iface.methods():
                name = imethod.name
                if name.startswith('_') or \
                       name in self.config.ignore_iface_methods:
                    # don't check method begining with an underscore, usually
                    # belonging to the interface implementation
                    continue
                # get class method astng
                try:
                    method = self._get_method(node, name)
                except astng.NotFoundError:
                    self.add_message('E0222', args=(name, iface.name),
                                     node=node)
                    continue
                if method is None:
                    continue
                # don't go further if it is an inherited method
                if not method.parent.get_frame() is node:
                    continue
                # check signature
                self._check_signature(method, imethod,
                                     '%s interface' % iface.name)

    def _check_init(self, node):
        """check that the __init__ method call super or ancestors'__init__
        method
        """
        klass_node = node.parent.get_frame()        
        to_call, unresolved = self._ancestors_to_call(node, klass_node)
        for stmt in get_nodes_from_class(node, astng.CallFunc):
            expr = stmt.node
            if not (isinstance(expr, astng.Name) or
                    isinstance(expr, astng.Getattr)):
                continue
            func_str = expr.as_string()
            if func_str.endswith('.__init__'):
                klass_name = '.'.join(func_str.split('.')[:-1])
                try:
                    del to_call[klass_name]
                except KeyError:
                    if unresolved.has_key(klass_name) \
                           or klass_name in klass_node.basenames:
                        continue
                    if klass_name.startswith('super('):
                        return
                    self.add_message('W0233', node=expr, args=klass_name)
        for klass_name in to_call.keys():
            self.add_message('W0231', args=klass_name, node=node)

    def _check_signature(self, method1, method2, class_type):
        """check that the signature of the two given methods match
        
        class_type is in 'class', 'interface'
        """
        if not (isinstance(method1, astng.Function)
                and isinstance(method2, astng.Function)):
            self.add_message('F0202', args=(method1, method2), node=method1)
            return
        # don't care about functions with unknown argument (builtins)
        if method1.argnames is None or method2.argnames is None:
            return
        if len(method1.argnames) != len(method2.argnames):
            self.add_message('W0221', args=class_type, node=method1)
        elif len(method1.defaults) != len(method2.defaults):
            self.add_message('W0222', args=class_type, node=method1)
            
    def _get_method(self, node, method_name, fatal=1):
        """get astng for <method_name> on the given class node"""
        try:
            return node.get_method(method_name)
        except astng.NotFoundError:
            raise
        except Exception:
            if fatal:
                self.add_message('F0201', args=(method_name, node.name),
                                 node=node)
            
    def _ancestors_to_call(self, node, klass_node, method='__init__'):
        """return two dictionarys :
        
        * one where keys are the list of base classes names with the given
          method that should be called from the method node
        * a second where keys are base classes names which could not have been
          resolved correctly
        """
        to_call = {}
        unresolved = {}
        for base in klass_node.basenames:
            parts = base.split('.')
            cbase = parts[0]
            try:
                baseastng = klass_node.resolve(cbase)
            except (astng.ResolveError, astng.NotFoundError):
                unresolved[base] = 1
                self.add_message('F0203', node=node, args=cbase)
                continue
            for part in parts[1:]:
                try:
                    cbase = '%s.%s' % (cbase, part)
                    baseastng = baseastng.resolve(part)
                except astng.ResolveError:
                    unresolved[base] = 1
                    #import traceback
                    #traceback.print_exc()
                    self.add_message('F0203', node=node, args=cbase)
                    break
            else:
                try:
                    baseastng.get_method(method)
                    to_call[base] = 1
                except astng.NotFoundError:
                    continue
        return to_call, unresolved
        
        
def register(linter):
    """required method to auto register this checker """
    linter.register_checker(ClassChecker(linter))
