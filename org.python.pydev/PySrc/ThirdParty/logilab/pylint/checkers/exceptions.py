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

exceptions checkers for Python code
"""

__revision__ = '$Id: exceptions.py,v 1.1 2004-10-26 12:52:30 fabioz Exp $'

from logilab.common import astng

from logilab.pylint.checkers import BaseChecker, CheckerHandler
from logilab.pylint.checkers.utils import is_empty, get_names
from logilab.pylint.interfaces import IASTNGChecker

MSGS = {
    'W0701': ('Raise a string exception',
              'Used when a string exception is raised.'),
    'W0702': ('No exception\'s type specified',
              'Used when an except clause doesn\'t specify exceptions type to \
              catch.'),
    'W0703': ('Catch "Exception"',
              'Used when an except catch Exception instances.'),
    'W0704': ('Except doesn\'t do anything',
              'Used when an except clause does nothing but "pass" and there is\
              no "else" clause.'),
    'W0705': ('Bad except clauses order',
              'Used when except clauses are not in the correct order (from the \
              more specific to the more generic).'),
    }

def is_raising(stmt):
    """return true if the given statement node raise an exception
    """
    for node in stmt.nodes:
        if isinstance(node, astng.Raise):
            return 1
    return 0
    
class ExceptionsChecker(BaseChecker, CheckerHandler):
    """checks for                                                              
    * excepts without exception filter                                         
    * string exceptions                                                        
    """
    
    __implements__ = IASTNGChecker

    name = 'exceptions'
    msgs = MSGS
    priority = -4
    options = ()

    def __init__(self, linter=None):
        BaseChecker.__init__(self, linter)
        CheckerHandler.__init__(self)

    def visit_raise(self, node):
        """check for string exception
        """
        # ignore empty raise
        if node.expr1 is None:
            return
        if isinstance(node.expr1, astng.Const):
            self.add_message('W0701', node=node)
        else:
            try:
                value = node.resolve(get_names(node.expr1)[0])
            except astng.ResolveError:
                pass
            else:
                if type(value) in (type(''), type(u'')):
                    self.add_message('W0701', node=node)
            
    def visit_tryexcept(self, node):
        """check for empty except
        """
        for index  in range(len(node.handlers)):
            exc_type = node.handlers[index][0]
            stmt = node.handlers[index][2]
            if exc_type is None:
                if len(node.handlers) == 1 and not is_raising(stmt):
                    # FIXME: unable to get the correct line num !
                    # need to patch ast 
                    self.add_message('W0702', node=stmt.nodes[0])
            elif exc_type.as_string() == 'Exception':
                # check if a "except Exception:" is followed by some other
                # except
                if index < (len(node.handlers) - 1):
                    self.add_message('W0705', node=exc_type)
                elif index == 0 and not is_raising(stmt):
                    self.add_message('W0703', node=exc_type)
                    
            # check for except  doing nothing but "pass", without else clause
            elif is_empty(stmt) and not node.else_:
                self.add_message('W0704', node=exc_type)
        
def register(linter):
    """required method to auto register this checker"""
    linter.register_checker(ExceptionsChecker(linter))
