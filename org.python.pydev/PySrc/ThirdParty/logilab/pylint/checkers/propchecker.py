from logilab.common import astng

from logilab.pylint.interfaces import IASTNGChecker
from logilab.pylint.checkers import BaseChecker

class MyChecker(BaseChecker):
    """add member attributes defined using my own "properties.create"
    function to the class locals dictionary
    """
    
    __implements__ = IASTNGChecker

    name = 'custom'
    msgs = {}
    options = ()
    # this is important so that your checker is executed before others
    priority = -1 

    def visit_callfunc(self, node):
        if not (isinstance(node.node, astng.Getattr)
                and isinstance(node.node.expr, astng.Name)
                and node.node.expr.name == 'properties'
                and node.node.attrname == 'create'):
            return
        in_class = node.get_frame()
        for param in node.args:
            in_class.locals[param.name] = node

    
def register(linter):
    """required method to auto register this checker"""
    linter.register_checker(MyChecker(linter))
