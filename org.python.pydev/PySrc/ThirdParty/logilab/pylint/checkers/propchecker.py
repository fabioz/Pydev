from logilab.common import astng

from logilab.pylint.interfaces import IASTNGChecker
from logilab.pylint.checkers import BaseChecker

def _title(prop_name):
    return prop_name.title().replace('_', '')

def make_get_name(prop_name):
    return 'Get' + _title(prop_name)
    
def make_set_name(prop_name):
    return 'Set' + _title(prop_name)

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
            in_class.locals[make_set_name(param.name)] = node
            in_class.locals[make_get_name(param.name)] = node

        in_class.locals["__properties__"] = node
    
def register(linter):
    """required method to auto register this checker"""
    linter.register_checker(MyChecker(linter))
