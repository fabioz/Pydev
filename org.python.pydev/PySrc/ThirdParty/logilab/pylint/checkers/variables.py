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
""" Copyright (c) 2000-2003 LOGILAB S.A. (Paris, FRANCE).
 http://www.logilab.fr/ -- mailto:contact@logilab.fr

variables checkers for Python code
"""

__revision__ = "$Id: variables.py,v 1.1 2004-10-26 12:52:30 fabioz Exp $"

from copy import copy

from logilab.common import astng

from logilab.pylint.interfaces import IASTNGChecker
from logilab.pylint.checkers import BaseChecker, CheckerHandler
from logilab.pylint.checkers.utils import is_interface, is_abstract, \
     is_builtin, is_native_builtin, is_error

    
MSGS = {
    'E0601': ('Using variable %r before assigment',
              'Used when a local variable is accessed before it\'s assigment.'),
    'E0602': ('Undefined variable %r',
              'Used when an undefined variable is accessed.'),
    'W0601': ('Undefined global %r',
              'Used when a variable is defined through the "global" statement \
              but the variable is not defined in the static global scope.'),
    
    'W0611': ('Unused import %s',
              'Used when an imported module or variable is not used.'),
    'W0612': ('Unused variable %r',
              'Used when a variable is defined but not used.'),
    'W0613': ('Unused argument %r',
              'Used when a function or method argument is not used.'),
    
    'W0621': ('Redefining name %r from outer scope (line %s)',
              'Used when a variable\'s name hide a name defined in the outer \
              scope.'),
    'W0622': ('Redefining built-in %r',
              'Used when a variable or function override a built-in.'),
    }

class VariablesChecker(BaseChecker, CheckerHandler):
    """checks for                                                              
    * unused variables / imports                                               
    * undefined variables                                                      
    * redefinition of variable from builtins or from an outer scope            
    * use of variable before assigment                                         
    """
    
    __implements__ = IASTNGChecker

    name = 'variables'
    msgs = MSGS
    priority = -1
    options = (
               ("init-import",
                {'default': 0, 'type' : 'yn', 'metavar' : '<y_or_n>',
                 'help' : 'Tells wether we should check for unused import in \
__init__ files.'}),
               )
    def __init__(self, linter=None):
        BaseChecker.__init__(self, linter)
        CheckerHandler.__init__(self)
        self._to_consume = None
        
    def visit_module(self, node):
        """visit module : update consumption analysis variable
        checks globals doesn't overrides builtins
        """
        self._to_consume = [(copy(node.locals), {}, 'module')]
        for name, stmt in node.locals.items():
            if name != '__builtins__' and is_native_builtin(name):
                self.add_message('W0622', args=name, node=stmt)
        
    def leave_module(self, node):
        """leave module: check globals
        """
        assert len(self._to_consume) == 1
        not_consumed = self._to_consume.pop()[0]
        # don't check unused imports in __init__ files
        if not self.config.init_import and node.package:
            return
        for name, stmt in not_consumed.items():
            # the latest test avoid warning on __builtins__ which is 
            # implicitly added by wildcard import 
            if name == '__builtins__':
                continue
            if isinstance(stmt, astng.Import) or (
                isinstance(stmt, astng.From) and stmt.modname != '__future__'):
                self.add_message('W0611', args=name, node=stmt)
        del self._to_consume

    def visit_class(self, node):
        """visit class: update consumption analysis variable
        """
        self._to_consume.append((copy(node.locals), {}, 'class'))
            
    def leave_class(self, node):
        """leave class: update consumption analysis variable
        """
        # do not check for not used locals here (no sense)
        self._to_consume.pop()

    def visit_lambda(self, node):
        """visit lambda: update consumption analysis variable
        """
        self._to_consume.append((copy(node.locals), {}, 'class'))
            
    def leave_lambda(self, node):
        """leave lambda: update consumption analysis variable
        """
        # do not check for not used locals here
        self._to_consume.pop()
        
    def visit_function(self, node):
        """visit function: update consumption analysis variable and check locals
        """
        globs = node.root().globals
        for name, stmt in node.locals.items():
            if globs.has_key(name) and not isinstance(stmt, astng.Global):
                line = globs[name].lineno
                self.add_message('W0621', args=(name, line), node=stmt)
            elif is_native_builtin(name):
                self.add_message('W0622', args=name, node=stmt)
                
        self._to_consume.append((copy(node.locals), {}, 'function'))

    def leave_function(self, node):
        """leave function: check function's locals are consumed
        """
        not_consumed = self._to_consume.pop()[0]
        is_method = node.is_method()
        klass = node.parent.get_frame().object
        if is_method and ((klass and is_interface(klass)) or
                          is_abstract(node)):
            return
        if is_error(node):
            return
        for name, stmt in not_consumed.items():
            # ignore names imported by the global statement
            # FIXME: should only ignore them if it's assigned latter
            if isinstance(stmt, astng.Global):
                continue
            # care about functions with unknown argument (builtins)
            if node.argnames is not None and name in node.argnames:
                # don't warn if the first argument of a method is not used
                if is_method and node.argnames and name == node.argnames[0]:
                    continue
                # don't check callback arguments
                if node.name.startswith('cb_') or \
                       node.name.endswith('_cb'):
                    continue
                self.add_message('W0613', args=name, node=node)
            else:
                self.add_message('W0612', args=name, node=stmt)

    def visit_global(self, node):
        """check names imported exists in the global scope"""
        globs = node.root().globals
        for name in node.names:
            if not globs.has_key(name):
                self.add_message('W0601', args=name, node=node)
                
    def visit_name(self, node):
        """check that a name is defined if the current scope and doesn't
        redefine a built-in
        """
        name = node.name
        stmt = node
        frame = stmt.get_frame()
        # if the name node is used as a function default argument's value, then
        # start from the parent frame of the function instead of the function
        # frame
        if is_func_default(node):
            start_index = len(self._to_consume) - 2
        else:
            start_index = len(self._to_consume) - 1
        # iterates through the parent scope, from the inner to the outer
        for i in range(start_index, -1, -1):
            to_consume, consumed, scope_type = self._to_consume[i]
            # the name has already been consumed, ends
            if consumed.has_key(name):
                break
            # if the current scope is a class scope but it's not the inner scope
            # ignore it
            #
            # this prevents to access this scope instead of the globals one in
            # function members when there are some common names
            if scope_type == 'class' and i != start_index:
                continue
            # mark the name as consumed if it's defined in this scope
            # (ie no KeyError is raise by "to_consume[name]"
            try:
                consumed[name] = def_stmt = to_consume[name].get_statement()
                del to_consume[name]
                # checks for use before assigment
                if def_stmt and frame is consumed[name].get_frame() and \
                       stmt.lineno < def_stmt.lineno:
                    self.add_message('E0601', args=name, node=stmt)
                break
            except KeyError:
                continue
        else:
            # we have not found the name, if it isn't a builtin, that's an
            # undefined name !
            if not is_builtin(name):
                self._to_consume[-1][1][name] = 1
                self.add_message('E0602', args=name, node=stmt)

def is_func_default(node):
    """return true if the name is used in function default argument's value
    """
    parent = node.parent
    if parent is None:
        return 0
    if isinstance(parent, astng.Function) and parent.defaults and \
           node in parent.defaults:
        return 1
    return is_func_default(parent)
    
    
def register(linter):
    """required method to auto register this checker"""
    linter.register_checker(VariablesChecker(linter))
